package com.example.blueapp.VersionControl

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.io.File

data class VersionInfo(
    val version_code: Int,
    val version_name: String,
    val download_url: String,
    val file_size: Long // Añadido tamaño del archivo
)

interface GithubApi {
    @GET("PeterG3POGamer/BlueApp/master/app/src/main/java/com/example/blueapp/VersionControl/version.json")
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
                .getPackageInfo(context.packageName, 0).versionCode

            if (latestVersion.version_code > currentVersionCode) {
                latestVersion
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("UpdateChecker", "Error checking for updates", e)
            null
        }
    }

    suspend fun checkAndDownloadUpdate() {
        val update = checkForUpdate()
        update?.let {
            updateManager.downloadUpdate(it)
        }
    }
}

class UpdateManager(private val context: Context) {
    private var downloadId: Long = 0

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun downloadUpdate(versionInfo: VersionInfo) {
        val request = DownloadManager.Request(Uri.parse(versionInfo.download_url))
            .setTitle("Actualización de BlueApp")
            .setDescription("Descargando versión ${versionInfo.version_name}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "SerlanVentas.apk")
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
                val file = File(context.getExternalFilesDir("Update"), "blueapp_update.apk")
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
    }
}