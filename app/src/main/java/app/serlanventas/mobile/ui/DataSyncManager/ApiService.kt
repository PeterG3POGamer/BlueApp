package app.serlanventas.mobile.ui.DataSyncManager

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ApiService {
    suspend fun makePostRequest(
        urlString: String,
        jsonBody: JSONObject,
        callback: (String?, Exception?) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.doOutput = true

                // Escribe el JSON en el cuerpo de la solicitud
                val outputStream = conn.outputStream
                outputStream.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
                outputStream.flush()
                outputStream.close()

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = conn.inputStream.bufferedReader().use { it.readText() }
                    withContext(Dispatchers.Main) {
                        callback(inputStream, null)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Log.e("ApiService", "Error al enviar datos: $responseCode")
                        callback(null, Exception("Error al enviar datos"))
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                withContext(Dispatchers.Main) {
                    callback(null, ex)
                }
            }
        }
    }
}