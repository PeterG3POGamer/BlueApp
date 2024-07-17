package com.example.blueapp.ui.Jabas

import NetworkUtils
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.blueapp.R
import com.example.blueapp.ui.DataBase.AppDatabase
import com.example.blueapp.ui.DataBase.Entities.DataDetaPesoPollosEntity
import com.example.blueapp.ui.DataBase.Entities.DataPesoPollosEntity
import com.example.blueapp.ui.DataBase.Entities.GalponEntity
import com.example.blueapp.ui.DataBase.Entities.NucleoEntity
import com.example.blueapp.ui.DataBase.Entities.PesosEntity
import com.example.blueapp.ui.Services.PreLoading
import com.example.blueapp.ui.Services.createNotificationChannel
import com.example.blueapp.ui.Services.generateAndOpenPDF2
import com.example.blueapp.ui.Services.showNotification
import com.example.blueapp.ui.Services.showProgressNotification
import com.example.blueapp.ui.Services.showSuccessNotification
import com.example.blueapp.ui.Utilidades.Constants
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
    private fun saveLocally(
        context: Context,
        jabasFragment: JabasFragment,
        dataDetaPesoPollos: List<DataDetaPesoPollosEntity>,
        dataPesoPollos: DataPesoPollosEntity
    ) {
        jabasFragment.view?.let { view ->
            val numeroDocumento = view.findViewById<TextView>(R.id.textDocCli)?.text?.toString().orEmpty()
            val nombreCliente = view.findViewById<TextView>(R.id.textNomCli)?.text?.toString().orEmpty()
            val spinnerGalpon = view.findViewById<Spinner>(R.id.select_galpon)
            val galponIndex = spinnerGalpon.selectedItemPosition

            if (galponIndex == 0) { // Asumiendo que la primera posición es "Seleccione galpón"
                showNotification(context, "Seleccionar Galpón", "Es necesario seleccionar un galpón.")
                return
            }

            if (numeroDocumento.isEmpty() || nombreCliente.isEmpty()) {
                showNotification(context, "Registrar Cliente", "Es necesario registrar el número y el nombre del cliente.")
                return
            }

            val db = AppDatabase(context)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val (totalJabas, totalPollos, totalPeso) = calcularTotales(dataDetaPesoPollos)

                    // Actualizar dataPesoPollos con los totales calculados
                    dataPesoPollos.totalJabas = totalJabas.toString()
                    dataPesoPollos.totalPollos = totalPollos.toString()
                    dataPesoPollos.totalPeso = totalPeso.toString()

                    dataDetaPesoPollos.forEach { db.insertDataDetaPesoPollos(it) }
                    db.insertDataPesoPollos(dataPesoPollos.copy(numeroDocCliente = numeroDocumento, nombreCompleto = nombreCliente))

                    withContext(Dispatchers.Main) {
                        showSuccessNotification(context)
                        jabasFragment.limpiarCampos()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        showNotification(context, "Error al guardar localmente", e.message ?: "Error desconocido")
                    }
                }
            }
        } ?: run {
            showNotification(context, "Error", "No se pudo acceder a la vista del fragmento.")
        }
    }

    fun captureData(jabasList: List<JabasItem>): List<DataDetaPesoPollosEntity> {
        return jabasList.map {
            DataDetaPesoPollosEntity(
                idDetaPP = it.id,
                cantJabas = it.numeroJabas,
                cantPollos = it.numeroPollos,
                peso = it.pesoKg,
                tipo = it.conPollos
            )
        }
    }

    fun captureDataPesoPollos(
        id: Int,
        serie: String,
        fecha: String,
        totalJabas: String,
        totalPollos: String,
        totalPeso: String,
        tipo: String,
        numeroDocCliente: String,
        nombreCompleto: String?,
        idGalpon: String,
        idNucleo: String,
        PKPollo: String,
        totalPesoJabas: String,
        totalNeto: String,
        totalPagar: String
    ): DataPesoPollosEntity {
        return DataPesoPollosEntity(
            id = id,
            serie = serie,
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
            TotalPagar = totalPagar
        )
    }

    @SuppressLint("InflateParams", "SetTextI18n")
    var lastToast: Toast? = null

    fun showCustomToast(context: Context, message: String, type: String) {
        val layoutInflater = LayoutInflater.from(context)
        val layout = layoutInflater.inflate(R.layout.toast_custom, null)

        // Configurar el icono, el mensaje y el texto adicional
        val toastIcon = layout.findViewById<ImageView>(R.id.toast_icon)
        val toastMessage = layout.findViewById<TextView>(R.id.toast_message)
        val toastType = layout.findViewById<TextView>(R.id.toast_type)
        val toastHeader = layout.findViewById<LinearLayout>(R.id.toast_header)
        val toastFooter = layout.findViewById<LinearLayout>(R.id.toast_footer)

        when (type) {
            "success" -> {
                toastIcon.setImageResource(R.drawable.outline_data_check_24)
                toastType.text = "ÉXITO"
                toastHeader.background = ContextCompat.getDrawable(context, R.drawable.toast_h_background_success)
                toastFooter.background = ContextCompat.getDrawable(context, R.drawable.toast_f_background)
            }
            "info" -> {
                toastIcon.setImageResource(R.drawable.sharp_data_info_alert_24)
                toastType.text = "INFO"
                toastHeader.background = ContextCompat.getDrawable(context, R.drawable.toast_h_background_info)
                toastFooter.background = ContextCompat.getDrawable(context, R.drawable.toast_f_background)
            }
            "error" -> {
                toastIcon.setImageResource(R.drawable.outline_data_alert_24)
                toastType.text = "ERROR"
                toastHeader.background = ContextCompat.getDrawable(context, R.drawable.toast_h_background_error)
                toastFooter.background = ContextCompat.getDrawable(context, R.drawable.toast_f_background)
            }
            else -> {
                toastIcon.visibility = View.GONE
            }
        }

        toastMessage.text = message

        // Cancelar el último Toast si está activo
        lastToast?.cancel()

        // Crear y mostrar el nuevo Toast
        lastToast = Toast(context).apply {
            duration = Toast.LENGTH_SHORT
            setGravity(Gravity.CENTER, 0, 0)
            view = layout
            show()
        }
    }

    // Funcion para verificar si hay coneccion a internet
    fun sendDataToServer(
        context: Context,
        fragment: JabasFragment,
        dataDetaPesoPollos: List<DataDetaPesoPollosEntity>,
        dataPesoPollos: DataPesoPollosEntity
    ) {
        if (NetworkUtils.isNetworkAvailable(context)) {
            // Hay conexión de red, enviar datos al servidor
            sendToServer(context, fragment, dataDetaPesoPollos, dataPesoPollos)
        } else {
            // No hay conexión de red, almacenar datos localmente
            // saveLocally(context, fragment, dataDetaPesoPollos, dataPesoPollos)
        showCustomToast(context, "No se detecto conexión a Internet, por favor conectece a internet para enviar los datos", "info")
        }
    }

    // Función para enviar datos directamente al servidor si hay internet
    fun sendToServer(
        context: Context,
        fragment: JabasFragment,
        dataDetaPesoPollos: List<DataDetaPesoPollosEntity>,
        dataPesoPollos: DataPesoPollosEntity
    ) {
        val preLoading = PreLoading(context)
        val urlString = "${Constants.BASE_URL}app/controllers/PesoPollosController.php?op=InsertarDataPesoPollos"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
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

                // Actualizar dataPesoPollos con los totales calculados
//                dataPesoPollos.totalJabas = totalJabas.toString()
//                dataPesoPollos.totalPollos = totalPollos.toString()
//                dataPesoPollos.totalPeso = totalPeso.toString()

                // Verificar si dataDetaPesoPollos es null o vacío antes de usarlo
                if (dataDetaPesoPollos.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        showCustomToast(context, "Error: dataDetaPesoPollos es null o vacío", "error")
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

//                        Log.d("ManagerPost", "DATAPDF: $jsonDataPdf")

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
                                    showRetryDialogNetwork(context, fragment, dataDetaPesoPollos, dataPesoPollos)
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
                                            sendDataToServer(context, fragment, dataDetaPesoPollos, dataPesoPollos)
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
                                    showRetryDialogNetwork(context, fragment, dataDetaPesoPollos, dataPesoPollos)

                                }
                            }
                        }
                    } catch (e: JSONException) {
                        // Captura la excepción si no se puede convertir a JSON
                        withContext(Dispatchers.Main) {
                            showCustomToast(context, "Respuesta inválida del servidor", "error")
                            Log.e("ManagerPost", "Error al convertir la respuesta a JSON: $inputStream", e)
                            showRetryDialogNetwork(context, fragment, dataDetaPesoPollos, dataPesoPollos)
                        }
                    }
                } else {
                    // Manejar errores HTTP
                    withContext(Dispatchers.Main) {
                        showCustomToast(context, "Error al enviar datos: $responseCode - $responseMessage", "error")
                        Log.e("ManagerPost", "Error al enviar mensaje: $responseCode - $responseMessage")
                        showRetryDialogNetwork(context, fragment, dataDetaPesoPollos, dataPesoPollos)
                    }
                }
                // Ocultar la carga después de obtener la respuesta del servidor
                withContext(Dispatchers.Main) {
                    preLoading.hidePreCarga()
                }

                // Cerrar la conexión al terminar
                conn.disconnect()

            } catch (e: Exception) {
                // Manejar cualquier otra excepción
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showCustomToast(context, "Error: ${e.message}", "error")
                    Log.e("ManagerPost", "Error: ${e.message}")
                    showRetryDialogNetwork(context, fragment, dataDetaPesoPollos, dataPesoPollos)
                }
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
        val urlString = "${Constants.BASE_URL}enviar.php"

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
                                    showRetryDialog(context, fragment, dataDetaPesoPollos, dataPesoPollos)
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
                            Log.e("ManagerPost", "Error al convertir la respuesta a JSON: $inputStream", e)
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
    fun BuscarCliente(parametros: String, callback: (String?) -> Unit) {
        val urlString = "${Constants.BASE_URL}app/controllers/FuncionesController/BuscarCliente.php"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(urlString)
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
                                    // Aquí manejas la respuesta exitosa si es necesario
                                    Log.d("ManagerPost", "Nombre completo del cliente: $nombreCompleto")
                                    callback(nombreCompleto)
                                }
                                "error" -> {
                                    // Aquí manejas el error si es necesario
                                    Log.d("ManagerPost", "Error de la Web: $message")
                                    callback(null)
                                }
                                "info" -> {
                                    // Aquí manejas la información si es necesario
                                    Log.d("ManagerPost", "Información de la Web: $message")
                                    callback(null)
                                }
                                else -> {
                                    // Manejar otro estado si es necesario
                                    Log.e("ManagerPost", "Error: $message")
                                    callback(null)
                                }
                            }
                        }
                    } catch (e: JSONException) {
                        // Captura la excepción si no se puede convertir a JSON
                        withContext(Dispatchers.Main) {
                            Log.e("ManagerPost", "Error al convertir la respuesta a JSON: $inputStream", e)
                            callback(null)
                        }
                    }
                } else {
                    // Manejar errores HTTP
                    withContext(Dispatchers.Main) {
                        Log.e("ManagerPost", "Error al enviar datos: $responseCode")
                        callback(null)
                    }
                }

            } catch (ex: Exception) {
                ex.printStackTrace()
                withContext(Dispatchers.Main) {
                    // Manejar excepciones y actualizar la UI
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
                .setPositiveButton("Aceptar", null) // Inicialmente configurado a null para control manual
                .setNegativeButton("Cancelar") { dialog, which ->
                    continuation.resume(null)
                    dialog.dismiss()
                }
                .setCancelable(false) // Deshabilitar la cancelación al tocar fuera del diálogo
                .create()

            dialog.setOnCancelListener {
                continuation.resume(null)
            }

            dialog.setOnShowListener {
                val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                button.isEnabled = false // Deshabilitar inicialmente

                inputEditText.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
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

    fun getSelectGalpon(idNucleo: String, callback: (List<GalponEntity>?) -> Unit) {
        val urlString = "${Constants.BASE_URL}app/controllers/PesoPollosController.php?op=getSelectGalpon&idNucleo=$idNucleo"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

                val responseCode = conn.responseCode
                val responseMessage = conn.responseMessage

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = conn.inputStream.bufferedReader().use { it.readText() }

                    try {
                        val jsonResponse = JSONObject(inputStream)
                        val status = jsonResponse.optString("status")
                        val message = jsonResponse.optString("message")

                        if (status == "success") {
                            val dataGalpones = jsonResponse.optJSONArray("dataGalpones")
                            val galponesList = mutableListOf<GalponEntity>()

                            for (i in 0 until dataGalpones.length()) {
                                val galponObj = dataGalpones.optJSONObject(i)
                                val idGalpon = galponObj.optInt("idgalpones")
                                val nombre = galponObj.optString("nomgal")
                                val galpon = GalponEntity(idGalpon, nombre)
                                galponesList.add(galpon)
                            }

                            withContext(Dispatchers.Main) {
                                callback(galponesList)
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                callback(null)
                            }
                        }
                    } catch (e: JSONException) {
                        withContext(Dispatchers.Main) {
                            Log.e("ManagerPost", "Error al convertir la respuesta a JSON: $inputStream", e)
                            callback(null)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Log.e("ManagerPost", "Error al obtener datos: $responseCode")
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

    fun getNucleos(callback: (List<NucleoEntity>?) -> Unit) {
        val urlString = "${Constants.BASE_URL}app/controllers/PesoPollosController.php?op=getNucleo"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

                val responseCode = conn.responseCode
                val responseMessage = conn.responseMessage

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = conn.inputStream.bufferedReader().use { it.readText() }

                    try {
                        val jsonResponse = JSONObject(inputStream)
                        val status = jsonResponse.optString("status")
                        val message = jsonResponse.optString("message")

                        if (status == "success") {
                            val dataNucleos = jsonResponse.optJSONArray("dataNucleos")
                            val nucleosList = mutableListOf<NucleoEntity>()

                            for (i in 0 until dataNucleos.length()) {
                                val nucleoObj = dataNucleos.optJSONObject(i)
                                val idEstablecimiento = nucleoObj.optString("idEstablecimiento")
                                val nombre = nucleoObj.optString("nombre")
                                val nucleo = NucleoEntity(idEstablecimiento, nombre)
                                nucleosList.add(nucleo)
                            }

                            withContext(Dispatchers.Main) {
                                callback(nucleosList)
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                callback(null)
                            }
                        }
                    } catch (e: JSONException) {
                        withContext(Dispatchers.Main) {
                            Log.e("ManagerPost", "Error al convertir la respuesta a JSON: $inputStream", e)
                            callback(null)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Log.e("ManagerPost", "Error al obtener datos: $responseCode")
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

    // =============================================
    // SECCION: LISTA DE PESOS GET, ADD, DELETE
    // =============================================

    fun getListPesosByIdGalpon(idGalpon: Int, idEstablecimiento: Int, diviceName: String, callback: (List<PesosEntity>?) -> Unit) {
        val urlString = "${Constants.BASE_URL}app/controllers/TempPesoPollosController.php?op=getListPesosByIdGalpon&idGalpon=$idGalpon&idEstablecimiento=${idEstablecimiento}&diviceName=$diviceName"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

                val responseCode = conn.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = conn.inputStream.bufferedReader().use { it.readText() }

                    try {
                        val jsonResponse = JSONObject(inputStream)
                        val status = jsonResponse.optString("status")

                        if (status == "success") {
                            val dataArray = jsonResponse.getJSONArray("data")
                            val pesosList = mutableListOf<PesosEntity>()

                            for (i in 0 until dataArray.length()) {
                                val item = dataArray.getJSONObject(i)
                                val pesosEntity = PesosEntity(
                                    id = item.getInt("idPeso"),
                                    idNucleo = item.getInt("idNucleo"),
                                    idGalpon = item.getInt("idGalpon"),
                                    numeroDocCliente = item.getString("numeroDocCliente"),
                                    nombreCompleto = item.optString("nombreCompleto"),
                                    dataPesoJson = item.getString("dataPesoJson"),
                                    dataDetaPesoJson = item.getString("dataDetaPesoJson"),
                                    fechaRegistro = item.getString("fechaRegistro")
                                )
                                pesosList.add(pesosEntity)
                            }

                            withContext(Dispatchers.Main) {
                                callback(pesosList)
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                callback(null)
                            }
                        }
                    } catch (e: JSONException) {
                        withContext(Dispatchers.Main) {
                            Log.e("GetListPesosByIdNucleo", "Error al convertir la respuesta a JSON: $inputStream", e)
                            callback(null)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Log.e("GetListPesosByIdNucleo", "Error al obtener datos: $responseCode")
                        callback(null)
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                withContext(Dispatchers.Main) {
                    Log.e("GetListPesosByIdNucleo", "Error: ${ex.message}")
                    callback(null)
                }
            }
        }
    }


    fun getListPesosId(idPeso: Int, callback: (List<PesosEntity>?, List<DataDetaPesoPollosEntity>?) -> Unit) {
        val urlString = "${Constants.BASE_URL}app/controllers/TempPesoPollosController.php?op=getListPesosByIdPeso&idPeso=$idPeso"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

                val responseCode = conn.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = conn.inputStream.bufferedReader().use { it.readText() }

                    try {
                        val jsonResponse = JSONObject(inputStream)
                        val status = jsonResponse.optString("status")

                        if (status == "success") {
                            val dataArray = jsonResponse.getJSONArray("data")
                            val pesosList = mutableListOf<PesosEntity>()
                            var dataDetaPesoList: List<DataDetaPesoPollosEntity>? = null

                            for (i in 0 until dataArray.length()) {
                                val item = dataArray.getJSONObject(i)

                                val dataDetaPesoJson = item.getString("dataDetaPesoJson")
                                dataDetaPesoList = parseDataDetaPesoJson(dataDetaPesoJson)
                            }

                            withContext(Dispatchers.Main) {
                                callback(pesosList, dataDetaPesoList)
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                callback(null, null)
                            }
                        }
                    } catch (e: JSONException) {
                        withContext(Dispatchers.Main) {
                            Log.e("GetListPesosByIdNucleo", "Error al convertir la respuesta a JSON: $inputStream", e)
                            callback(null, null)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Log.e("GetListPesosByIdNucleo", "Error al obtener datos: $responseCode")
                        callback(null, null)
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                withContext(Dispatchers.Main) {
                    Log.e("GetListPesosByIdNucleo", "Error: ${ex.message}")
                    callback(null, null)
                }
            }
        }
    }

    private fun parseDataDetaPesoJson(json: String): List<DataDetaPesoPollosEntity> {
        val jabasList = JSONArray(json)
        return (0 until jabasList.length()).map { i ->
            val jaba = jabasList.getJSONObject(i)
            DataDetaPesoPollosEntity(
                idDetaPP = jaba.getInt("id"),
                cantJabas = jaba.getInt("numeroJabas"),
                cantPollos = jaba.getInt("numeroPollos"),
                peso = jaba.getDouble("pesoKg"),
                tipo = jaba.getString("conPollos")
            )
        }
    }

    suspend fun addListPesos(
        context: Context,
        fragment: JabasFragment,
        pesosEntity: PesosEntity
    ): Boolean = withContext(Dispatchers.IO) {
        val urlString = "${Constants.BASE_URL}app/controllers/TempPesoPollosController.php?op=insertar"

        try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.doOutput = true

            // Convertir PesosEntity a JSON
            val jsonInputString = pesosEntity.toJson().toString()

            // Escribir JSON en el cuerpo de la solicitud
            conn.outputStream.use { os ->
                val input = jsonInputString.toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            val responseCode = conn.responseCode
            val responseMessage = conn.responseMessage

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = conn.inputStream.bufferedReader().use { it.readText() }
                Log.e("AddListPesos", "Error: $inputStream")

                try {
                    val jsonResponse = JSONObject(inputStream)
                    val status = jsonResponse.optString("status")
                    val message = jsonResponse.optString("message")

                    if (status == "success") {
                        withContext(Dispatchers.Main) {
                            showCustomToast(context, "Datos enviados correctamente", "success")
                        }
                        true
                    } else {
                        withContext(Dispatchers.Main) {
                            showCustomToast(context, "Ocurrio un error al insertar", "error")
                        }
                        Log.e("AddListPesos", "Error: $message")
                        false
                    }
                } catch (e: JSONException) {
                    withContext(Dispatchers.Main) {
                        showCustomToast(context, "Error al procesar la respuesta del servidor", "error")
                    }
                    Log.e("addListPesos", "JSON Exception: ${e.message}")
                    false
                }
            } else {
                withContext(Dispatchers.Main) {
                    showCustomToast(context, "Error de red: ${conn.responseMessage}", "error")
                }
                Log.e("addListPesos", "Server response: $responseCode ${conn.responseMessage}")
                false
            }
        } catch (ex: Exception) {
            withContext(Dispatchers.Main) {
                showCustomToast(context, "Error de red: ${ex.message}", "error")
            }
            Log.e("addListPesos", "Exception: ${ex.message}")
            false
        }
    }

    suspend fun updateListPesos(
        context: Context,
        fragment: JabasFragment,
        pesosEntity: PesosEntity,
        idPesoShared: Int
    ): Boolean = withContext(Dispatchers.IO) {
        val urlString = "${Constants.BASE_URL}app/controllers/TempPesoPollosController.php?op=insertar&idPesoShared=${idPesoShared}"

        try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.doOutput = true

            // Convertir PesosEntity a JSON
            val jsonInputString = pesosEntity.toJson().toString()

            // Escribir JSON en el cuerpo de la solicitud
            conn.outputStream.use { os ->
                val input = jsonInputString.toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            val responseCode = conn.responseCode
            val responseMessage = conn.responseMessage

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = conn.inputStream.bufferedReader().use { it.readText() }
                Log.e("AddListPesos", "Error: $inputStream")

                try {
                    val jsonResponse = JSONObject(inputStream)
                    val status = jsonResponse.optString("status")
                    val message = jsonResponse.optString("message")

                    if (status == "success") {
                        withContext(Dispatchers.Main) {
                            showCustomToast(context, "Peso actualizado correctamente", "success")
                        }
                        true
                    } else {
                        withContext(Dispatchers.Main) {
                            showCustomToast(context, "Ocurrio un error al insertar", "error")
                        }
                        Log.e("AddListPesos", "Error: $message")
                        false
                    }
                } catch (e: JSONException) {
                    withContext(Dispatchers.Main) {
                        showCustomToast(context, "Error al procesar la respuesta del servidor", "error")
                    }
                    Log.e("addListPesos", "JSON Exception: ${e.message}")
                    false
                }
            } else {
                withContext(Dispatchers.Main) {
                    showCustomToast(context, "Error de red: ${conn.responseMessage}", "error")
                }
                Log.e("addListPesos", "Server response: $responseCode ${conn.responseMessage}")
                false
            }
        } catch (ex: Exception) {
            withContext(Dispatchers.Main) {
                showCustomToast(context, "Error de red: ${ex.message}", "error")
            }
            Log.e("addListPesos", "Exception: ${ex.message}")
            false
        }
    }

    fun removeListPesosId(
        context: Context,
        idPeso: Int,
        callback: (Boolean) -> Unit
    ) {
        val urlString = "${Constants.BASE_URL}app/controllers/TempPesoPollosController.php?op=removeByIdPeso&idPeso=$idPeso"

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
        diviceName: String,
        callback: (Boolean) -> Unit
    ) {
        val urlString = "${Constants.BASE_URL}app/controllers/TempPesoPollosController.php?op=$status&idPeso=$idPeso&diviceName=$diviceName"

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
                            callback(true)
                        } else {
                            callback(false)
                        }
                    }
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        callback(false)
                    }
                }
            } catch (ex: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    callback(false)
                }
            }
        }
    }

    fun getStautusPeso(
        context: Context,
        idPeso: Int,
        callback: (String?) -> Unit
    ) {
        val urlString = "${Constants.BASE_URL}app/controllers/TempPesoPollosController.php?op=getStatusPeso&idPeso=$idPeso"

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

                            Log.d("getStautusPeso", "Status: $statusFromData, AddresMac: $addresMac")

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
        }
    }
}

           