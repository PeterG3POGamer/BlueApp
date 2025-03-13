package app.serlanventas.mobile.ui.Jabas

import NetworkUtils
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import app.serlanventas.mobile.R
import app.serlanventas.mobile.ui.DataBase.AppDatabase
import app.serlanventas.mobile.ui.DataBase.Entities.DataDetaPesoPollosEntity
import app.serlanventas.mobile.ui.DataBase.Entities.DataPesoPollosEntity
import app.serlanventas.mobile.ui.DataBase.Entities.GalponEntity
import app.serlanventas.mobile.ui.DataBase.Entities.NucleoEntity
import app.serlanventas.mobile.ui.DataBase.Entities.PesosEntity
import app.serlanventas.mobile.ui.DataBase.Entities.pesoUsedEntity
import app.serlanventas.mobile.ui.DataSyncManager.ApiService
import app.serlanventas.mobile.ui.Services.generateAndOpenPDF2
import app.serlanventas.mobile.ui.Services.getAddressMacDivice
import app.serlanventas.mobile.ui.Services.getAddressMacDivice.getDeviceId
import app.serlanventas.mobile.ui.Services.showNotification
import app.serlanventas.mobile.ui.Services.showSuccessNotification
import app.serlanventas.mobile.ui.Utilidades.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume

//ManagerPost.kt
object ManagerPost {
    fun saveLocally(
        context: Context,
        dataDetaPesoPollos: List<DataDetaPesoPollosEntity>,
        dataPesoPollos: DataPesoPollosEntity,
        numeroDocCliente: String,
        nombreCompleto: String,
        idNucleo: String
    ) {

        val numeroDocumento = numeroDocCliente
        val nombreCliente = nombreCompleto

        if (numeroDocumento.isEmpty() || nombreCliente.isEmpty()) {
            showNotification(
                context,
                "Registrar Cliente",
                "Es necesario registrar el número y el nombre del cliente."
            )
            return
        }

        val db = AppDatabase(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val serie = db.getSerieDevice()
                if (serie != null) {
                    var ultimoNumero = db.getUltimoNumeroSerie(serie.codigo)
                    if (ultimoNumero == null) {
                        ultimoNumero = "0"
                    }
                    val nuevoNumero = ultimoNumero.toInt() + 1

                    val idPesoPollo = db.insertDataPesoPollos(
                        dataPesoPollos.copy(
                            numeroDocCliente = numeroDocumento,
                            nombreCompleto = nombreCliente,
                            serie = serie.codigo,
                            numero = nuevoNumero.toString(),
                            idEstado = "0",
                            idNucleo = idNucleo
                        )
                    )

                    if (idPesoPollo != -1L) {
                        dataDetaPesoPollos.forEach {
                            val detaPeso = it.copy(idPesoPollo = idPesoPollo.toString())
                            val result = db.insertDataDetaPesoPollos(detaPeso)
                            if (result == -1L) {
                                Log.e(
                                    "ManagerPost",
                                    "Error al insertar el registro en la tabla de peso pollos"
                                )
                            }
                        }
                    } else {
                        Log.e(
                            "ManagerPost",
                            "Error al insertar el registro en la tabla de peso pollos"
                        )
                    }
                    withContext(Dispatchers.Main) {
                        showSuccessNotification(context)
                    }

                    val pesoPollos = db.obtenerPesoPollosPorId(idPesoPollo.toInt())
                    val detallesPesoPollos = db.obtenerDetaPesoPollosPorId(idPesoPollo.toString())

                    pesoPollos?.let { it ->
                        val dataNucleo = db.obtenerNucleoPorId(it.idNucleo)
                        val dataGalpon = db.obtenerGalponPorId(it.idGalpon)

                        val totalPollos = it.totalPollos.toIntOrNull() ?: 0
                        val totalNeto = it.totalNeto.toDoubleOrNull() ?: 0.0
                        val pesoPromedio = if (totalPollos > 0) String.format(
                            "%.2f",
                            totalNeto / totalPollos
                        ) else "0.00"
                        val correlativo = "${it.serie} - ${it.numero}"
                        // Crear JSON
                        val DATAPDF = JSONObject().apply {
                            put("PESO_POLLO", JSONArray().put(JSONObject().apply {
                                put("serie", correlativo)
                                put("fecha", it.fecha)
                                put("totalJabas", it.totalJabas)
                                put("totalPollos", totalPollos.toString())
                                put("totalPeso", it.totalPeso)
                                put("tara", it.totalPesoJabas)
                                put("neto", it.totalNeto)
                                put("precio_kilo", it.PKPollo)
                                put("pesoPromedio", pesoPromedio)
                                put("total_pagar", it.TotalPagar)
                            }))

                            put("CLIENTE", JSONArray().put(JSONObject().apply {
                                put("dni", it.numeroDocCliente ?: "N/A")
                                put("rs", it.nombreCompleto ?: "N/A")
                            }))

                            put("GALPON", JSONArray().put(JSONObject().apply {
                                put("nomgal", dataGalpon?.nombre ?: "N/A")
                            }))

                            put("ESTABLECIMIENTO", JSONArray().put(JSONObject().apply {
                                put("nombre", dataNucleo?.nombre ?: "N/A")
                            }))

                            put("EMPRESA", JSONArray().put(JSONObject().apply {
                                put("nroRuc", dataNucleo?.idEmpresa ?: "N/A")
                                put("nombreComercial", "MULTIGRANJAS SERLAN S.A.C.")
                            }))

                            put("DETA_PESOPOLLO", JSONArray().apply {
                                detallesPesoPollos.forEach { detalle ->
                                    put(JSONObject().apply {
                                        put("cantJabas", detalle.cantJabas)
                                        put("cantPollos", detalle.cantPollos)
                                        put("peso", detalle.peso)
                                        put("tipo", detalle.tipo)
                                    })
                                }
                            })
                        }

                        generateAndOpenPDF2(DATAPDF, context)

                    }
                } else {
                    Log.e(
                        "ManagerPost",
                        "No se encontró una serie de dispositivo válida"
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showNotification(
                        context,
                        "Error al guardar localmente",
                        e.message ?: "Error desconocido"
                    )
                }
            }
        }


    }


    fun captureData(jabasList: List<JabasItem>): List<DataDetaPesoPollosEntity> {
        return jabasList.map {
            DataDetaPesoPollosEntity(
                idDetaPP = it.id,
                cantJabas = it.numeroJabas,
                cantPollos = it.numeroPollos,
                peso = it.pesoKg,
                tipo = it.conPollos,
                idPesoPollo = it.idPesoPollo,
                fechaPeso = it.fechaPeso
            )
        }
    }

    fun captureDataPesoPollos(
        id: Int,
        serie: String,
        numero: String,
        fecha: String,
        totalJabas: String,
        totalPollos: String,
        totalPeso: String,
        tipo: String,
        numeroDocCliente: String,
        nombreCompleto: String,
        idGalpon: String,
        idNucleo: String,
        PKPollo: String,
        totalPesoJabas: String,
        totalNeto: String,
        totalPagar: String,
        idUsuario: String,
        idEstado: String,
    ): DataPesoPollosEntity {
        return DataPesoPollosEntity(
            id = id,
            serie = serie,
            numero = numero,
            fecha = fecha,
            totalJabas = totalJabas,
            totalPollos = totalPollos,
            totalPeso = totalPeso,
            tipo = tipo,
            numeroDocCliente = numeroDocCliente,
            nombreCompleto = nombreCompleto,
            idGalpon = idGalpon,
            idNucleo = idNucleo,
            PKPollo = PKPollo,
            totalPesoJabas = totalPesoJabas,
            totalNeto = totalNeto,
            TotalPagar = totalPagar,
            idUsuario = idUsuario,
            idEstado = idEstado,
        )
    }

    @SuppressLint("InflateParams", "SetTextI18n")
    var lastPopupWindow: PopupWindow? = null

    @SuppressLint("ResourceType", "ClickableViewAccessibility")
    fun showCustomToast(context: Context, message: String, type: String) {
        if (context == null || (context is Activity && (context.isFinishing || context.isDestroyed))) {
            return  // No mostrar toast si el contexto no es válido
        }
        val layoutInflater = LayoutInflater.from(context)
        val layout = layoutInflater.inflate(R.layout.toast_custom, null)

        // Configurar el icono y el mensaje
        val toastIcon = layout.findViewById<ImageView>(R.id.toast_icon)
        val toastMessage = layout.findViewById<TextView>(R.id.toast_message)

        // Configurar el estilo y el icono en función del tipo de Toast
        when (type) {
            "success" -> {
                toastIcon.setImageResource(R.drawable.ic_success)
                layout.background = ContextCompat.getDrawable(
                    context,
                    R.drawable.toast_background_success
                )
            }

            "info" -> {
                toastIcon.setImageResource(R.drawable.ic_info)
                layout.background = ContextCompat.getDrawable(
                    context,
                    R.drawable.toast_background_info
                )
            }

            "error" -> {
                toastIcon.setImageResource(R.drawable.ic_error)
                layout.background = ContextCompat.getDrawable(
                    context,
                    R.drawable.toast_background_error
                )
            }

            "warning" -> {
                toastIcon.setImageResource(R.drawable.ic_warning)
                layout.background = ContextCompat.getDrawable(
                    context,
                    R.drawable.toast_background_warning
                )
            }

            else -> {
                toastIcon.visibility = View.GONE
                layout.background = ContextCompat.getDrawable(
                    context,
                    R.drawable.toast_background_default
                )
            }
        }

        toastMessage.text = message

        lastPopupWindow?.dismiss()

        val popupWindow = PopupWindow(
            layout,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        popupWindow.showAtLocation(layout, Gravity.TOP, 0, 0)
        lastPopupWindow = popupWindow

        // Cargar la animación desde el archivo XML
        val slideDown = AnimationUtils.loadAnimation(context, R.anim.slide_down)

        // Aplicar la animación al layout
        layout.startAnimation(slideDown)

        // Variables para detectar el gesto de deslizamiento
        var initialX = 0f
        var initialY = 0f
        val SWIPE_THRESHOLD = 10

        // Configurar el detector de gestos para permitir SOLO deslizar de izquierda a derecha
        layout.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = event.x
                    initialY = event.y
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val diffX = event.x - initialX

                    // Solo reaccionar a deslizamientos de izquierda a derecha (diffX positivo)
                    if (diffX > SWIPE_THRESHOLD) {
                        // Animar el deslizamiento completo hacia la derecha
                        val animator = ObjectAnimator.ofFloat(
                            view,
                            "translationX",
                            diffX,
                            view.width.toFloat()
                        )
                        animator.duration = 200
                        animator.addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                popupWindow.dismiss()
                            }
                        })
                        animator.start()
                        return@setOnTouchListener true
                    } else {
                        // Si no es un deslizamiento hacia la derecha válido, volver a la posición original
                        val animator =
                            ObjectAnimator.ofFloat(view, "translationX", view.translationX, 0f)
                        animator.duration = 100
                        animator.start()
                    }
                    false
                }

                MotionEvent.ACTION_MOVE -> {
                    val diffX = event.x - initialX

                    // Solo permitir movimiento horizontal hacia la derecha
                    if (diffX > 0) {
                        view.translationX = diffX
                    }
                    true
                }

                else -> false
            }
        }

        // Auto-dismiss después de un tiempo si el usuario no lo desliza
        layout.postDelayed({
            if (popupWindow.isShowing) {
                val fadeOut = AlphaAnimation(1f, 0f)
                fadeOut.duration = 500
                fadeOut.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}
                    override fun onAnimationEnd(animation: Animation?) {
                        popupWindow.dismiss()
                    }

                    override fun onAnimationRepeat(animation: Animation?) {}
                })
                layout.startAnimation(fadeOut)
            }
        }, 3000) // 3 segundos antes de auto-dismiss

        // Vibración al mostrar el Toast personalizado
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    300,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(300)
        }
    }

    // Función para buscar cliente en el servidor ManagerPost.kt
    fun BuscarCliente(baseUrl: String, parametros: String, callback: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(baseUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.doOutput = true

                // Escribir JSON en el cuerpo de la solicitud
                val wr = OutputStreamWriter(conn.outputStream)
                wr.write(parametros)
                wr.flush()

                // Leer la respuesta del servidor
                val responseCode = conn.responseCode
                val responseMessage = conn.responseMessage

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Leer la respuesta del servidor
                    val inputStream = conn.inputStream.bufferedReader().use { it.readText() }

                    try {
                        // Intenta convertir la respuesta a JSON
                        val jsonResponse = JSONObject(inputStream)
                        val status = jsonResponse.optString("status")
                        val message = jsonResponse.optString("message")
                        val nombreCompleto = jsonResponse.optString("nombreCompleto")
                        withContext(Dispatchers.Main) {
                            when (status) {
                                "success" -> {
                                    Log.d(
                                        "ManagerPost",
                                        "Nombre completo del cliente: $nombreCompleto"
                                    )
                                    callback(nombreCompleto)
                                }

                                "error" -> {
                                    Log.d("ManagerPost", "Error de la Web: $message")
                                    callback(null)
                                }

                                "info" -> {
                                    Log.d("ManagerPost", "Información de la Web: $message")
                                    callback(null)
                                }

                                else -> {
                                    Log.e("ManagerPost", "Error: $message")
                                    callback(null)
                                }
                            }
                        }
                    } catch (e: JSONException) {
                        withContext(Dispatchers.Main) {
                            Log.e(
                                "ManagerPost",
                                "Error al convertir la respuesta a JSON: $inputStream",
                                e
                            )
                            callback(null)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Log.e("ManagerPost", "Error al enviar datos: $responseCode")
                        callback(null)
                    }
                }

            } catch (ex: Exception) {
                ex.printStackTrace()
                withContext(Dispatchers.Main) {
                    Log.e("ManagerPost", "Error: ${ex.message}")
                    callback(null)
                }
            }
        }
    }

    suspend fun showInputDialog(context: Context): String? {
        return suspendCancellableCoroutine { continuation ->
            val inputEditText = EditText(context).apply {
                inputType = InputType.TYPE_TEXT_FLAG_CAP_WORDS
                filters = arrayOf(InputFilter.AllCaps())
            }

            val dialog = AlertDialog.Builder(context)
                .setTitle("Ingresar Apellido y Nombre")
                .setView(inputEditText)
                .setPositiveButton("Aceptar", null)
                .setNegativeButton("Cancelar") { dialog, which ->
                    continuation.resume(null)
                    dialog.dismiss()
                }
                .setCancelable(false)
                .create()

            dialog.setOnCancelListener {
                continuation.resume(null)
            }

            dialog.setOnShowListener {
                val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                button.isEnabled = false // Deshabilitar inicialmente

                inputEditText.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        button.isEnabled = !s.isNullOrBlank()
                    }

                    override fun afterTextChanged(s: Editable?) {}
                })

                button.setOnClickListener {
                    val nombreCompleto = inputEditText.text.toString().trim()
                    continuation.resume(nombreCompleto)
                    dialog.dismiss()
                }
            }
        }
    }

    // =============================================
    // SECCION: SELECT PARA NUCLEOS Y GALPONES
    // =============================================

    fun getSelectGalpon(
        idNucleo: String,
        baseUrl: String,
        context: Context,
        callback: (List<GalponEntity>?) -> Unit
    ) {
        val db = AppDatabase(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val galponList = getGalponFromLocalDatabase(db, idNucleo)
                withContext(Dispatchers.Main) {
                    callback(galponList)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                withContext(Dispatchers.Main) {
                    Log.e("ManagerPost", "Error al obtener datos locales: ${ex.message}")
                    callback(null)
                }
            }
        }

//        if (NetworkUtils.isNetworkAvailable(context)) {
//            val urlString =
//                "${baseUrl}controllers/PesoPollosController.php?op=getSelectGalpon&idNucleo=$idNucleo"
//
//            CoroutineScope(Dispatchers.IO).launch {
//                try {
//                    val url = URL(urlString)
//                    val conn = url.openConnection() as HttpURLConnection
//                    conn.requestMethod = "GET"
//                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
//
//                    val responseCode = conn.responseCode
//                    val responseMessage = conn.responseMessage
//
//                    if (responseCode == HttpURLConnection.HTTP_OK) {
//                        val inputStream = conn.inputStream.bufferedReader().use { it.readText() }
//
//                        try {
//                            val jsonResponse = JSONObject(inputStream)
//                            val status = jsonResponse.optString("status")
//                            val message = jsonResponse.optString("message")
//
//                            if (status == "success") {
//                                val dataGalpones = jsonResponse.optJSONArray("dataGalpones")
//                                val galponesList = mutableListOf<GalponEntity>()
//
//                                for (i in 0 until dataGalpones.length()) {
//                                    val galponObj = dataGalpones.optJSONObject(i)
//                                    val idGalpon = galponObj.optInt("idgalpones")
//                                    val nombre = galponObj.optString("nomgal")
//                                    val galpon = GalponEntity(idGalpon, nombre, "")
//                                    galponesList.add(galpon)
//                                }
//
//                                withContext(Dispatchers.Main) {
//                                    callback(galponesList)
//                                }
//                            } else {
//                                withContext(Dispatchers.Main) {
//                                    callback(null)
//                                }
//                            }
//                        } catch (e: JSONException) {
//                            withContext(Dispatchers.Main) {
//                                Log.e(
//                                    "ManagerPost",
//                                    "Error al convertir la respuesta a JSON: $inputStream",
//                                    e
//                                )
//                                callback(null)
//                            }
//                        }
//                    } else {
//                        withContext(Dispatchers.Main) {
//                            Log.e("ManagerPost", "Error al obtener datos: $responseCode")
//                            callback(null)
//                        }
//                    }
//                } catch (ex: Exception) {
//                    ex.printStackTrace()
//                    withContext(Dispatchers.Main) {
//                        Log.e("ManagerPost", "Error: ${ex.message}")
//                        callback(null)
//                    }
//                }
//            }
//        } else {
//            // Si no hay conexión a internet, obtener datos de la base de datos local
//            CoroutineScope(Dispatchers.IO).launch {
//                try {
//                    val galponList = getGalponFromLocalDatabase(db, idNucleo)
//                    withContext(Dispatchers.Main) {
//                        callback(galponList)
//                    }
//                } catch (ex: Exception) {
//                    ex.printStackTrace()
//                    withContext(Dispatchers.Main) {
//                        Log.e("ManagerPost", "Error al obtener datos locales: ${ex.message}")
//                        callback(null)
//                    }
//                }
//            }
//        }
    }


    fun getNucleos(
        baseUrl: String,
        context: Context,
        callback: (List<NucleoEntity>?) -> Unit
    ) {
        val urlString = "${baseUrl}controllers/PesoPollosController.php?op=getNucleo"
        val db = AppDatabase(context)

        // Si no hay conexión a internet, obtener datos de la base de datos local
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val nucleosList = getNucleosFromLocalDatabase(db)
                withContext(Dispatchers.Main) {
                    callback(nucleosList)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                withContext(Dispatchers.Main) {
                    Log.e("ManagerPost", "Error al obtener datos locales: ${ex.message}")
                    callback(null)
                }
            }
        }
    }

    suspend fun getNucleosFromLocalDatabase(db: AppDatabase): List<NucleoEntity> {
        return withContext(Dispatchers.IO) {
            val nucleosList = mutableListOf<NucleoEntity>()
            val localNucleos = db.getAllNucleos()
            localNucleos.forEach { nucleo ->
                nucleosList.add(
                    NucleoEntity(
                        nucleo.idEstablecimiento,
                        nucleo.nombre, ""
                    )
                )
            }
            nucleosList
        }
    }

    suspend fun getGalponFromLocalDatabase(db: AppDatabase, idNucleo: String): List<GalponEntity> {
        return withContext(Dispatchers.IO) {
            val galponList = mutableListOf<GalponEntity>()
            val localGalpones = db.getGalponesForByIdNucleo(idNucleo)
            localGalpones.forEach { galpon ->
                galponList.add(
                    GalponEntity(
                        galpon.idGalpon,
                        galpon.nombre,
                        galpon.idEstablecimiento
                    )
                )
            }
            galponList
        }
    }

    // =============================================
    // SECCION: LISTA DE PESOS GET, ADD, DELETE
    // =============================================

    fun getListPesosByIdGalpon(
        baseUrl: String,
        context: Context,
        idGalpon: Int,
        idEstablecimiento: Int,
        diviceName: String,
        callback: (List<PesosEntity>?) -> Unit
    ) {
        val urlString =
            "${baseUrl}controllers/TempPesoPollosController.php?op=getListPesosByIdGalpon&idGalpon=$idGalpon&idEstablecimiento=${idEstablecimiento}&diviceName=$diviceName"
        val db = AppDatabase(context)
        val pesosList = db.getPesosByIdGalponAndEstablecimiento(idGalpon, idEstablecimiento)
        callback(pesosList)
    }


    suspend fun addListPesos(
        context: Context,
        fragment: JabasFragment,
        pesosEntity: PesosEntity
    ): Boolean = withContext(Dispatchers.IO) {
        // Insertar en la base de datos local
        val db = AppDatabase(context)
        val serie = pesosEntity.serieDevice

        var numSeriePeso = db.getUltimoNumeroSeriePeso(serie)

        if (numSeriePeso == null) {
            numSeriePeso = "0"
        }
        val partes = numSeriePeso.split('-')
        val numeroActual = if (partes.size > 1) partes[1].toIntOrNull() ?: 0 else 0

        val nuevoNumero = numeroActual + 1
        val newSerie = "$serie-$nuevoNumero"

        val pesoUsedEntity = PesosEntity(
            id = 0,
            idNucleo = pesosEntity.idNucleo,
            idGalpon = pesosEntity.idGalpon,
            numeroDocCliente = pesosEntity.numeroDocCliente,
            nombreCompleto = pesosEntity.nombreCompleto,
            dataPesoJson = pesosEntity.dataPesoJson,
            dataDetaPesoJson = pesosEntity.dataDetaPesoJson,
            idEstado = "0",
            isSync = pesosEntity.isSync,
            devicedName = pesosEntity.devicedName,
            serieDevice = newSerie,
            fechaRegistro = ""
        )
        val result = db.insertListPesos(pesoUsedEntity)

        if (result != -1L) {
            withContext(Dispatchers.Main) {
                showCustomToast(context, "Peso guardado", "success")
            }
        } else {
            withContext(Dispatchers.Main) {
                showCustomToast(context, "Error al guardar datos localmente", "error")
            }
            return@withContext false
        }

        val baseUrl = Constants.getBaseUrl()
        val urlString = "${baseUrl}controllers/TempPesoPollosController.php?op=insertar"
//         Intentar enviar al servidor remoto
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.doOutput = true

                val loadPesoEntity = PesosEntity(
                    id = 0,
                    idNucleo = pesosEntity.idNucleo,
                    idGalpon = pesosEntity.idGalpon,
                    numeroDocCliente = pesosEntity.numeroDocCliente,
                    nombreCompleto = pesosEntity.nombreCompleto,
                    dataPesoJson = pesosEntity.dataPesoJson,
                    dataDetaPesoJson = pesosEntity.dataDetaPesoJson,
                    idEstado = "0",
                    isSync = "1",
                    devicedName = pesosEntity.devicedName,
                    serieDevice = newSerie,
                    fechaRegistro = ""
                )

                // Convertir PesosEntity a JSON
                val jsonInputString = loadPesoEntity.toJson().toString()

                // Escribir JSON en el cuerpo de la solicitud
                conn.outputStream.use { os ->
                    val input = jsonInputString.toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = conn.inputStream.bufferedReader().use { it.readText() }
                    Log.d("AddListPesos", "Response: $inputStream")

                    val jsonResponse = JSONObject(inputStream)
                    val status = jsonResponse.optString("status")

                    if (status == "success") {
                        withContext(Dispatchers.Main) {
                            showCustomToast(context, "Datos enviados correctamente", "success")
                        }
                        db.updateStatusPesoSync(newSerie)
                        return@withContext true
                    } else {
                        val message = jsonResponse.optString("message")
                        Log.e("AddListPesos", "Error: $message")
                    }
                } else {
                    Log.e("addListPesos", "Server response: $responseCode ${conn.responseMessage}")
                }
            } catch (ex: Exception) {
                Log.e("addListPesos", "Network exception: ${ex.message}")
            }
        }

        // Si falla el envío al servidor, los datos ya están guardados localmente
        return@withContext true
    }

    suspend fun updateListPesos(
        context: Context,
        fragment: JabasFragment,
        pesosEntity: PesosEntity,
        idPesoShared: Int
    ): Boolean = withContext(Dispatchers.IO) {
        val idDevice = getDeviceId(context)
        val pesoUsedEntity = pesoUsedEntity(
            idPesoUsed = idPesoShared,
            devicedName = idDevice,
            dataPesoPollosJson = pesosEntity.dataPesoJson,
            dataDetaPesoPollosJson = pesosEntity.dataDetaPesoJson,
            fechaRegistro = ""
        )
        val db = AppDatabase(context)

        if (idPesoShared == 0) {
            val result = db.addPesoUsed(pesoUsedEntity)
            if (result == -1L) {
                withContext(Dispatchers.Main) {
                    showCustomToast(context, "Error al usar el peso", "error")
                }
                return@withContext false
            }
        } else {
            val existeLocalmente = db.getPesoPorId(idPesoShared)
            if (existeLocalmente != null) {
                val result2 = db.updatePesoById(idPesoShared, pesosEntity)
                if (result2 <= 0) {
                    withContext(Dispatchers.Main) {
                        showCustomToast(context, "Error al usar el peso", "error")
                    }
                    return@withContext false
                }
            } else {
                val result = db.addPesoUsed(pesoUsedEntity)
                if (result == -1L) {
                    withContext(Dispatchers.Main) {
                        showCustomToast(context, "Error al actualizar el peso local", "error")
                    }
                    return@withContext false
                }
            }
        }
        withContext(Dispatchers.Main) {
            showCustomToast(
                context,
                "Estas usando el peso de ${pesosEntity.nombreCompleto} ",
                "info"
            )
        }

        // Después de la actualización local exitosa, intentamos actualizar el servidor remoto
        val baseUrl = Constants.getBaseUrl()
        val urlString =
            "${baseUrl}controllers/TempPesoPollosController.php?op=insertar&idPesoShared=${idPesoShared}"

        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.doOutput = true

                val existeLocalmente = db.getPesoPorId(idPesoShared)
                // Convertir PesosEntity a JSON
                val loadPesoEntity = PesosEntity(
                    id = 0,
                    idNucleo = pesosEntity.idNucleo,
                    idGalpon = pesosEntity.idGalpon,
                    numeroDocCliente = pesosEntity.numeroDocCliente,
                    nombreCompleto = pesosEntity.nombreCompleto,
                    dataPesoJson = pesosEntity.dataPesoJson,
                    dataDetaPesoJson = pesosEntity.dataDetaPesoJson,
                    idEstado = "0",
                    isSync = "1",
                    devicedName = pesosEntity.devicedName,
                    serieDevice = existeLocalmente!!.serieDevice,
                    fechaRegistro = ""
                )

                // Convertir PesosEntity a JSON
                val jsonInputString = loadPesoEntity.toJson().toString()

                // Escribir JSON en el cuerpo de la solicitud
                conn.outputStream.use { os ->
                    val input = jsonInputString.toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = conn.inputStream.bufferedReader().use { it.readText() }
                    Log.e("UpdateListPesos", "Response: $inputStream")

                    try {
                        val jsonResponse = JSONObject(inputStream)
                        val status = jsonResponse.optString("status")
                        val message = jsonResponse.optString("message")

                        if (status == "success") {
                            withContext(Dispatchers.Main) {
                                showCustomToast(
                                    context,
                                    "Peso actualizado correctamente ",
                                    "success"
                                )
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                showCustomToast(
                                    context,
                                    "Ocurrió un error al actualizar en el servidor",
                                    "error"
                                )
                            }
                            Log.e("UpdateListPesos", "Error: $message")
                            return@withContext false
                        }
                    } catch (e: JSONException) {
                        withContext(Dispatchers.Main) {
                            showCustomToast(
                                context,
                                "Error al procesar la respuesta del servidor",
                                "error"
                            )
                        }
                        Log.e("updateListPesos", "JSON Exception: ${e.message}")
                    }
                } else {
                    Log.e(
                        "updateListPesos",
                        "Server response: $responseCode ${conn.responseMessage}"
                    )
                }
            } catch (ex: Exception) {
                Log.e("updateListPesos", "Exception: ${ex.message}")
            }
        }

        // Si llegamos aquí, significa que la actualización remota falló, pero la local fue exitosa
        return@withContext true
    }

    /*
        - Para eliminar un peso y finalizarlo, se cambiara su estado a 2 para que el servidor lo identifique
        como peso eliminado o finalizado

        - Para eliminar con internet, se eliminará directamente de la base de datos y localmente si es exitoso
         si no el estado estara cambiado para que posteriormente lo elimine el servidor.
     */
    fun removeListPesosId(
        context: Context,
        idPeso: Int,
        callback: (Boolean) -> Unit
    ) {
        val db = AppDatabase(context)
        val serieDevice = db.getPesoPorId(idPeso)
        var serie = serieDevice!!.serieDevice

        val result = db.setStatusDeletedPeso(serie)

        if (result > 0){
            Log.d("removeListPesosId", "Peso eliminado correctamente")
        }

        val baseUrl = Constants.getBaseUrl()
        val urlString =
            "${baseUrl}controllers/TempPesoPollosController.php?op=removeBySerieDevice&serieDevice=$serie"
        if (NetworkUtils.isNetworkAvailable(context)) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val url = URL(urlString)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"

                    val responseCode = conn.responseCode

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val inputStream = conn.inputStream.bufferedReader().use { it.readText() }
                        val jsonResponse = JSONObject(inputStream)
                        val status = jsonResponse.optString("status")

                        CoroutineScope(Dispatchers.Main).launch {
                            if (status == "success") {
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        if (idPeso != 0) {
                                            val result = db.deletePesosBySerieDevice(serie)
                                            // Llamar al callback en el hilo principal
                                            withContext(Dispatchers.Main) {
                                                callback(result > 0)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        // Manejar cualquier excepción y llamar al callback con false
                                        withContext(Dispatchers.Main) {
                                            callback(false)
                                        }
                                    }
                                }
                                callback(true)
                            } else {
                                showCustomToast(context, "Error al eliminar el peso", "error")
                                callback(false)
                            }
                        }
                    } else {
                        CoroutineScope(Dispatchers.Main).launch {
                            showCustomToast(
                                context,
                                "Error de red: ${conn.responseMessage}",
                                "error"
                            )
                            callback(false)
                        }
                    }
                } catch (ex: Exception) {
                    CoroutineScope(Dispatchers.Main).launch {
                        showCustomToast(context, "Error de red: ${ex.message}", "error")
                        callback(false)
                    }
                }
            }
        }

        callback(result > 0)
    }

    fun setStatusUsed(
        context: Context,
        idPeso: Int,
        status: String,
        deviceName: String,
        callback: (Boolean) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            // Primero, actualizamos en la base de datos local
            val db = AppDatabase(context)
            if (idPeso != 0) {
                val pesoEntity = PesosEntity(
                    id = idPeso,
                    isSync = "",
                    idEstado = if (status == "Used") "1" else "0",
                    devicedName = if (status == "Used") deviceName else "",
                    fechaRegistro = "",
                    idGalpon = 0,
                    idNucleo = 0,
                    numeroDocCliente = "",
                    nombreCompleto = "",
                    dataPesoJson = "",
                    serieDevice = "",
                    dataDetaPesoJson = ""
                )
                val result = db.setStatusUsed(pesoEntity)

                if (result <= 0) {
                    // Si la actualización local falla, notificamos el error
                    CoroutineScope(Dispatchers.Main).launch {
                        callback(false)
                    }
                    return@launch
                }
            }


////          Después de la actualización local exitosa, intentamos actualizar el servidor remoto
//            val baseUrl = Constants.getBaseUrl()
//            val urlString =
//                "${baseUrl}controllers/TempPesoPollosController.php?op=$status&idPeso=$idPeso&diviceName=$deviceName"
//
//            try {
//                val url = URL(urlString)
//                val conn = url.openConnection() as HttpURLConnection
//                conn.requestMethod = "GET"
//
//                val responseCode = conn.responseCode
//
//                if (responseCode == HttpURLConnection.HTTP_OK) {
//                    val inputStream = conn.inputStream.bufferedReader().use { it.readText() }
//                    val jsonResponse = JSONObject(inputStream)
//                    val responseStatus = jsonResponse.optString("status")
//
//                    CoroutineScope(Dispatchers.Main).launch {
//                        callback(responseStatus == "success")
//                    }
//                } else {
//                    CoroutineScope(Dispatchers.Main).launch {
//                        callback(false)
//                    }
//                }
//            } catch (ex: Exception) {
//                CoroutineScope(Dispatchers.Main).launch {
//                    callback(false)
//                }
//            }
        }
    }


    fun getStautusPeso(
        context: Context,
        idPeso: Int,
        callback: (String?) -> Unit
    ) {
        val baseUrl = Constants.getBaseUrl()
        val urlString =
            "${baseUrl}controllers/TempPesoPollosController.php?op=getStatusPeso&idPeso=$idPeso"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"

                val responseCode = conn.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = JSONObject(inputStream)
                    val status = jsonResponse.optString("status")

                    if (status == "success") {
                        val dataArray = jsonResponse.optJSONArray("data")
                        if (dataArray != null && dataArray.length() > 0) {
                            val dataObject = dataArray.getJSONObject(0)
                            val statusFromData = dataObject.optString("status", "0")
                            val addresMac = dataObject.optString("addresMac", "")

                            Log.d(
                                "getStautusPeso",
                                "Status: $statusFromData, AddresMac: $addresMac"
                            )

                            val result = "$statusFromData|$addresMac"
                            CoroutineScope(Dispatchers.Main).launch {
                                callback(result)
                            }
                        } else {
                            Log.d("getStautusPeso", "Data array está vacío o nulo")
                            CoroutineScope(Dispatchers.Main).launch {
                                callback(null)
                            }
                        }
                    } else {
                        CoroutineScope(Dispatchers.Main).launch {
                            callback(null)
                        }
                    }
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        callback(null)
                    }
                }
            } catch (ex: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    callback(null)
                }
            }

            val db = AppDatabase(context)
            val result = db.getPesosStatus(idPeso)

            if (result.isNotEmpty()) {
                val pesosEntity = result[0]
                val statusFromData = "0"
                val addresMac = "null"

                val resultString = "$statusFromData|$addresMac"
                CoroutineScope(Dispatchers.Main).launch {
                    callback(resultString)
                }
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    callback(null)
                }
            }
        }
    }


    fun obtenerPesosServer(
        context: Context,
        callback: (Boolean) -> Unit
    ) {
        val db = AppDatabase(context)
        val baseUrl = Constants.getBaseUrl()
        val apiService = ApiService()
        var idDevice = db.getSerieIdDeviceLocal()
        if (idDevice.isEmpty()) {
            idDevice = getDeviceId(context)
        }
        val deviceModel = getAddressMacDivice.getDeviceManufacturer()

        val urlString = "${baseUrl}controllers/PesoPollosController.php?op=getAllDataSynchronized"
        // Crea un objeto JSON con el campo mac
        val jsonBody = JSONObject().apply {
            put("mac", idDevice)
            put("deviceModel", deviceModel)
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                apiService.makePostRequest(urlString, jsonBody) { response, error ->
                    if (error != null) {
                        Log.e("DataSyncManager", "Error: ${error.message}")
                        callback(false)
                        return@makePostRequest
                    }

                    try {
                        val jsonResponse = JSONObject(response)
                        val dataString = jsonResponse.getString("data")
                        val data = JSONObject(dataString)

                        // Procesar los datos recibidos
                        val tempPesos = data.getJSONArray("tempPesos")

                        processTempPesos(context, tempPesos)
                        callback(true)
                    } catch (e: Exception) {
                        Log.e("DataSyncManager", "Error al procesar JSON: ${e.message}")
                        callback(false)
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                withContext(Dispatchers.Main) {
                    Log.e("DataSyncManager", "Error: ${ex.message}")
                    callback(false)
                }
            }

        }

    }

    private fun processTempPesos(context: Context, tempPesos: JSONArray): Boolean {
        var needsSync = false
        val db = AppDatabase(context)
        try {
            db.beginTransaction()
            try {
                for (i in 0 until tempPesos.length()) {
                    val pesotemp = tempPesos.getJSONObject(i)

                    val serieDevice = pesotemp.getString("serieDevice")

                    val serieDeviceEntity = PesosEntity(
                        id = 0,
                        idNucleo = pesotemp.getInt("temp_idEstablecimiento"),
                        idGalpon = pesotemp.getInt("temp_idGalpones"),
                        numeroDocCliente = pesotemp.getString("temp_numeroDocCliente"),
                        nombreCompleto = pesotemp.getString("temp_nombreCompleto"),
                        dataPesoJson = pesotemp.getString("temp_dataJsonPeso"),
                        dataDetaPesoJson = pesotemp.getString("temp_dataJsonDetaPeso"),
                        idEstado = pesotemp.getString("status"),
                        isSync = "1",
                        serieDevice = pesotemp.getString("serieDevice"),
                        devicedName = pesotemp.getString("addresMac"),
                        fechaRegistro = pesotemp.getString("temp_fechaRegistro")
                    )

                    val existePeso = db.getPesoBySerieDevice(serieDevice)

                    if (existePeso == null) {
                        db.insertListPesos(serieDeviceEntity)
                    } else {
                        db.updatePesoBySerieDevice(serieDevice, serieDeviceEntity)
                    }

                    val pesosEliminar = db.getAllPesosEliminar()

                    if (pesosEliminar.isNotEmpty()){
                        db.deletePesosBySerieDevice(serieDevice)
                    }
                }

                val pesosLocales = db.getAllPesosNotSync()
                if (pesosLocales.isNotEmpty()) {
                    needsSync = true
                }
                db.setTransactionSuccessful()
                return needsSync
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e("DataProcessor", "Error procesando series: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
}