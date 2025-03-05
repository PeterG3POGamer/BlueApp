package app.serlanventas.mobile.ui.Auth

import NetworkUtils
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import app.serlanventas.mobile.ui.DataBase.AppDatabase
import app.serlanventas.mobile.ui.DataBase.Entities.UsuarioEntity
import app.serlanventas.mobile.ui.Utilidades.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.mindrot.jbcrypt.BCrypt
import java.net.HttpURLConnection
import java.net.URL

class Login(private val context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    suspend fun authenticate(dni: String, password: String): Boolean {
        return withContext(Dispatchers.IO) {
            val db = AppDatabase(context)

            if (NetworkUtils.isNetworkAvailable(context)) {
                // Si hay conexión a internet, realizar la autenticación en línea
                val loginUrl = "${Constants.getBaseUrl()}controllers/LoginControllerApp.php?op=verificar&dni=$dni&pass=$password" // Construir la URL correcta
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

                        // Intentar analizar la respuesta como JSONArray
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
            } else {
                // Si no hay conexión a internet, autenticar utilizando los datos locales
                val user = db.getUsuarioById(dni)
                val modifiedHash = user?.pass?.replace("\$2y\$", "\$2a\$")
                if (user != null && BCrypt.checkpw(password, modifiedHash)) {
                    saveUserEntityData(user)
                    saveAuthenticationState(true)
                    true
                } else {
                    false
                }
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

    fun saveUserEntityData(user: UsuarioEntity) {
        val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("idUsuario", user.idUsuario)
        editor.putString("userName", user.userName)
        editor.putString("rolName", user.rolName)
        editor.putString("idEstablecimiento", user.idEstablecimiento)
        editor.apply()

        with(sharedPreferences.edit()) {
            putString("dni", user.idUsuario)
            putString("nombre", user.userName)
            putString("rolname", user.rolName)
            putString("tipo_usuario", user.idRol)
            putString("nucleo_name", "N/A")
            putString("idEstablecimiento", user.idEstablecimiento)
            putString("pass", user.pass)
            apply()
        }
    }


    fun showLoginFailureMessage() {
        Toast.makeText(context, "Invalid credentials", Toast.LENGTH_SHORT).show()
    }
}
