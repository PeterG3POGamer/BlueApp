package app.serlanventas.mobile

import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import app.serlanventas.mobile.VersionControl.UpdateChecker
import app.serlanventas.mobile.VersionControl.UpdateManager
import app.serlanventas.mobile.ui.DataSyncManager.DataSyncManager
import app.serlanventas.mobile.ui.Interfaces.ProgressCallback
import app.serlanventas.mobile.ui.Services.getAddressMacDivice.getDeviceModel
import app.serlanventas.mobile.ui.Utilidades.Constants
import app.serlanventas.mobile.ui.Utilidades.NetworkChangeReceiver
import app.serlanventas.mobile.ui.login.LoginFragment
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LoginActivity : AppCompatActivity(), ProgressCallback {
    private lateinit var sharedPreferences: SharedPreferences
    private val updateChecker by lazy { UpdateChecker(this) }
    private lateinit var progressGif: ImageView
    private lateinit var statusMessage: TextView
    private lateinit var progressDetails: TextView
    private var isProduction: Boolean = false
    private var baseUrl: String = ""
    private lateinit var dataSyncManager: DataSyncManager
    private lateinit var networkChangeReceiver: NetworkChangeReceiver

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
        baseUrl = Constants.getBaseUrl()
        dataSyncManager = DataSyncManager(this)
        // Inicializar vistas de progreso
        progressGif = findViewById(R.id.progress_gif)
        statusMessage = findViewById(R.id.status_message)
        progressDetails = findViewById(R.id.progress_details)
        dataSyncManager.setProgressGif(progressGif)

        // Mostrar GIF mientras se sincronizan los datos
        Glide.with(this).asGif().load(R.drawable.icon_loading2).into(progressGif)
        statusMessage.visibility = View.VISIBLE
        progressDetails.visibility = View.VISIBLE

        // Primero, verificar si es necesario sincronizar los datos
        networkChangeReceiver = NetworkChangeReceiver { isConnected ->
            if (isFinishing || isDestroyed) return@NetworkChangeReceiver

            if (isConnected) {
                dataSyncManager.checkSincronizarData(baseUrl, isLoggedIn, this) { success ->
                    if (!isFinishing && !isDestroyed && success) {
                        navigateBasedOnLoginState(isLoggedIn)
                    }
                }
            } else {
                if (!isFinishing && !isDestroyed) {
                    navigateBasedOnLoginState(isLoggedIn)
                }
            }
        }
    }

    private fun navigateBasedOnLoginState(isLoggedIn: Boolean) {
        if (isFinishing || isDestroyed) {
            Log.d("LoginActivity", "Actividad destruida, ignorando navegación")
            return
        }

        runOnUiThread {
            // Doble verificación por si cambió el estado mientras se programaba en el UI thread
            if (isFinishing || isDestroyed) return@runOnUiThread

            if (isLoggedIn) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                findViewById<View>(R.id.fragment_container).visibility = View.VISIBLE
                if (supportFragmentManager.findFragmentById(R.id.fragment_container) == null) {
                    showLoginFragment()
                }
            }
        }
    }

    fun showLoginFragment() {
        try {
            if (!isFinishing && !isDestroyed) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, LoginFragment())
                    .commitAllowingStateLoss() // Usamos commitAllowingStateLoss
            }
        } catch (e: IllegalStateException) {
            Log.e("LoginActivity", "Error al mostrar fragmento", e)
        }
    }

    private fun forceCrash() {
        throw RuntimeException("¡Esto es un crasheo de prueba!")
    }

    private fun setupExceptionHandler() {
        val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e("CrashHandler", "Excepción no controlada", throwable)
                val stackTrace = StringWriter()
                throwable.printStackTrace(PrintWriter(stackTrace))

                val idDevice = getDeviceModel()
                val timestamp =
                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val filename = "${idDevice}_crash_$timestamp.txt"

                val file = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    File(getExternalFilesDir(null), filename)
                } else {
                    @Suppress("DEPRECATION")
                    (File(
                        Environment.getExternalStorageDirectory(),
                        "files/crashLogs/$filename"
                    ))
                }

                try {
                    file.parentFile?.mkdirs()
                    file.writeText("Timestamp: $timestamp\n\n$stackTrace")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            defaultExceptionHandler?.uncaughtException(thread, throwable)
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
            UpdateManager.setCurrentActivity(this@LoginActivity)
            updateChecker.checkAndDownloadUpdate()
        }
    }

    override fun onPause() {
        super.onPause()
        this@LoginActivity.unregisterReceiver(networkChangeReceiver)

    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        this@LoginActivity.registerReceiver(networkChangeReceiver, filter)

        // Solo verificar actualización si no hay descarga en curso
        if (!UpdateManager.isDownloading()) {
            checkAndRequestInstallPermission()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            UpdateManager.cleanup(applicationContext)
        }
    }

    override fun onProgressUpdate(message: String) {
        runOnUiThread {
            statusMessage.text = "Sincronización en curso"
            progressDetails.text = message
        }
    }
}
