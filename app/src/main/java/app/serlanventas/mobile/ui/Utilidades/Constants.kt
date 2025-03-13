package app.serlanventas.mobile.ui.Utilidades

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Constants {
    private const val LOGIN_PATH = "controllers/LoginControllerApp.php?op=verificar"

    const val BASE_URL_PRODUCCION = "https://emprender.guru/sp_20393514630/"
    const val WEB_URL_GUIA_PRODUCCION =
        "https://emprender.guru/sp_20393514630/view/index.php?action=NucleoGalpon"
    const val WEB_URL_VENTAS_PRODUCCION =
        "https://emprender.guru/sp_20393514630/view/index.php?action=VentasApp"
    const val URL_CRASH_SYNC =
        "https://emprender.guru/APIS_REST/api_log_crash/api.php"

    fun getBaseUrl(): String {
        return BASE_URL_PRODUCCION
    }

    fun getCrashUrl(): String {
        return URL_CRASH_SYNC
    }

    fun getGuiaUrl(): String {
        return WEB_URL_GUIA_PRODUCCION
    }

    fun getVentasUrl(): String {
        return WEB_URL_VENTAS_PRODUCCION
    }

    // Método para construir la URL de inicio de sesión
    fun buildLoginUrl(context: Context): String {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val dni = sharedPreferences.getString("dni", "default_dni") ?: "default_dni"
        val pass = sharedPreferences.getString("pass", "default_pass") ?: "default_pass"

        val baseUrl = getBaseUrl()
        return "$baseUrl$LOGIN_PATH&dni=$dni&pass=$pass"
    }

    private const val TAG = "Constants" // Etiqueta para los mensajes de Log

    fun guardarEstadoModo(context: Context, isProduction: Boolean) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("isProduction", isProduction)
        editor.apply()
    }

    fun obtenerEstadoModo(context: Context): Boolean {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(
            "isProduction",
            true
        ) // Por defecto, es false (modo de prueba)
    }

    fun getCurrentDateTime(): String {
        // Define el formato de fecha y hora que deseas usar
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        // Obtiene la fecha y hora actual del sistema
        val currentDate = Date()
        // Formatea la fecha y hora como una cadena
        return dateFormat.format(currentDate)
    }
}
