package app.serlanventas.mobile.VersionControl

import NetworkUtils
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import java.util.Locale

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

class UpdateChecker(private val context: Context) {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://raw.githubusercontent.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val githubApi = retrofit.create(GithubApi::class.java)
    private val updateManager = UpdateManager(context)
    private val okHttpClient = OkHttpClient()

    suspend fun checkForUpdate(): VersionInfo? {
        return try {
            val latestVersion = githubApi.getLatestVersion()
            val currentVersionCode = context.packageManager
                .getPackageInfo(context.packageName, 0).versionName

            if (latestVersion.version_name != currentVersionCode) {
                val fileSize = getFileSizeFromUrl(latestVersion.download_url)
                latestVersion.copy(file_size = fileSize)
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("UpdateChecker", "Error checking for updates", e)
            null
        }
    }

    private suspend fun getFileSizeFromUrl(url: String): Long = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).head().build()
            val response = okHttpClient.newCall(request).execute()
            response.header("Content-Length")?.toLong() ?: -1L
        } catch (e: IOException) {
            android.util.Log.e("UpdateChecker", "Error getting file size", e)
            -1L
        }
    }

    suspend fun checkAndDownloadUpdate() {
        if (NetworkUtils.isNetworkAvailable(context)) {
            val update = checkForUpdate()
            update?.let {
                showUpdateDialog(it)
            }
        }
    }

    @SuppressLint("MissingInflatedId", "SetTextI18n")
    private fun showUpdateDialog(versionInfo: VersionInfo) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_update_version, null)
        val dialog = AlertDialog.Builder(context, R.style.CustomAlertDialog)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val versionInfoTextView = dialogView.findViewById<TextView>(R.id.update_info)
        val changesRecyclerView = dialogView.findViewById<RecyclerView>(R.id.changes_recycler_view)
        val downloadButton = dialogView.findViewById<Button>(R.id.btn_download)
        val cancelButton = dialogView.findViewById<Button>(R.id.btn_cancel)

        val fileSizeMB = versionInfo.file_size / (1024.0 * 1024.0)
        versionInfoTextView.text = "Versión: ${versionInfo.version_name}\nTamaño: %.2f MB".format(fileSizeMB)

        changesRecyclerView.layoutManager = LinearLayoutManager(context)
        changesRecyclerView.adapter = ChangesAdapter(versionInfo.changes ?: emptyList())

        downloadButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    intent.data = Uri.parse("package:${context.packageName}")
                    (context as? AppCompatActivity)?.startActivityForResult(intent, REQUEST_INSTALL_PERMISSION)
                } else {
                    updateManager.downloadUpdate(versionInfo)
                }
            } else {
                updateManager.downloadUpdate(versionInfo)
            }
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    companion object {
        const val REQUEST_INSTALL_PERMISSION = 1001
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

        val backgroundColor = getColorForChangeType(change.type)
        val borderColor = darkenColor(backgroundColor, 0.7f)
        holder.changeType.setBackgroundColor(backgroundColor)

        val drawable = GradientDrawable().apply {
            setColor(backgroundColor)
            setStroke(0, borderColor)
            cornerRadius = 8f
        }
        holder.changeType.background = drawable

        holder.changeType.setTextColor(borderColor)
    }

    override fun getItemCount() = changes.size

    private fun getColorForChangeType(type: String): Int {
        return when (type.lowercase(Locale.getDefault())) {
            "actualizado" -> Color.parseColor("#f5b169")
            "nuevo" -> Color.parseColor("#69fa6e")
            "mejorado" -> Color.parseColor("#7abcfa")
            "arreglado" -> Color.parseColor("#faacac")
            else -> Color.parseColor("#9E9E9E")
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

class UpdateManager(private val context: Context) {
    private var downloadId: Long = 0
    private val fileName = "app-debug.apk"
    private lateinit var progressDialog: AlertDialog
    private lateinit var progressBar: ProgressBar
    private lateinit var percentageText: TextView
    private var currentProgress = 0
    private var onDownloadComplete: BroadcastReceiver? = null

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun downloadUpdate(versionInfo: VersionInfo) {
        cleanDownloadFolder()
        showProgressDialog()

        val file = File(getDownloadFolder(), fileName)
        val request = DownloadManager.Request(Uri.parse(versionInfo.download_url))
            .setTitle("Actualización de BlueApp")
            .setDescription("Descargando versión ${versionInfo.version_name}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationUri(Uri.fromFile(file))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)

        onDownloadComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    animateProgress(currentProgress, 100)
                    Handler(Looper.getMainLooper()).postDelayed({
                        progressDialog.dismiss()
                        val file = File(getDownloadFolder(), fileName)
                        installUpdate(file)
                        context.unregisterReceiver(this)
                    }, 1000)
                }
            }
        }

        context.registerReceiver(
            onDownloadComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )

        updateProgress()
    }

    private fun showProgressDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_download_progress, null)
        progressBar = dialogView.findViewById(R.id.progressBar)
        percentageText = dialogView.findViewById(R.id.percentageText)

        progressDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        progressDialog.show()
    }

    private fun updateProgress() {
        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            @SuppressLint("Range")
            override fun run() {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val cursor = downloadManager.query(query)

                if (cursor.moveToFirst()) {
                    val bytesDownloaded = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val bytesTotal = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                    if (bytesTotal > 0) {
                        val progress = ((bytesDownloaded * 100) / bytesTotal).toInt()
                        animateProgress(currentProgress, progress)
                        currentProgress = progress
                    }

                    val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    when (status) {
                        DownloadManager.STATUS_FAILED -> {
                            progressDialog.dismiss()
                            Toast.makeText(context, "Descarga fallida", Toast.LENGTH_LONG).show()
                        }
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            // No hacemos nada aquí, el BroadcastReceiver se encargará de esto
                        }
                        else -> handler.postDelayed(this, 500)
                    }
                }

                cursor.close()
            }
        })
    }

    private fun animateProgress(from: Int, to: Int) {
        val animator = ValueAnimator.ofInt(from, to)
        animator.duration = 500 // duración de la animación en milisegundos
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener { animation ->
            val progress = animation.animatedValue as Int
            progressBar.progress = progress
            percentageText.text = "$progress%"
        }
        animator.start()
    }

    private fun installUpdate(file: File) {
        val intent = Intent(Intent.ACTION_VIEW)
        val downloadedApk = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        intent.setDataAndType(downloadedApk, "application/vnd.android.package-archive")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        context.startActivity(intent)
    }

    private fun getDownloadFolder(): File {
        return context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
    }

    fun cleanDownloadFolder() {
        val folder = getDownloadFolder()
        folder.listFiles()?.forEach { file ->
            if (file.name.endsWith(".apk")) {
                file.delete()
            }
        }
    }
}