package app.serlanventas.mobile

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import app.serlanventas.mobile.VersionControl.UpdateChecker
import app.serlanventas.mobile.databinding.ActivityMainBinding
import app.serlanventas.mobile.ui.Jabas.ManagerPost
import app.serlanventas.mobile.ui.Services.getAddressMacDivice
import app.serlanventas.mobile.ui.Services.getAddressMacDivice.getDeviceModel
import app.serlanventas.mobile.ui.Utilidades.Constants
import app.serlanventas.mobile.ui.Utilidades.Constants.getCrashUrl
import app.serlanventas.mobile.ui.ViewModel.SharedViewModel
import app.serlanventas.mobile.ui.ViewModel.TabViewModel
import app.serlanventas.mobile.ui.login.LogoutReceiver
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private val updateChecker by lazy { UpdateChecker(this) }
    private lateinit var tabViewModel: TabViewModel
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private val inactivityHandler = Handler(Looper.getMainLooper())
    private val inactivityRunnable = Runnable {
        performLogout()
    }
    private var logoutTime: Long = 0L

    //    private val INACTIVITY_TIMEOUT: Long = 10000
    private val INACTIVITY_TIMEOUT: Long = 60 * 60 * 1000
    private var hasLoggedExpiration = false
    private val logHandler = Handler(Looper.getMainLooper())
    private val logRunnable = object : Runnable {
        override fun run() {
            if (hasLoggedExpiration == false) {
                logRemainingTime()
                logHandler.postDelayed(this, 1000) // Ejecutar nuevamente después de 1 segundo
            }
        }
    }
    private lateinit var alarmManager: AlarmManager
    private var sharedViewModel: SharedViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupExceptionHandler()

        // Inicializar SharedPreferences
        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        sharedViewModel = ViewModelProvider(this).get(SharedViewModel::class.java)
        handler = Handler(Looper.getMainLooper())
        runnable = Runnable {
            performLogout() // Cierra sesión si el tiempo de inactividad ha pasado
        }
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestScheduleExactAlarmPermission()
        }
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                resetInactivityTimer()
            }

            override fun onDrawerOpened(drawerView: View) {
                resetInactivityTimer()
            }

            override fun onDrawerClosed(drawerView: View) {
                resetInactivityTimer()
            }

            override fun onDrawerStateChanged(newState: Int) {
                resetInactivityTimer()
            }
        })


        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        val headerView = navView.getHeaderView(0)
//        val modeSwitch: SwitchMaterial = headerView.findViewById(R.id.modeSwitch)

        tabViewModel = ViewModelProvider(this).get(TabViewModel::class.java)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_initBluetooth,
                R.id.nav_initReportePeso,
                R.id.nav_initReporteGuias,
                R.id.nav_initNucleoGalpon,
                R.id.nav_initReporteVentasApp,
                R.id.nav_initLocalData,
                R.id.nav_impresoraConfig
            ),
            drawerLayout
        )

        navView.setupWithNavController(navController)

        drawerToggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            binding.appBarMain.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        // Actualiza el ítem seleccionado en función del destino actual
        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateNavViewMenu(navView.menu, destination)
        }

        navView.setNavigationItemSelectedListener { item ->
            fun uncheckAll(menu: Menu) {
                for (i in 0 until menu.size()) {
                    val menuItem = menu.getItem(i)
                    menuItem.isChecked = false
                    if (menuItem.hasSubMenu()) {
                        uncheckAll(menuItem.subMenu!!)
                    }
                }
            }

            uncheckAll(navView.menu)

            val currentDestination = navController.currentDestination
            when (item.itemId) {
                R.id.nav_initBluetooth -> if (currentDestination?.id != R.id.nav_initBluetooth) {
                    navController.navigate(R.id.nav_initBluetooth)
                }
                R.id.nav_initReportePeso -> if (currentDestination?.id != R.id.nav_initReportePeso) {
                    navController.navigate(R.id.nav_initReportePeso)
                }
                R.id.nav_initReporteGuias -> if (currentDestination?.id != R.id.nav_initReporteGuias) {
                    navController.navigate(R.id.nav_initReporteGuias)
                }
                R.id.nav_initNucleoGalpon -> if (currentDestination?.id != R.id.nav_initNucleoGalpon) {
                    navController.navigate(R.id.nav_initNucleoGalpon)
                }
                R.id.nav_initReporteVentasApp -> if (currentDestination?.id != R.id.nav_initReporteVentasApp) {
                    navController.navigate(R.id.nav_initReporteVentasApp)
                }
                R.id.nav_initLocalData -> if (currentDestination?.id != R.id.nav_initLocalData) {
                    navController.navigate(R.id.nav_initLocalData)
                }
                R.id.nav_impresoraConfig -> if (currentDestination?.id != R.id.nav_impresoraConfig) {
                    navController.navigate(R.id.nav_impresoraConfig)
                }
                else -> return@setNavigationItemSelectedListener false
            }

            item.isChecked = true
            drawerLayout.closeDrawers()
            true
        }

        // Actualiza el ítem de la versión en el menú del NavigationView
        updateAppVersion(navView)
        checkAndRequestInstallPermission()

        // Actualiza los datos del usuario en el nav_header
        updateUserHeader(navView)

        binding.buttonLogout.setOnClickListener {
            performLogout()
        }
        val isProductionMode = Constants.obtenerEstadoModo(this)
//        modeSwitch.isChecked = isProductionMode
//        modeSwitch.text = if (isProductionMode) {
//            getString(R.string.mode_production)
//        } else {
//            getString(R.string.mode_testing)
//        }

//        modeSwitch.setOnCheckedChangeListener { _, isChecked ->
//            restartActivity()
//            Constants.guardarEstadoModo(this, isChecked)
//            val modeText = if (isChecked) {
//                getString(R.string.mode_production)
//            } else {
//                getString(R.string.mode_testing)
//            }
//            modeSwitch.text = modeText
//        }

        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                resetInactivityTimer()
            }

            override fun onDrawerOpened(drawerView: View) {
                resetInactivityTimer()
            }

            override fun onDrawerClosed(drawerView: View) {
                resetInactivityTimer()
            }

            override fun onDrawerStateChanged(newState: Int) {
                resetInactivityTimer()
            }
        })

        resetInactivityTimer()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        resetInactivityTimer()
        return super.onTouchEvent(event)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        resetInactivityTimer()
    }

    private fun resetInactivityTimer() {
        // Eliminar operaciones redundantes
        handler.removeCallbacks(runnable)

        // Programar logout solo una vez
        handler.postDelayed(runnable, INACTIVITY_TIMEOUT)

        // Eliminar log cada segundo
        logHandler.removeCallbacks(logRunnable)
    }

    @SuppressLint("MissingPermission", "ObsoleteSdkInt")
    private fun resetLogoutTimer() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, LogoutReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, flags)

        val triggerTime = System.currentTimeMillis() + INACTIVITY_TIMEOUT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }

//        Log.d("MainActivity", "Temporizador de logout reiniciado")
    }

    @SuppressLint("NewApi")
    private fun requestScheduleExactAlarmPermission() {
        if (!alarmManager.canScheduleExactAlarms()) {
            Intent().also { intent ->
                intent.action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                startActivity(intent)
            }
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
            updateChecker.checkAndDownloadUpdate()
        }
    }

    private fun logRemainingTime() {
        val currentTime = System.currentTimeMillis()
        val remainingTime = logoutTime - currentTime

        if (remainingTime > 0) {
            val remainingSeconds = remainingTime / 1000 % 60
            val remainingMinutes = remainingTime / (1000 * 60) % 60
            val remainingHours = remainingTime / (1000 * 60 * 60)

//            Log.d("InactivityTimer", "Tiempo restante: $remainingHours horas, $remainingMinutes minutos, $remainingSeconds segundos")

            // Resetear la bandera si el tiempo restante es positivo
            hasLoggedExpiration = false
        } else {
            // Solo registrar el mensaje de expiración una vez
            if (!hasLoggedExpiration) {
                Log.d("InactivityTimer", "El tiempo de inactividad ha expirado")
                hasLoggedExpiration = true
            }
        }
    }


    @SuppressLint("ObsoleteSdkInt")
    private fun performLogout() {
        // Detener todos los temporizadores y eliminarlos
        handler.removeCallbacks(runnable)
        inactivityHandler.removeCallbacks(inactivityRunnable)
        logHandler.removeCallbacks(logRunnable)
        hasLoggedExpiration = true

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, LogoutReceiver::class.java)

        // Añadir las banderas correctas para el PendingIntent
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, flags)

        alarmManager.cancel(pendingIntent)
//        sharedPreferences = this.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        sharedPreferences = this.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        // Borra los datos del usuario en SharedPreferences
        sharedPreferences.edit().clear().apply()
        sharedPreferences.edit().putBoolean("isLoggedIn", false).apply()
        sharedPreferences.edit().putBoolean("isProduction", true).apply()

        val loginIntent = Intent(this, LoginActivity::class.java)
        loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(loginIntent)
        finish()
    }

    @SuppressLint("SetTextI18n")
    private fun updateAppVersion(navView: NavigationView) {
        val context = navView.context
        val packageManager = context.packageManager
        val packageName = context.packageName
        val versionName = try {
            val packageInfo =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getPackageInfo(
                        packageName,
                        PackageManager.PackageInfoFlags.of(0)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(packageName, 0)
                }
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            "Desconocida"
        }

        val versionTextView: TextView? = navView.findViewById(R.id.nav_version)
        versionTextView?.text = "Versión: v$versionName"
    }

    private fun updateNavViewMenu(menu: Menu, destination: NavDestination) {
        fun uncheckAll(menu: Menu) {
            for (i in 0 until menu.size()) {
                val menuItem = menu.getItem(i)
                menuItem.isChecked = false
                if (menuItem.hasSubMenu()) {
                    uncheckAll(menuItem.subMenu!!)
                }
            }
        }

        uncheckAll(menu)

        when (destination.id) {
            R.id.nav_initBluetooth -> menu.findItem(R.id.nav_initBluetooth).isChecked = true
            R.id.nav_initReportePeso -> menu.findItem(R.id.nav_initReportePeso).isChecked = true
            R.id.nav_initReporteGuias -> menu.findItem(R.id.nav_initReporteGuias).isChecked = true
            R.id.nav_initNucleoGalpon -> menu.findItem(R.id.nav_initNucleoGalpon).isChecked = true
            R.id.nav_initReporteVentasApp -> menu.findItem(R.id.nav_initReporteVentasApp).isChecked =
                true

            R.id.nav_initLocalData -> menu.findItem(R.id.nav_initLocalData).isChecked = true
            R.id.nav_initClientes -> menu.findItem(R.id.nav_initLocalData).isChecked = true
            R.id.nav_impresoraConfig -> menu.findItem(R.id.nav_initLocalData).isChecked = true
            R.id.nav_initConfConex -> menu.findItem(R.id.nav_initLocalData).isChecked = true
        }
    }

    private fun updateUserHeader(navView: NavigationView) {
        // Asegúrate de que estás usando el mismo nombre de SharedPreferences que en Login.kt
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

        val nameUser = sharedPreferences.getString("nombre", "Desconocido")
        val nameRole = sharedPreferences.getString("rolname", "Desconocido")
        val idRol = sharedPreferences.getString("tipo_usuario", "2")?.toIntOrNull() ?: 2

        val headerView = navView.getHeaderView(0)
        val textViewNameUser: TextView? = headerView.findViewById(R.id.userName)
        val textViewNameRole: TextView? = headerView.findViewById(R.id.userRole)
//        val swMode: TextView? = headerView.findViewById(R.id.modeSwitch)

//        if (idRol == 3) {
//            swMode?.visibility = View.VISIBLE
//        } else {
//            swMode?.visibility = View.GONE
//        }
        textViewNameUser?.text = nameUser
        textViewNameRole?.text = nameRole

        Log.d("UpdateUserHeader", "User Name: $nameUser")
        Log.d("UpdateUserHeader", "User Role: $nameRole")
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                drawerToggle.onOptionsItemSelected(item)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun restartActivity() {
        val idPesoShared = sharedViewModel?.getIdListPesos() ?: 0
        if (idPesoShared != 0) {
            updatePesoStatus(idPesoShared, "NotUsed")
        }
        val isProduction = Constants.obtenerEstadoModo(this)
        sharedViewModel?.setContadorJabas(0)
        sharedViewModel?.setIdListPesos(0)

        sharedViewModel?.setDataPesoPollosJson("")
        sharedViewModel?.setDataDetaPesoPollosJson("")
        handler.removeCallbacks(runnable)
        inactivityHandler.removeCallbacks(inactivityRunnable)
        logHandler.removeCallbacks(logRunnable)
        hasLoggedExpiration = true

        // Crear un intent para reiniciar la actividad
        val intent = intent
        finish() // Cerrar la actividad actual
        startActivity(intent) // Iniciar una nueva instancia de la actividad
    }

    fun updatePesoStatus(id: Int, status: String) {
        if (id != 0) {
            val idDevice = getAddressMacDivice.getDeviceId(this)
            ManagerPost.setStatusUsed(this, id, "$status", idDevice) { success ->
                if (!success) {
                    Log.d("StatusLog", "Error al cambiar el estado del peso")
                }
            }
        }
    }

    private fun setupExceptionHandler() {
        val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e("CrashHandler", "Excepción no controlada", throwable)
                val stackTrace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }

                val idDevice = getDeviceModel()
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val filename = "${idDevice}_crash_$timestamp.txt"

                val file = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    File(getExternalFilesDir(null), filename)
                } else {
                    @Suppress("DEPRECATION")
                    File(Environment.getExternalStorageDirectory(), "files/crashLogs/$filename")
                }

                try {
                    file.parentFile?.mkdirs()
                    file.writeText("Timestamp: $timestamp\n\n$stackTrace")

                    // Enviar el archivo al servidor PHP en un hilo separado
                    Thread {
                        val crashUrl = getCrashUrl()
                        Log.d("CrashHandler", "URL de envío: $crashUrl")
                        uploadFile(file, crashUrl)

                        // Pequeño retraso para asegurar que el envío se complete
                        Thread.sleep(2000)

                        // Cerrar la aplicación después de un retraso
                        defaultExceptionHandler?.uncaughtException(thread, throwable)
                    }.start()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("CrashHandler", "Error al guardar o enviar el archivo", e)
                    defaultExceptionHandler?.uncaughtException(thread, throwable)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                defaultExceptionHandler?.uncaughtException(thread, throwable)
            }
        }
        }

    private fun uploadFile(file: File, url: String) {
        val client = OkHttpClient()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody("text/plain".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Log.e("CrashHandler", "Error al enviar el archivo", e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d("CrashHandler", "Archivo enviado exitosamente")
                } else {
                    Log.e("CrashHandler", "Fallo al enviar el archivo. Código: ${response.code}")
                }
            }
        })
    }


    override fun onBackPressed() {
        // Crear un diálogo personalizado con un estilo más atractivo
        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialog)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_exit_confirmation, null)
        builder.setView(dialogView)
        val alertDialog = builder.create()
        dialogView.findViewById<Button>(R.id.btnConfirmExit).setOnClickListener {
            finishAffinity()
        }
        dialogView.findViewById<Button>(R.id.btnCancelExit).setOnClickListener {
            alertDialog.dismiss()
        }
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        alertDialog.show()
    }
}
