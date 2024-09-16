package app.serlanventas.mobile

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import app.serlanventas.mobile.VersionControl.UpdateChecker
import app.serlanventas.mobile.ui.login.LoginFragment
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.IOException


class LoginActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private val updateChecker by lazy { UpdateChecker(this) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Guardar el error en un archivo de texto
            guardarLogError(throwable)
        }

        // Inicializar SharedPreferences
        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)

        // Verificar si el usuario ya ha iniciado sesión
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

        if (isLoggedIn) {
            // Si ya está logueado, redirigir a la MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // Si no está logueado, mostrar el LoginFragment
            if (savedInstanceState == null) {
                showLoginFragment()
            }
        }
    }

    private fun guardarLogError(throwable: Throwable) {
        try {
            val logFile = File(filesDir, "crash_log.txt")
            val fileWriter = FileWriter(logFile, true)
            fileWriter.append("Error capturado: ${throwable.message}\n")
            fileWriter.append(throwable.stackTraceToString() + "\n\n")
            fileWriter.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun checkAndRequestInstallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Si el permiso aún no está habilitado
            if (!this.packageManager.canRequestPackageInstalls()) {
                // Mostrar un diálogo que el usuario no puede cancelar
                AlertDialog.Builder(this)
                    .setTitle("Permiso requerido")
                    .setMessage("Para descargar e instalar actualizaciones, necesitamos que habilites la opción de 'Instalar aplicaciones de fuentes desconocidas'.")
                    .setCancelable(false) // Impide que el diálogo sea cancelado
                    .setPositiveButton("Habilitar") { dialog, _ ->
                        // Redirigir a la configuración para habilitar el permiso
                        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        startActivity(intent)
                        dialog.dismiss()
                    }
                    .show()
            }else{
                downloadUpdate()
            }
        } else {
            // Para versiones anteriores a Android 8.0, llevar al usuario a la configuración de seguridad
            val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
            startActivity(intent)
        }
    }

    private fun downloadUpdate() {
        lifecycleScope.launch {
            updateChecker.checkAndDownloadUpdate()
        }
    }

    fun showLoginFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, LoginFragment())
            .commit()
    }

    override fun onResume() {
        super.onResume()
        checkAndRequestInstallPermission()
    }
}
