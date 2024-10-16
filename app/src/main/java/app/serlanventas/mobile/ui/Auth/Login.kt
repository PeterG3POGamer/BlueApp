package app.serlanventas.mobile.ui.Auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import app.serlanventas.mobile.ui.Utilidades.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class Login(private val context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    suspend fun authenticate(dni: String, password: String): Boolean {
        return withContext(Dispatchers.IO) {
            val isProduction = Constants.obtenerEstadoModo(context) // Obtener el modo (producción o prueba)
            val loginUrl = "${Constants.getBaseUrl(isProduction)}controllers/LoginControllerApp.php?op=verificar&dni=$dni&pass=$password" // Construir la URL correcta
            val url = URL(loginUrl)
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("LoginResponse", response)

                    // Intenta analizar la respuesta como JSONArray
                    return@withContext try {
                        val jsonResponse = JSONArray(response)

                        if (jsonResponse.length() > 0) {
                            val userObject = jsonResponse.getJSONObject(0)
                            // Aquí puedes añadir condiciones para validar si el usuario es correcto
                            val status = true // Cambia esto según la lógica de tu API

                            if (status) {
                                saveUserData(userObject)
                                saveAuthenticationState(true)
                                true
                            } else {
                                false
                            }
                        } else {
                            Log.e("LoginError", "Empty JSON array")
                            false
                        }
                    } catch (e: Exception) {
                        Log.e("LoginError", "Failed to parse JSON: ${e.message}")
                        false
                    }
                } else {
                    Log.e("LoginError", "HTTP error code: $responseCode")
                    false
                }
            } catch (e: Exception) {
                Log.e("LoginError", "Exception: ${e.message}")
                false
            } finally {
                connection.disconnect()
            }
        }
    }

    fun isAuthenticated(): Boolean {
        return sharedPreferences.getBoolean("is_authenticated", false)
    }

    private fun saveAuthenticationState(isAuthenticated: Boolean) {
        with(sharedPreferences.edit()) {
            putBoolean("is_authenticated", isAuthenticated)
            apply()
        }
    }

    private fun saveUserData(userObject: JSONObject) {
        with(sharedPreferences.edit()) {
            putString("dni", userObject.optString("dni"))
            putString("nombre", userObject.optString("nombre"))
            putString("rolname", userObject.optString("rolname"))
            putString("tipo_usuario", userObject.optString("tipo_usuario"))
            putString("nucleo_name", userObject.optString("nucleo_name"))
            putString("idEstablecimiento", userObject.optString("idEstablecimiento"))
            putString("pass", userObject.optString("pass")) // Añadido para guardar el campo pass
            apply()
        }
    }


    fun showLoginFailureMessage() {
        Toast.makeText(context, "Invalid credentials", Toast.LENGTH_SHORT).show()
    }
}
