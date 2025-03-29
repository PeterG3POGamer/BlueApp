package app.serlanventas.mobile.VersionControl

import NetworkUtils
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.serlanventas.mobile.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

data class Change(
    val type: String,
    val description: String
)

data class VersionInfo(
    val version_code: Int,
    val version_name: String,
    val download_url: String,
    var file_size: Long,
    val changes: List<Change>?
)

interface GithubApi {
    @GET("PeterG3POGamer/BlueApp/master/app/src/main/java/app/serlanventas/mobile/VersionControl/version.json")
    suspend fun getLatestVersion(): VersionInfo
}

@SuppressLint("StaticFieldLeak")
object UpdateManager {
    private var downloadId: Long = -1
    private const val fileName = "app-update.apk"
    private var progressBar: ProgressBar? = null
    private var percentageText: TextView? = null
    private var downloadedSizeText: TextView? = null
    private var totalSizeText: TextView? = null
    private var speedText: TextView? = null
    private var statusText: TextView? = null
    private var lastUpdateTime: Long = 0
    private var lastBytesDownloaded: Long = 0
    private var currentProgress = 0
    private val isDownloading = AtomicBoolean(false)
    var currentActivityRef: WeakReference<Activity>? = null
    private var currentDialog: AlertDialog? = null
    private var downloadManager: DownloadManager? = null
    private var progressHandler: Handler? = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null
    private var downloadReceiver: BroadcastReceiver? = null
    private var cachedVersionInfo: VersionInfo? = null

    fun setCurrentActivity(activity: Activity) {
        currentActivityRef = WeakReference(activity)
        if (isDownloading.get() && (currentDialog == null || !currentDialog!!.isShowing)) {
            showProgressDialog(activity)
        }
    }

    fun isDownloading(): Boolean = isDownloading.get()

    fun downloadUpdate(context: Context, versionInfo: VersionInfo) {
        cachedVersionInfo = versionInfo
        if (isDownloading.get()) {
            currentActivityRef?.get()?.let { showProgressDialog(it) }
            return
        }

        if (progressHandler == null) {
            progressHandler = Handler(Looper.getMainLooper())
            Log.d("UpdateManager", "Handler reinicializado")
        }

        isDownloading.set(true)
        cleanDownloadFolder(context)

        val file = File(getDownloadFolder(context), fileName)
        val request = DownloadManager.Request(Uri.parse(versionInfo.download_url))
            .setTitle("Actualización de la App")
            .setDescription("Descargando versión ${versionInfo.version_name}")
            // Notificación visible durante toda la descarga
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationUri(Uri.fromFile(file))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        // Configuración adicional para la notificación
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            request.setRequiresCharging(false)
        }

        // Hacer que la notificación sea persistente
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            request.setRequiresDeviceIdle(false)
        }

        downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager!!.enqueue(request)

        currentActivityRef?.get()?.let { showProgressDialog(it) }
        registerDownloadReceiver(context)
        startProgressUpdates(context)
    }

    private fun registerDownloadReceiver(context: Context) {
        if (isReceiverRegistered) return

        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    DownloadManager.ACTION_DOWNLOAD_COMPLETE -> {
                        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                        if (id == downloadId) {
                            handleDownloadCompletion(context)
                        }
                    }
                    DownloadManager.ACTION_NOTIFICATION_CLICKED -> {
                        currentActivityRef?.get()?.let { showProgressDialog(it) }
                    }
                }
            }
        }

        try {
            val filter = IntentFilter().apply {
                addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED)
            }
            context.registerReceiver(downloadReceiver, filter)
            isReceiverRegistered = true
        } catch (e: Exception) {
            Log.e("UpdateManager", "Error al registrar receiver", e)
        }
    }

    private fun startProgressUpdates(context: Context) {
        if (progressHandler == null) {
            progressHandler = Handler(Looper.getMainLooper())
        }

        progressRunnable = object : Runnable {
            @SuppressLint("Range")
            override fun run() {
                if (!isDownloading.get() || downloadId == -1L) {
                    Log.d("UpdateManager", "Descarga no activa o ID inválido")
                    return
                }

                val query = DownloadManager.Query().setFilterById(downloadId)
                var cursor: android.database.Cursor? = null

                try {
                    cursor = downloadManager?.query(query)
                    if (cursor?.moveToFirst() == true) {
                        val bytesDownloaded = cursor.getLong(cursor.getColumnIndex(
                            DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val bytesTotal = cursor.getLong(cursor.getColumnIndex(
                            DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        val status = cursor.getInt(cursor.getColumnIndex(
                            DownloadManager.COLUMN_STATUS))

                        Log.d("UpdateManager", "Bytes: $bytesDownloaded/$bytesTotal - Status: $status")

                        if (bytesTotal > 0) {
                            val progress = (bytesDownloaded * 100 / bytesTotal).toInt()
                            updateProgressUI(progress, bytesDownloaded, bytesTotal)
                        }

                        when (status) {
                            DownloadManager.STATUS_RUNNING -> {
                                progressHandler?.postDelayed(this, 1000)
                            }
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                handleDownloadCompletion(context)
                            }
                            DownloadManager.STATUS_FAILED -> {
                                handleDownloadFailure(context)
                            }
                        }
                    } else {
                        Log.d("UpdateManager", "Cursor vacío o error al mover")
                    }
                } catch (e: Exception) {
                    Log.e("UpdateManager", "Error al verificar progreso", e)
                    progressHandler?.postDelayed(this, 2000)
                } finally {
                    cursor?.close()
                }
            }
        }
        progressHandler?.post(progressRunnable!!)
    }

    private fun updateNotification(context: Context, progress: Int, isComplete: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

            val channel = NotificationChannel(
                "download_channel",
                "Descargas de actualizaciones",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)

            if (isComplete) {
                // Notificación de completado
                val file = File(getDownloadFolder(context), fileName)
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    val downloadedApk = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    setDataAndType(downloadedApk, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    installIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, "download_channel")
                    .setContentTitle("Descarga completada")
                    .setContentText("Toque para instalar la actualización")
                    .setSmallIcon(R.drawable.ic_cabezapollo2) // Asegúrate de tener este icono
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()

                notificationManager.notify(downloadId.toInt(), notification)
            } else {
                // Notificación de progreso
                val notification = NotificationCompat.Builder(context, "download_channel")
                    .setContentTitle("Descargando actualización")
                    .setContentText("$progress% completado")
                    .setSmallIcon(R.drawable.ic_download)
                    .setProgress(100, progress, false)
                    .setOngoing(true) // Notificación persistente
                    .build()

                notificationManager.notify(downloadId.toInt(), notification)
            }
        }
    }

    private fun updateProgressUI(progress: Int, bytesDownloaded: Long, bytesTotal: Long) {
        Handler(Looper.getMainLooper()).post {
            try {
                currentProgress = progress

                // Actualizar barra de progreso
                progressBar?.progress = progress
                percentageText?.text = "$progress%"

                // Actualizar tamaños de descarga
                if (bytesTotal > 0) {
                    val downloadedMB = bytesDownloaded / (1024.0 * 1024.0)
                    val totalMB = bytesTotal / (1024.0 * 1024.0)

                    downloadedSizeText?.text = "%.2f".format(downloadedMB)
                    totalSizeText?.text = "%.2f".format(totalMB)

                    // Calcular velocidad de descarga
                    val currentTime = System.currentTimeMillis()
                    if (lastUpdateTime > 0) {
                        val timeDiff = (currentTime - lastUpdateTime) / 1000.0
                        if (timeDiff > 0) {
                            val bytesDiff = bytesDownloaded - lastBytesDownloaded
                            val speedKBps = (bytesDiff / 1024.0) / timeDiff
                            speedText?.text = "Velocidad: %.1f KB/s".format(speedKBps)
                        }
                    }
                    lastUpdateTime = currentTime
                    lastBytesDownloaded = bytesDownloaded
                }

                // Actualizar estado según progreso
                statusText?.text = when {
                    progress >= 100 -> "Completado"
                    progress > 0 -> "Descargando..."
                    else -> "Preparando..."
                }

                // Mostrar diálogo si no está visible
                if (currentDialog == null || !currentDialog!!.isShowing) {
                    currentActivityRef?.get()?.let { activity ->
                        showProgressDialog(activity)
                    }
                }
            } catch (e: Exception) {
                Log.e("UpdateManager", "Error al actualizar UI", e)
            }
        }
    }

    fun showProgressDialog(context: Context) {
        val activity = context as? Activity ?: return
        if (activity.isFinishing || activity.isDestroyed) return

        // Verificar si el diálogo actual está mostrando la misma actividad
        val currentContext = (currentDialog?.context as? ContextWrapper)?.baseContext as? Activity
        if (currentContext == activity && currentDialog?.isShowing == true) {
            return // Ya está mostrando el diálogo para esta actividad
        }

        safeDismissDialog()

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_download_progress, null)

        // Inicializar todas las vistas
        progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar).apply {
            progress = currentProgress
            ObjectAnimator.ofInt(this, "progress", currentProgress)
                .setDuration(300)
                .start()
        }

        percentageText = dialogView.findViewById<TextView>(R.id.percentageText).apply {
            text = "$currentProgress%"
            setTextColor(Color.WHITE)
        }

        // Nuevas vistas añadidas
        downloadedSizeText = dialogView.findViewById<TextView>(R.id.downloadedSizeText).apply {
            setTextColor(Color.WHITE)
        }

        totalSizeText = dialogView.findViewById<TextView>(R.id.totalSizeText).apply {
            setTextColor(Color.WHITE)
        }

        speedText = dialogView.findViewById<TextView>(R.id.speedText).apply {
            setTextColor(Color.WHITE)
        }

        statusText = dialogView.findViewById<TextView>(R.id.statusText).apply {
            setTextColor(Color.WHITE)
        }

        currentDialog = AlertDialog.Builder(context, R.style.CustomProgressDialog)
            .setView(dialogView)
            .setCancelable(false)
            .create().apply {
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                window?.setDimAmount(0.7f)
            }

        try {
            currentDialog?.show()
            // Forzar actualización inicial
            updateProgressUI(currentProgress, 0L, 0L)
        } catch (e: Exception) {
            Log.e("UpdateManager", "Error al mostrar diálogo", e)
        }
    }

    private fun handleDownloadCompletion(context: Context) {
        Handler(Looper.getMainLooper()).post {
            // Verificar si el contexto aún es válido
            val activity = currentActivityRef?.get()
            if (activity == null || activity.isFinishing || activity.isDestroyed) {
                Log.w("UpdateManager", "Actividad no disponible para completar descarga")
                cleanup(context.applicationContext, force = true)
                return@post
            }

            var shouldRetry = false
            var errorMessage: String? = null
            var versionInfo: VersionInfo? = null

            try {
                versionInfo = this.cachedVersionInfo // Asume que guardaste la info de versión
                val file = File(getDownloadFolder(context), fileName)

                // Verificación adicional del archivo
                if (!file.exists() || file.length() == 0L) {
                    errorMessage = "Archivo de actualización no válido"
                    shouldRetry = true
                    Log.e("UpdateManager", "Archivo no existe o está vacío: ${file.absolutePath}")
                    throw IllegalStateException(errorMessage)
                }

                // Verificar permisos antes de instalar
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    !context.packageManager.canRequestPackageInstalls()) {
                    errorMessage = "Permiso para instalar aplicaciones no concedido"
                    shouldRetry = false
                    Log.e("UpdateManager", errorMessage)
                    throw SecurityException(errorMessage)
                }

                // Intentar instalación
                updateNotification(context, 100, true)
                installUpdate(context, file)

            } catch (e: SecurityException) {
                errorMessage = "Error de permisos al instalar actualización"
                Log.e("UpdateManager", errorMessage, e)
            } catch (e: IllegalStateException) {
                errorMessage = "No se puede instalar en este momento"
                shouldRetry = true
                Log.e("UpdateManager", errorMessage, e)
            } catch (e: Exception) {
                errorMessage = "Error al instalar la actualización: ${e.localizedMessage}"
                shouldRetry = true
                Log.e("UpdateManager", errorMessage, e)
            } finally {
                // Limpieza con contexto de aplicación para mayor seguridad
                cleanup(context.applicationContext, force = true)

                // Reintentar descarga si es necesario
                if (shouldRetry && activity != null && !activity.isFinishing && !activity.isDestroyed) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        AlertDialog.Builder(activity)
                            .setTitle("Error en descarga")
                            .setMessage("${errorMessage}\n\n¿Desea reintentar la descarga?")
                            .setPositiveButton("Reintentar") { _, _ ->
                                // Llamar al método downloadUpdate directamente
                                if (versionInfo != null) {
                                    downloadUpdate(context, versionInfo)
                                } else {
                                    showError(activity, "No se pudo obtener información de actualización")
                                }
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    }, 500)
                } else if (errorMessage != null) {
                    showError(activity, errorMessage)
                }
            }
        }
    }

    private fun handleDownloadFailure(context: Context) {
        Handler(Looper.getMainLooper()).post {
            cleanup(context, force = true)
            showError(context, "La descarga falló. Por favor, inténtalo nuevamente.")
        }
    }

    private fun showError(context: Context, message: String) {
        currentActivityRef?.get()?.let { activity ->
            if (!activity.isFinishing && !activity.isDestroyed) {
                AlertDialog.Builder(activity)
                    .setTitle("Error")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun safeDismissDialog() {
        try {
            currentDialog?.dismiss()
        } catch (e: Exception) {
            Log.e("UpdateManager", "Error al cerrar diálogo", e)
        }
        currentDialog = null
    }

    private var isReceiverRegistered = false

    fun cleanup(context: Context, force: Boolean = false) {
        if (!force && isDownloading.get()) {
            return
        }

        isDownloading.set(false)
        progressHandler?.removeCallbacks(progressRunnable ?: return)
        safeDismissDialog()

        try {
            if (isReceiverRegistered) {
                context.unregisterReceiver(downloadReceiver)
                isReceiverRegistered = false
            }
        } catch (e: IllegalArgumentException) {
            Log.e("UpdateManager", "Receiver no registrado", e)
        }

        // No nuleamos el handler, solo limpiamos callbacks
        progressRunnable = null
        downloadReceiver = null
        downloadManager = null
        downloadId = -1
    }

    private fun installUpdate(context: Context, file: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                val downloadedApk = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                setDataAndType(downloadedApk, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("UpdateManager", "Error al instalar actualización", e)
            showError(context, "No se pudo instalar la actualización")
        }
    }

    private fun getDownloadFolder(context: Context): File {
        return context.getExternalFilesDir("Updates") ?: File(context.filesDir, "Updates").apply {
            mkdirs()
        }
    }

    fun cleanDownloadFolder(context: Context) {
        getDownloadFolder(context).listFiles()?.forEach { file ->
            if (file.name.endsWith(".apk")) file.delete()
        }
    }
}

class UpdateChecker(private val context: Context) {
    private var currentDialog: AlertDialog? = null
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://raw.githubusercontent.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val githubApi by lazy { retrofit.create(GithubApi::class.java) }
    private val okHttpClient by lazy { OkHttpClient() }
    private var isDialogShowing = false

    suspend fun checkAndDownloadUpdate() {
        // Verificar si ya está descargando primero
        if (UpdateManager.isDownloading()) {
            UpdateManager.currentActivityRef?.get()?.let {
                UpdateManager.showProgressDialog(it)
            }
            return
        }

        if (isDialogShowing || !NetworkUtils.isNetworkAvailable(context)) return

        val update = checkForUpdate()
        update?.let { showUpdateDialog(it) }
    }

    private suspend fun checkForUpdate(): VersionInfo? {
        return try {
            val latestVersion = githubApi.getLatestVersion()
            val currentVersion = context.packageManager
                .getPackageInfo(context.packageName, 0).versionName

            if (latestVersion.version_name != currentVersion) {
                latestVersion.copy(file_size = getFileSizeFromUrl(latestVersion.download_url))
            } else null
        } catch (e: Exception) {
            Log.e("UpdateChecker", "Error al verificar actualización", e)
            null
        }
    }

    @SuppressLint("MissingInflatedId", "SetTextI18n")
    private fun showUpdateDialog(versionInfo: VersionInfo) {
        // Verificar si el contexto es una actividad válida
        val activity = context as? Activity ?: return
        if (activity.isFinishing || activity.isDestroyed) return

        if (isDialogShowing || UpdateManager.isDownloading()) return
        isDialogShowing = true

        // Cerrar diálogo previo si existe
        currentDialog?.dismiss()

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_update_version, null)
        currentDialog = AlertDialog.Builder(context, R.style.CustomAlertDialog)
            .setView(dialogView)
            .setCancelable(false)
            .create().apply {
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setOnDismissListener {
                    isDialogShowing = false
                    currentDialog = null
                }
            }

        with(dialogView) {
            findViewById<TextView>(R.id.update_info).text =
                "Versión: ${versionInfo.version_name}\nTamaño: %.2f MB".format(versionInfo.file_size / (1024.0 * 1024.0))

            findViewById<RecyclerView>(R.id.changes_recycler_view).apply {
                layoutManager = LinearLayoutManager(context)
                adapter = ChangesAdapter(versionInfo.changes ?: emptyList())
                setHasFixedSize(true)
            }

            findViewById<Button>(R.id.btn_download).setOnClickListener {
                UpdateManager.downloadUpdate(context, versionInfo)
                currentDialog?.dismiss()
            }

            findViewById<Button>(R.id.btn_cancel).setOnClickListener {
                currentDialog?.dismiss()
            }
        }

        try {
            currentDialog?.show()
        } catch (e: Exception) {
            Log.e("UpdateChecker", "Error al mostrar diálogo", e)
            isDialogShowing = false
            currentDialog = null
        }
    }

    private suspend fun getFileSizeFromUrl(url: String): Long = withContext(Dispatchers.IO) {
        try {
            val response = okHttpClient.newCall(Request.Builder().url(url).build()).execute()
            response.header("Content-Length")?.toLong() ?: -1L
        } catch (e: IOException) {
            Log.e("UpdateChecker", "Error al obtener tamaño del archivo", e)
            -1L
        }
    }
}

class ChangesAdapter(private val changes: List<Change>) : RecyclerView.Adapter<ChangesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val changeType: TextView = view.findViewById(R.id.change_type)
        val changeDescription: TextView = view.findViewById(R.id.change_description)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_change, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val change = changes[position]
        holder.changeType.text = change.type
        holder.changeDescription.text = change.description

        // Set background color and border based on change type
        val backgroundColor = getColorForChangeType(change.type)
        val borderColor = darkenColor(backgroundColor, 0.7f)
        holder.changeType.setBackgroundColor(backgroundColor)

        // Create a drawable with background color and border
        val drawable = GradientDrawable().apply {
            setColor(backgroundColor)
            setStroke(0, borderColor)
            cornerRadius = 8f
        }
        holder.changeType.background = drawable

        // Set text color as a darker version of the background color
        holder.changeType.setTextColor(borderColor)
    }

    override fun getItemCount() = changes.size

    private fun getColorForChangeType(type: String): Int {
        return when (type.lowercase(Locale.getDefault())) {
            "actualizado" -> Color.parseColor("#f5b169")  // Orange
            "nuevo" -> Color.parseColor("#69fa6e")  // Green
            "mejorado" -> Color.parseColor("#7abcfa")  // Blue
            "arreglado" -> Color.parseColor("#faacac")  // Red
            else -> Color.parseColor("#9E9E9E")  // Grey
        }
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) * factor).toInt()
        val g = (Color.green(color) * factor).toInt()
        val b = (Color.blue(color) * factor).toInt()
        return Color.argb(a, r, g, b)
    }
}


