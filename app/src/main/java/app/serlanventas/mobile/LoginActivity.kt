package app.serlanventas.mobile

import NetworkUtils
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import app.serlanventas.mobile.VersionControl.UpdateChecker
import app.serlanventas.mobile.ui.DataSyncManager.DataSyncManager
import app.serlanventas.mobile.ui.DataSyncManager.SyncResult
import app.serlanventas.mobile.ui.Jabas.ManagerPost.showCustomToast
import app.serlanventas.mobile.ui.Services.getAddressMacDivice
import app.serlanventas.mobile.ui.Utilidades.Constants
import app.serlanventas.mobile.ui.login.LoginFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LoginActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private val updateChecker by lazy { UpdateChecker(this) }
    private lateinit var progressBar: ProgressBar
    private lateinit var statusMessage: TextView
    private lateinit var progressDetails: TextView
    private var isProduction: Boolean = false
    private var baseUrl: String = ""
    private lateinit var dataSyncManager: DataSyncManager

    // Mueve la inicialización de isLoggedIn dentro de onCreate
    private var isLoggedIn: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        setupExceptionHandler()

        // Inicializar SharedPreferences
        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)

        // Inicializar isLoggedIn después de que sharedPreferences esté inicializada
        isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

        isProduction = Constants.obtenerEstadoModo(this)
        baseUrl = Constants.getBaseUrl(isProduction)
        dataSyncManager = DataSyncManager(this)

        // Inicializar vistas de progreso
        progressBar = findViewById(R.id.progress_bar)
        statusMessage = findViewById(R.id.status_message)
        progressDetails = findViewById(R.id.progress_details)

        // Mostrar ProgressBar mientras se sincronizan los datos
        progressBar.visibility = View.VISIBLE
        statusMessage.visibility = View.VISIBLE
        progressDetails.visibility = View.VISIBLE

        // Primero, verificar si es necesario sincronizar los datos
        if (NetworkUtils.isNetworkAvailable(this)) {
            lifecycleScope.launch {
                updateProgress("Iniciando sincronización...")
                delay(1000)

                val idDevice = getAddressMacDivice.getDeviceId(this@LoginActivity)

                dataSyncManager.sincronizarData(baseUrl, idDevice) { result ->
                    when (result) {
                        is SyncResult.Success -> {
                            if (result.needsSync) {
                                updateProgress("Es necesario sincronizar datos...")
                                dataSyncManager.showSyncConfirmationDialog { shouldSync ->
                                    if (shouldSync) {
                                        updateProgress("Sincronizando datos...")
                                        dataSyncManager.sincronizarData(baseUrl, idDevice) { syncResult ->
                                            handleSyncResult(syncResult)
                                        }
                                    } else {
                                        showCustomToast(this@LoginActivity, "No se sincronizarán los datos.", "info")
                                        navigateBasedOnLoginState(isLoggedIn)
                                    }
                                }
                            } else {
                                showCustomToast(this@LoginActivity, "Sus datos están sincronizados.", "success")
                                navigateBasedOnLoginState(isLoggedIn)
                            }
                        }
                        is SyncResult.Error -> {
                            dataSyncManager.showErrorDialog { retry ->
                                if (retry) {
                                    dataSyncManager.sincronizarData(baseUrl, idDevice) { syncResult ->
                                        handleSyncResult(syncResult)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            navigateBasedOnLoginState(isLoggedIn)
        }
    }

    private fun handleSyncResult(result: SyncResult) {
        when (result) {
            is SyncResult.Success -> {
                if (result.needsSync) {
                    dataSyncManager.showSuccessDialog {
                        navigateBasedOnLoginState(isLoggedIn)
                    }
                }else{
                    navigateBasedOnLoginState(isLoggedIn)
                }
            }
            is SyncResult.Error -> {
                dataSyncManager.showErrorDialog(null)
            }
        }
    }

    private fun navigateBasedOnLoginState(isLoggedIn: Boolean) {
        // Ocultar ProgressBar y mostrar el contenido principal
        progressBar.visibility = View.GONE
        statusMessage.visibility = View.GONE
        progressDetails.visibility = View.GONE

        if (isLoggedIn) {
            // Si ya está logueado, redirigir a la MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // Si no está logueado, mostrar el LoginFragment
            findViewById<View>(R.id.fragment_container).visibility = View.VISIBLE
            if (supportFragmentManager.findFragmentById(R.id.fragment_container) == null) {
                showLoginFragment()
            }
        }
    }

    private fun updateProgress(message: String) {
        statusMessage.text = "Autenticando..."
        progressDetails.text = message
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
            } else {
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

    private fun forceCrash() {
        throw RuntimeException("¡Esto es un crasheo de prueba!")
    }

    private fun setupExceptionHandler() {
        val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val stackTrace = StringWriter()
            throwable.printStackTrace(PrintWriter(stackTrace))

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "crash_$timestamp.txt"

            val file = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), filename)
            } else {
                @Suppress("DEPRECATION")
                File(Environment.getExternalStorageDirectory(), "files/crashLogs/$filename")
            }

            try {
                file.parentFile?.mkdirs()
                file.writeText("Timestamp: $timestamp\n\n$stackTrace")
            } catch (e: Exception) {
                e.printStackTrace()
            }

            defaultExceptionHandler?.uncaughtException(thread, throwable)
        }
    }
}
