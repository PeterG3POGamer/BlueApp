package com.example.blueapp.VersionControl

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url

data class VersionInfo(
    val version_code: Int,
    val version_name: String,
    val download_url: String
)

data class GithubContent(
    val content: String,
    val encoding: String,
    val download_url: String
)

interface GithubApi {
    @GET("repos/PeterG3POGamer/BlueApp/contents/version.json")
    suspend fun getLatestVersion(): GithubContent

    @GET
    suspend fun getRawContent(@Url url: String): VersionInfo
}

class UpdateChecker(private val context: Context) {
    private val token = "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"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val newRequest: Request = chain.request().newBuilder()
                .addHeader("Authorization", "token $token")
                .build()
            chain.proceed(newRequest)
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val githubApi = retrofit.create(GithubApi::class.java)

    suspend fun checkForUpdate(): VersionInfo? {
        return try {
            val content = githubApi.getLatestVersion()
            val decodedContent = Base64.decode(content.content, Base64.DEFAULT).toString(Charsets.UTF_8)
            val versionInfo = Gson().fromJson(decodedContent, VersionInfo::class.java)

            val currentVersionCode = context.packageManager
                .getPackageInfo(context.packageName, 0).versionCode

            if (versionInfo.version_code > currentVersionCode) {
                versionInfo
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("UpdateChecker", "Error checking for updates", e)
            null
        }
    }
}