package app.serlanventas.mobile.VersionControl

import NetworkUtils
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.serlanventas.mobile.R
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.io.File

data class Change(
    val type: String,
    val description: String
)

data class VersionInfo(
    val version_code: Int,
    val version_name: String,
    val download_url: String,
    val file_size: Long,
    val changes: List<Change>? // Cambiado a nullable
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

    suspend fun checkForUpdate(): VersionInfo? {
        return try {
            val latestVersion = githubApi.getLatestVersion()
            val currentVersionCode = context.packageManager
                .getPackageInfo(context.packageName, 0).versionName

            if (latestVersion.version_name != currentVersionCode) {
                latestVersion
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("UpdateChecker", "Error checking for updates", e)
            null
        }
    }

    suspend fun checkAndDownloadUpdate() {
        if (NetworkUtils.isNetworkAvailable(context)) {
            val update = checkForUpdate()
            update?.let {
                showUpdateDialog(it)
            }
        } else {
            showNoInternetDialog()
        }
    }

    private fun showUpdateDialog(versionInfo: VersionInfo) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_update_version, null)
        val dialog = AlertDialog.Builder(context, R.style.CustomAlertDialog)
            .setView(dialogView)
            .create()

        val updateInfoTextView = dialogView.findViewById<TextView>(R.id.update_info)
        val changesRecyclerView = dialogView.findViewById<RecyclerView>(R.id.changes_recycler_view)
        val downloadButton = dialogView.findViewById<Button>(R.id.btn_download)
        val cancelButton = dialogView.findViewById<Button>(R.id.btn_cancel)

        updateInfoTextView.text = "Versión ${versionInfo.version_name}\nTamaño: ${versionInfo.file_size / 1024 / 1024} MB"

        changesRecyclerView.layoutManager = LinearLayoutManager(context)
        changesRecyclerView.adapter = ChangesAdapter(versionInfo.changes ?: emptyList())

        downloadButton.setOnClickListener {
            updateManager.downloadUpdate(versionInfo)
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showNoInternetDialog() {
        AlertDialog.Builder(context, R.style.CustomAlertDialog)
            .setTitle("Sin conexión a Internet")
            .setMessage("No se puede verificar actualizaciones. Por favor, comprueba tu conexión a Internet e intenta nuevamente.")
            .setPositiveButton("Aceptar", null)
            .show()
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

        // Set background color based on change type
        holder.changeType.setBackgroundColor(getColorForChangeType(change.type))
    }

    override fun getItemCount() = changes.size

    private fun getColorForChangeType(type: String): Int {
        return when (type.toLowerCase()) {
            "fix" -> Color.parseColor("#FF9800")  // Orange
            "update" -> Color.parseColor("#4CAF50")  // Green
            "new" -> Color.parseColor("#2196F3")  // Blue
            else -> Color.parseColor("#9E9E9E")  // Grey
        }
    }
}


class UpdateManager(private val context: Context) {
    private var downloadId: Long = 0
    private val fileName = "SerlanVentas.apk"

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun downloadUpdate(versionInfo: VersionInfo) {
        cleanDownloadFolder()

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

        context.registerReceiver(
            onDownloadComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
    }

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadId == id) {
                val file = File(getDownloadFolder(), fileName)
                installUpdate(file)
            }
        }
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

        // Programar la eliminación del archivo después de iniciar la instalación
        file.deleteOnExit()
    }

    private fun getDownloadFolder(): File {
        return context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
    }

    private fun cleanDownloadFolder() {
        val folder = getDownloadFolder()
        folder.listFiles()?.forEach { file ->
            if (file.name.endsWith(".apk")) {
                file.delete()
            }
        }
    }
}