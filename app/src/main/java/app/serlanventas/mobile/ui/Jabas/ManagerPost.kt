package app.serlanventas.mobile.ui.Jabas

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
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.serlanventas.mobile.R
import app.serlanventas.mobile.ui.DataBase.AppDatabase
import app.serlanventas.mobile.ui.DataBase.Entities.DataDetaPesoPollosEntity
import app.serlanventas.mobile.ui.DataBase.Entities.DataPesoPollosEntity
import app.serlanventas.mobile.ui.DataBase.Entities.GalponEntity
import app.serlanventas.mobile.ui.DataBase.Entities.NucleoEntity
import app.serlanventas.mobile.ui.DataBase.Entities.PesosEntity
import app.serlanventas.mobile.ui.DataBase.Entities.pesoUsedEntity
import app.serlanventas.mobile.ui.Services.PreLoading
import app.serlanventas.mobile.ui.Services.createNotificationChannel
import app.serlanventas.mobile.ui.Services.generateAndOpenPDF2
import app.serlanventas.mobile.ui.Services.getAddressMacDivice.getDeviceId
import app.serlanventas.mobile.ui.Services.showNotification
import app.serlanventas.mobile.ui.Services.showProgressNotification
import app.serlanventas.mobile.ui.Services.showSuccessNotification
import app.serlanventas.mobile.ui.Utilidades.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
                idPesoPollo = it.idPesoPollo
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

    @SuppressLint("ResourceType")
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
                toastIcon.setImageResource(R.drawable.ic_success) // Icono de éxito
                layout.background = ContextCompat.getDrawable(
                    context,
                    R.drawable.toast_background_success
                ) // Fondo personalizado para éxito
            }

            "info" -> {
                toastIcon.setImageResource(R.drawable.ic_info) // Icono de información
                layout.background = ContextCompat.getDrawable(
                    context,
                    R.drawable.toast_background_info
                ) // Fondo personalizado para info
            }

            "error" -> {
                toastIcon.setImageResource(R.drawable.ic_error) // Icono de error
                layout.background = ContextCompat.getDrawable(
                    context,
                    R.drawable.toast_background_error
                ) // Fondo personalizado para error
            }

            "warning" -> {
                toastIcon.setImageResource(R.drawable.ic_warning) // Icono de advertencia
                layout.background = ContextCompat.getDrawable(
                    context,
                    R.drawable.toast_background_warning
                ) // Fondo personalizado para advertencia
            }

            else -> {
                toastIcon.visibility = View.GONE // Ocultar icono si no es un tipo válido
                layout.background = ContextCompat.getDrawable(
                    context,
                    R.drawable.toast_background_default
                ) // Fondo predeterminado
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

        // Ocultar el PopupWindow después de un tiempo (similar a la duración del Toast)
        layout.postDelayed({
            popupWindow.dismiss()
        }, 2000) // El tiempo puede ajustarse a la duración que desees

        // Vibración al mostrar el Toast personalizado
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    300,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            ) // 300ms de vibración
        } else {
            vibrator.vibrate(300) // Para versiones anteriores a Android O
        }
    }

    // Funcion para verificar si hay coneccion a internet
    fun sendDataToServer(
        context: Context,
        fragment: JabasFragment,
        dataDetaPesoPollos: List<DataDetaPesoPollosEntity>,
        dataPesoPollos: DataPesoPollosEntity
    ) {
//        saveLocally(context, fragment, dataDetaPesoPollos, dataPesoPollos)
    }

    // Función para enviar datos directamente al servidor si hay internet
    fun sendToServer(
        context: Context,
        fragment: JabasFragment,
        dataDetaPesoPollos: List<DataDetaPesoPollosEntity>,
        dataPesoPollos: DataPesoPollosEntity
    ) {
        val preLoading = PreLoading(context)

        val baseUrl = Constants.getBaseUrl()
        val urlString = "${baseUrl}controllers/PesoPollosController.php?op=InsertarDataPesoPollos"
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection

        CoroutineScope(Dispatchers.IO).launch {
            try {
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.doOutput = true
                val db = AppDatabase(context)
                // Mostrar carga antes de enviar los datos

                withContext(Dispatchers.Main) {
                    preLoading.showPreCarga()
                }

                // Calcular totales y manejar excepciones
                val (totalJabas, totalPollos, totalPeso) = try {
                    calcularTotales(dataDetaPesoPollos)
                } catch (e: IllegalArgumentException) {
                    withContext(Dispatchers.Main) {
                        showCustomToast(context, "Error: ${e.message}", "error")
                        Log.e("ManagerPost", "Error al calcular totales: ${e.message}")
                    }
                    return@launch
                }

                // Verificar si dataPesoPollos es null antes de usarlo
                if (dataPesoPollos == null) {
                    withContext(Dispatchers.Main) {
                        showCustomToast(context, "Error: dataPesoPollos es null", "error")
                        Log.e("ManagerPost", "dataPesoPollos es null")
                    }
                    return@launch
                }

                // Verificar si dataDetaPesoPollos es null o vacío antes de usarlo
                if (dataDetaPesoPollos.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        showCustomToast(
                            context,
                            "Error: dataDetaPesoPollos es null o vacío",
                            "error"
                        )
                        Log.e("ManagerPost", "dataDetaPesoPollos es null o vacío")
                    }
                    return@launch
                }

                // Crear JSON para datosDetaPesoPollos
                val jsonDetaPesoPollos = JSONArray()
                dataDetaPesoPollos.forEach { detaPesoPollo ->
                    jsonDetaPesoPollos.put(detaPesoPollo.toJson())
                }

                // Crear JSON para datosPesoPollos
                val jsonPesoPollos = dataPesoPollos.toJson()

                // Crear JSON principal
                val jsonParam = JSONObject()
                jsonParam.put("datosDetaPesoPollos", jsonDetaPesoPollos)
                jsonParam.put("datosPesoPollos", JSONArray().put(jsonPesoPollos))

                // Escribir JSON en el cuerpo de la solicitud
                val wr = OutputStreamWriter(conn.outputStream)
                wr.write(jsonParam.toString())
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
                        val serie = jsonResponse.optString("serie")

                        // Obtener el JSON completo de PESO_POLLO como un objeto JSON
                        val jsonDataPdf = jsonResponse.optJSONObject("DATAPDF")

                        withContext(Dispatchers.Main) {
                            when (status) {
                                "success" -> {
                                    // Actualizar base de datos local
                                    dataDetaPesoPollos.forEach { db.insertDataDetaPesoPollos(it) }
                                    db.insertDataPesoPollos(dataPesoPollos)
                                    // Mostrar mensaje de éxito al usuario
                                    showCustomToast(context, message, "success")
                                    delay(4000)

                                    generateAndOpenPDF2(jsonDataPdf, context)
                                }

                                "error" -> {
                                    // Mostrar mensaje de error al usuario
                                    showCustomToast(context, message, "error")

                                    // Registrar mensaje de error en el Log
                                    Log.d("ManagerPost", "Error de la Web: $message")
                                    showRetryDialogNetwork(
                                        context,
                                        fragment,
                                        dataDetaPesoPollos,
                                        dataPesoPollos
                                    )
                                }

                                "info" -> {
                                    // Mostrar mensaje de información al usuario
                                    showCustomToast(context, message, "info")

                                    // Registrar mensaje de información en el Log
                                    Log.d("ManagerPost", "Información de la Web: $message")

                                    // Ejecutar acción adicional según información recibida
                                    CoroutineScope(Dispatchers.Main).launch {
                                        val nombreCompleto = showInputDialog(context)
                                        if (!nombreCompleto.isNullOrBlank()) {
                                            dataPesoPollos.nombreCompleto = nombreCompleto
                                            sendDataToServer(
                                                context,
                                                fragment,
                                                dataDetaPesoPollos,
                                                dataPesoPollos
                                            )
                                        } else {
                                            showCustomToast(context, "Proceso cancelado", "info")
                                        }
                                    }
                                }

                                else -> {
                                    // Mostrar mensaje de error genérico al usuario
                                    showCustomToast(context, "Error: $message", "error")

                                    // Registrar mensaje de error en el Log
                                    Log.e("ManagerPost", "Error: $message")
                                    showRetryDialogNetwork(
                                        context,
                                        fragment,
                                        dataDetaPesoPollos,
                                        dataPesoPollos
                                    )

                                }
                            }
                        }
                    } catch (e: JSONException) {
                        // Captura la excepción si no se puede convertir a JSON
                        withContext(Dispatchers.Main) {
                            showCustomToast(context, "Respuesta inválida del servidor", "error")
                            Log.e(
                                "ManagerPost",
                                "Error al convertir la respuesta a JSON: $inputStream",
                                e
                            )
                            showRetryDialogNetwork(
                                context,
                                fragment,
                                dataDetaPesoPollos,
                                dataPesoPollos
                            )
                        }
                    }
                } else {
                    // Manejar errores HTTP
                    withContext(Dispatchers.Main) {
                        showCustomToast(
                            context,
                            "Error al enviar datos: $responseCode - $responseMessage",
                            "error"
                        )
                        Log.e(
                            "ManagerPost",
                            "Error al enviar mensaje: $responseCode - $responseMessage"
                        )
                        showRetryDialogNetwork(
                            context,
                            fragment,
                            dataDetaPesoPollos,
                            dataPesoPollos
                        )
                    }
                }
                // Ocultar la carga después de obtener la respuesta del servidor
                withContext(Dispatchers.Main) {
                    delay(1000)
                    preLoading.hidePreCarga()
                }

            } catch (e: Exception) {
                // Manejar cualquier otra excepción
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showCustomToast(context, "Error: ${e.message}", "error")
                    Log.e("ManagerPost", "Error: ${e.message}")
                    showRetryDialogNetwork(context, fragment, dataDetaPesoPollos, dataPesoPollos)
                }
            } finally {
                conn.disconnect()
            }
        }
    }


    fun calcularTotales(dataDetaPesoPollos: List<DataDetaPesoPollosEntity>): Triple<Int, Int, Double> {
        var totalJabas = 0
        var totalPollos = 0
        var totalPeso = 0.0

        dataDetaPesoPollos.forEach { detaPesoPollo ->
            val cantJabas = detaPesoPollo.cantJabas ?: 0
            val cantPolllos = detaPesoPollo.cantPollos ?: 0
            val peso = detaPesoPollo.peso ?: 0.0

            try {
                totalJabas += cantJabas
                totalPollos += cantPolllos
                totalPeso += peso
            } catch (e: NumberFormatException) {
                throw IllegalArgumentException("Datos incompletos o incorrectos: ${e.message}")
            }
        }

        return Triple(totalJabas, totalPollos, totalPeso)
    }

    // Funcion para enviar datos guardados localmente al servidor si ya hay internet
    fun sendLocalDataToServer(
        context: Context,
        fragment: JabasFragment,
        dataDetaPesoPollos: List<DataDetaPesoPollosEntity>,
        dataPesoPollos: List<DataPesoPollosEntity>,
    ) {
        val baseUrl = Constants.getBaseUrl()

        val urlString = "${baseUrl}enviar.php"

        // Crear el canal de notificación
        createNotificationChannel(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Crear JSON para datosDetaPesoPollos
                val jsonDetaPesoPollos = JSONArray()
                dataDetaPesoPollos.forEach { detaPesoPollo ->
                    jsonDetaPesoPollos.put(detaPesoPollo.toJson())
                }

                // Crear JSON para datosPesoPollos
                val jsonPesoPollos = JSONArray()
                for (item in dataPesoPollos) {
                    jsonPesoPollos.put(item.toJson())
                }

                // Crear JSON principal
                val jsonParam = JSONObject()
                jsonParam.put("datosDetaPesoPollos", jsonDetaPesoPollos)
                jsonParam.put("datosPesoPollos", jsonPesoPollos)

                val jsonData = jsonParam.toString()
                val totalDataSize = jsonData.toByteArray().size.toDouble()

                // Configurar la conexión HTTP
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.doOutput = true

                // Escribir JSON en el cuerpo de la solicitud
                val wr = OutputStreamWriter(conn.outputStream)
                wr.write(jsonData)
                wr.flush()

                // Leer la respuesta del servidor
                val responseCode = conn.responseCode
                val responseMessage = conn.responseMessage

                withContext(Dispatchers.Main) {
                    // Cancelar la notificación de progreso
                    val notificationManager = NotificationManagerCompat.from(context)
                    notificationManager.cancel(1)

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        // Leer la respuesta del servidor
                        val inputStream = conn.inputStream.bufferedReader().use { it.readText() }

                        try {
                            // Intenta convertir la respuesta a JSON
                            val jsonResponse = JSONObject(inputStream)
                            val status = jsonResponse.optString("status")
                            val message = jsonResponse.optString("message")

                            when (status) {
                                "success" -> {
//                                    fragment.limpiarCampos()
                                    showCustomToast(context, message, "success")
                                    Log.d("ManagerPost", "Respuesta de la Web: $message")
                                }

                                "error" -> {
                                    showCustomToast(context, message, "error")
                                    Log.d("ManagerPost", "Error de la Web: $message")
                                    // Mostrar opción para reintentar en caso de error
                                    showRetryDialog(
                                        context,
                                        fragment,
                                        dataDetaPesoPollos,
                                        dataPesoPollos
                                    )
                                }

                                "info" -> {
                                    showCustomToast(context, message, "info")
                                    Log.d("ManagerPost", "Información de la Web: $message")
                                    // Aquí puedes implementar lógica adicional según la respuesta "info"
                                }

                                else -> {
                                    showCustomToast(context, "Error: $message", "error")
                                    Log.e("ManagerPost", "Error: $message")
                                }
                            }
                        } catch (e: JSONException) {
                            // Captura la excepción si no se puede convertir a JSON
                            showCustomToast(context, "Respuesta inválida del servidor", "error")
                            Log.e(
                                "ManagerPost",
                                "Error al convertir la respuesta a JSON: $inputStream",
                                e
                            )
                        }
                    } else {
                        // Manejar errores HTTP
                        showCustomToast(context, "Error al enviar datos: $responseCode", "error")
                        Log.e("ManagerPost", "Error al enviar mensaje: $responseCode")
                        // Mostrar opción para reintentar en caso de error
                        showRetryDialog(context, fragment, dataDetaPesoPollos, dataPesoPollos)
                    }
                }

            } catch (ex: Exception) {
                ex.printStackTrace()
                withContext(Dispatchers.Main) {
                    // Manejar excepciones y actualizar la UI
                    showCustomToast(context, "Error: ${ex.message}", "error")
                    Log.e("ManagerPost", "Error: ${ex.message}")
                    // Mostrar opción para reintentar en caso de error
                    showRetryDialog(context, fragment, dataDetaPesoPollos, dataPesoPollos)
                }
            }
        }

        // Mostrar notificación de progreso mientras se envían los datos
        showProgressNotification(context, 0, "Calculando tiempo...")
    }

    private fun showRetryDialog(
        context: Context,
        fragment: JabasFragment,
        dataDetaPesoPollos: List<DataDetaPesoPollosEntity>,
        dataPesoPollos: List<DataPesoPollosEntity>,
    ) {
        AlertDialog.Builder(context)
            .setTitle("Error al Enviar Datos")
            .setMessage("Hubo un error al enviar los datos. ¿Deseas intentarlo de nuevo?")
            .setPositiveButton("Reintentar") { dialog, _ ->
                // El usuario aceptó reintentar el envío
                sendLocalDataToServer(context, fragment, dataDetaPesoPollos, dataPesoPollos)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                // El usuario canceló el reenvío
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun showRetryDialogNetwork(
        context: Context,
        fragment: JabasFragment,
        dataDetaPesoPollos: List<DataDetaPesoPollosEntity>,
        dataPesoPollos: DataPesoPollosEntity,
    ) {
        AlertDialog.Builder(context)
            .setTitle("Error al Enviar Datos")
            .setMessage("Hubo un error al enviar los datos. ¿Deseas intentarlo de nuevo?")
            .setPositiveButton("Reintentar") { dialog, _ ->
                // El usuario aceptó reintentar el envío
                sendDataToServer(context, fragment, dataDetaPesoPollos, dataPesoPollos)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                // El usuario canceló el reenvío
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
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

        // Condición para saber si tenemos internet o no:
//        if (NetworkUtils.isNetworkAvailable(context)) {
//            // Si hay conexión a internet, obtener datos de la nube
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
//                                val dataNucleos = jsonResponse.optJSONArray("dataNucleos")
//                                val nucleosList = mutableListOf<NucleoEntity>()
//
//                                for (i in 0 until dataNucleos.length()) {
//                                    val nucleoObj = dataNucleos.optJSONObject(i)
//                                    val idEstablecimiento = nucleoObj.optString("idEstablecimiento")
//                                    val nombre = nucleoObj.optString("nombre")
//                                    val nucleo = NucleoEntity(idEstablecimiento, nombre, "")
//                                    nucleosList.add(nucleo)
//                                }
//
//                                withContext(Dispatchers.Main) {
//                                    callback(nucleosList)
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
//                    val nucleosList = getNucleosFromLocalDatabase(db)
//                    withContext(Dispatchers.Main) {
//                        callback(nucleosList)
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

//        if (NetworkUtils.isNetworkAvailable(context)) {
//            CoroutineScope(Dispatchers.IO).launch {
//                try {
//                    val url = URL(urlString)
//                    val conn = url.openConnection() as HttpURLConnection
//                    conn.requestMethod = "GET"
//                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
//
//                    val responseCode = conn.responseCode
//
//                    if (responseCode == HttpURLConnection.HTTP_OK) {
//                        val inputStream = conn.inputStream.bufferedReader().use { it.readText() }
//
//                        try {
//                            val jsonResponse = JSONObject(inputStream)
//                            val status = jsonResponse.optString("status")
//
//                            if (status == "success") {
//                                val dataArray = jsonResponse.getJSONArray("data")
//                                val pesosList = mutableListOf<PesosEntity>()
//
//                                for (i in 0 until dataArray.length()) {
//                                    val item = dataArray.getJSONObject(i)
//                                    val pesosEntity = PesosEntity(
//                                        id = item.getInt("idPeso"),
//                                        idNucleo = item.getInt("idNucleo"),
//                                        idGalpon = item.getInt("idGalpon"),
//                                        numeroDocCliente = item.getString("numeroDocCliente"),
//                                        nombreCompleto = item.optString("nombreCompleto"),
//                                        dataPesoJson = item.getString("dataPesoJson"),
//                                        dataDetaPesoJson = item.getString("dataDetaPesoJson"),
//                                        idEstado = "0",
//                                        devicedName = "",
//                                        fechaRegistro = item.getString("fechaRegistro")
//                                    )
//                                    pesosList.add(pesosEntity)
//                                }
//
//                                withContext(Dispatchers.Main) {
//                                    callback(pesosList)
//                                }
//                            } else {
//                                withContext(Dispatchers.Main) {
//                                    callback(null)
//                                }
//                            }
//                        } catch (e: JSONException) {
//                            withContext(Dispatchers.Main) {
//                                Log.e(
//                                    "GetListPesosByIdNucleo",
//                                    "Error al convertir la respuesta a JSON: $inputStream",
//                                    e
//                                )
//                                callback(null)
//                            }
//                        }
//                    } else {
//                        withContext(Dispatchers.Main) {
//                            Log.e("GetListPesosByIdNucleo", "Error al obtener datos: $responseCode")
//                            callback(null)
//                        }
//                    }
//                } catch (ex: Exception) {
//                    ex.printStackTrace()
//                    withContext(Dispatchers.Main) {
//                        Log.e("GetListPesosByIdNucleo", "Error: ${ex.message}")
//                        callback(null)
//                    }
//                }
//            }
//        } else {
//            // Si no hay conexión a internet, obtener datos locales
//            val pesosList = db.getPesosByIdGalponAndEstablecimiento(idGalpon, idEstablecimiento)
//            callback(pesosList)
//        }
    }


    suspend fun addListPesos(
        context: Context,
        fragment: JabasFragment,
        pesosEntity: PesosEntity
    ): Boolean = withContext(Dispatchers.IO) {
        // Insertar en la base de datos local
        val pesoUsedEntity = PesosEntity(
            id = 0,
            idNucleo = pesosEntity.idNucleo,
            idGalpon = pesosEntity.idGalpon,
            numeroDocCliente = pesosEntity.numeroDocCliente,
            nombreCompleto = pesosEntity.nombreCompleto,
            dataPesoJson = pesosEntity.dataPesoJson,
            dataDetaPesoJson = pesosEntity.dataDetaPesoJson,
            idEstado = "0",
            devicedName = "",
            fechaRegistro = ""
        )
        val db = AppDatabase(context)
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

        // Intentar enviar al servidor remoto
//        val baseUrl = Constants.getBaseUrl()
//        val urlString = "${baseUrl}controllers/TempPesoPollosController.php?op=insertar"
//
//        try {
//            val url = URL(urlString)
//            val conn = url.openConnection() as HttpURLConnection
//            conn.requestMethod = "POST"
//            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
//            conn.doOutput = true
//
//            // Convertir PesosEntity a JSON
//            val jsonInputString = pesosEntity.toJson().toString()
//
//            // Escribir JSON en el cuerpo de la solicitud
//            conn.outputStream.use { os ->
//                val input = jsonInputString.toByteArray(Charsets.UTF_8)
//                os.write(input, 0, input.size)
//            }
//
//            val responseCode = conn.responseCode
//            if (responseCode == HttpURLConnection.HTTP_OK) {
//                val inputStream = conn.inputStream.bufferedReader().use { it.readText() }
//                Log.d("AddListPesos", "Response: $inputStream")
//
//                val jsonResponse = JSONObject(inputStream)
//                val status = jsonResponse.optString("status")
//
//                if (status == "success") {
//                    withContext(Dispatchers.Main) {
//                        showCustomToast(context, "Datos enviados correctamente", "success")
//                    }
//                    return@withContext true
//                } else {
//                    val message = jsonResponse.optString("message")
//                    Log.e("AddListPesos", "Error: $message")
//                }
//            } else {
//                Log.e("addListPesos", "Server response: $responseCode ${conn.responseMessage}")
//            }
//        } catch (ex: Exception) {
//            Log.e("addListPesos", "Network exception: ${ex.message}")
//        }

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
            showCustomToast(context, "Estas usando el peso de ${pesosEntity.nombreCompleto} ", "info")
        }

        // Después de la actualización local exitosa, intentamos actualizar el servidor remoto
//        val baseUrl = Constants.getBaseUrl()
//        val urlString =
//            "${baseUrl}controllers/TempPesoPollosController.php?op=insertar&idPesoShared=${idPesoShared}"
//
//        try {
//            val url = URL(urlString)
//            val conn = url.openConnection() as HttpURLConnection
//            conn.requestMethod = "POST"
//            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
//            conn.doOutput = true
//
//            // Convertir PesosEntity a JSON
//            val jsonInputString = pesosEntity.toJson().toString()
//
//            // Escribir JSON en el cuerpo de la solicitud
//            conn.outputStream.use { os ->
//                val input = jsonInputString.toByteArray(Charsets.UTF_8)
//                os.write(input, 0, input.size)
//            }
//
//            val responseCode = conn.responseCode
//            if (responseCode == HttpURLConnection.HTTP_OK) {
//                val inputStream = conn.inputStream.bufferedReader().use { it.readText() }
//                Log.e("UpdateListPesos", "Response: $inputStream")
//
//                try {
//                    val jsonResponse = JSONObject(inputStream)
//                    val status = jsonResponse.optString("status")
//                    val message = jsonResponse.optString("message")
//
//                    if (status == "success") {
//                        withContext(Dispatchers.Main) {
//                            showCustomToast(context, "Peso actualizado correctamente ", "success")
//                        }
//                        return@withContext true
//                    } else {
//                        withContext(Dispatchers.Main) {
//                            showCustomToast(
//                                context,
//                                "Ocurrió un error al actualizar en el servidor",
//                                "error"
//                            )
//                        }
//                        Log.e("UpdateListPesos", "Error: $message")
//                    }
//                } catch (e: JSONException) {
//                    withContext(Dispatchers.Main) {
//                        showCustomToast(
//                            context,
//                            "Error al procesar la respuesta del servidor",
//                            "error"
//                        )
//                    }
//                    Log.e("updateListPesos", "JSON Exception: ${e.message}")
//                }
//            } else {
//                Log.e("updateListPesos", "Server response: $responseCode ${conn.responseMessage}")
//            }
//        } catch (ex: Exception) {
//            Log.e("updateListPesos", "Exception: ${ex.message}")
//        }

        // Si llegamos aquí, significa que la actualización remota falló, pero la local fue exitosa
        return@withContext true
    }


    fun removeListPesosId(
        context: Context,
        idPeso: Int,
        callback: (Boolean) -> Unit
    ) {
        val db = AppDatabase(context)

        // Ejecutar la operación de eliminación en un hilo de trabajo
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (idPeso != 0) {
                    val result = db.deletePesosById(idPeso)
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

        val baseUrl = Constants.getBaseUrl()
        val urlString =
            "${baseUrl}controllers/TempPesoPollosController.php?op=removeByIdPeso&idPeso=$idPeso"

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
                            showCustomToast(context, "Peso eliminado correctamente", "success")
                            callback(true)
                        } else {
                            showCustomToast(context, "Error al eliminar el peso", "error")
                            callback(false)
                        }
                    }
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        showCustomToast(context, "Error de red: ${conn.responseMessage}", "error")
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
                    idEstado = if (status == "Used") "1" else "0",
                    devicedName = deviceName,
                    fechaRegistro = "",
                    idGalpon = 0,
                    idNucleo = 0,
                    numeroDocCliente = "",
                    nombreCompleto = "",
                    dataPesoJson = "",
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
}