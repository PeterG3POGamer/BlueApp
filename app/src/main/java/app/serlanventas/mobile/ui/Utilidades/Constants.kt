package app.serlanventas.mobile.ui.Utilidades

import android.content.Context
import android.content.SharedPreferences

object Constants {
    private const val LOGIN_PATH = "app/controllers/LoginControllerApp.php?op=verificar"

    const val BASE_URL_DEV = "https://emprender.guru/VentaPollos2/"
    const val WEB_URL_GUIA_DEV = "https://emprender.guru/VentaPollos2/views/index.php?action=NucleoGalpon"
    const val WEB_URL_VENTAS_DEV = "https://emprender.guru/VentaPollos2/views/index.php?action=VentasApp"

    const val BASE_URL_PRODUCCION = "https://emprender.guru/sp_20393514630/"
    const val WEB_URL_GUIA_PRODUCCION = "https://emprender.guru/sp_20393514630/app/views/index.php?action=NucleoGalpon"
    const val WEB_URL_VENTAS_PRODUCCION = "https://emprender.guru/sp_20393514630/app/views/index.php?action=VentasApp"

    fun getBaseUrl(isProduction: Boolean): String {
        return if (isProduction) BASE_URL_PRODUCCION else BASE_URL_DEV
    }

    fun getGuiaUrl(isProduction: Boolean): String {
        return if (isProduction) WEB_URL_GUIA_PRODUCCION else WEB_URL_GUIA_DEV
    }

    fun getVentasUrl(isProduction: Boolean): String {
        return if (isProduction) WEB_URL_VENTAS_PRODUCCION else WEB_URL_VENTAS_DEV
    }
    // Método para construir la URL de inicio de sesión
    fun buildLoginUrl(context: Context, isProduction: Boolean): String {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val dni = sharedPreferences.getString("dni", "default_dni") ?: "default_dni"
        val pass = sharedPreferences.getString("pass", "default_pass") ?: "default_pass"

        val baseUrl = getBaseUrl(isProduction)
        return "$baseUrl$LOGIN_PATH&dni=$dni&pass=$pass"
    }


    private const val TAG = "Constants" // Etiqueta para los mensajes de Log

    fun guardarEstadoModo(context: Context, isProduction: Boolean) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("isProduction", isProduction)
        editor.apply()
    }

    fun obtenerEstadoModo(context: Context): Boolean {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("isProduction", false) // Por defecto, es false (modo de prueba)
    }
}
