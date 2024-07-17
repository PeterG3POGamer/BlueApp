package com.example.blueapp.VersionControl

import android.content.Context
import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

data class VersionInfo(
    val version_code: Int,
    val version_name: String,
    val download_url: String
)

interface GithubApi {
    @GET("PeterG3POGamer/BlueApp/main/version.json")
    suspend fun getLatestVersion(): VersionInfo
}

class UpdateChecker(private val context: Context) {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://raw.githubusercontent.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val githubApi = retrofit.create(GithubApi::class.java)

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
}