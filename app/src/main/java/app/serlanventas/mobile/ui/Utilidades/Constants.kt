package app.serlanventas.mobile.ui.Utilidades

import android.content.Context
import android.content.SharedPreferences

object Constants {
    private const val LOGIN_PATH = "controllers/LoginControllerApp.php?op=verificar"

//    const val BASE_URL_DEV = "http://192.168.161.104/VentaPollos/"
//    const val WEB_URL_GUIA_DEV = "http://192.168.161.104/VentaPollos/view/index.php?action=NucleoGalpon"
//    const val WEB_URL_VENTAS_DEV = "http://192.168.161.104/VentaPollos/view/index.php?action=VentasApp"


    const val BASE_URL_PRODUCCION = "https://emprender.guru/sp_20393514630/"
    const val WEB_URL_GUIA_PRODUCCION = "https://emprender.guru/sp_20393514630/view/index.php?action=NucleoGalpon"
    const val WEB_URL_VENTAS_PRODUCCION = "https://emprender.guru/sp_20393514630/view/index.php?action=VentasApp"



    fun getBaseUrl(): String {
        return BASE_URL_PRODUCCION
    }

    fun getGuiaUrl(): String {
        return WEB_URL_GUIA_PRODUCCION
    }

    fun getVentasUrl(): String {
        return WEB_URL_VENTAS_PRODUCCION
    }
    // Método para construir la URL de inicio de sesión
    fun buildLoginUrl(context: Context): String {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val dni = sharedPreferences.getString("dni", "default_dni") ?: "default_dni"
        val pass = sharedPreferences.getString("pass", "default_pass") ?: "default_pass"

        val baseUrl = getBaseUrl()
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
        return sharedPreferences.getBoolean("isProduction", true) // Por defecto, es false (modo de prueba)
    }
}
