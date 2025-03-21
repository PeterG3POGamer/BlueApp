package app.serlanventas.mobile.ui.DataSyncManager

import NetworkUtils
import android.app.Activity
import android.content.Context
import android.graphics.PorterDuff
import android.util.Log
import android.widget.ImageView
import androidx.core.content.ContextCompat
import app.serlanventas.mobile.R
import app.serlanventas.mobile.ui.DataBase.AppDatabase
import app.serlanventas.mobile.ui.DataBase.Entities.CaptureDeviceEntity
import app.serlanventas.mobile.ui.DataBase.Entities.DataPesoPollosEntity
import app.serlanventas.mobile.ui.DataBase.Entities.PesosEntity
import app.serlanventas.mobile.ui.Interfaces.ProgressCallback
import app.serlanventas.mobile.ui.Services.getAddressMacDivice
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class DataSyncManager(private val context: Context) {
    private val db = AppDatabase(context)
    private val apiService = ApiService()
    private val dataProcessor = DataProcessor(context, db)
    private val dataComparator = DataComparator(db)
    private val dialogManager = DialogManager(context)
    private lateinit var progressGif: ImageView

    fun setProgressGif(progressGif: ImageView) {
        this.progressGif = progressGif
    }

    fun checkSincronizarData(
        baseUrl: String,
        isLoggedIn: Boolean,
        progressCallback: ProgressCallback,
        callback: (Boolean) -> Unit
    ) {
        if (NetworkUtils.isNetworkAvailable(context)) {
            CoroutineScope(Dispatchers.IO).launch {
                updateProgressGif(context, progressGif, R.drawable.icon_loading2)
                progressGif.setColorFilter(
                    ContextCompat.getColor(context, R.color.purple), // Color deseado
                    PorterDuff.Mode.SRC_IN // Modo de mezcla
                )
                animateProgressMessage(progressCallback, "Iniciando sincronización")

                var idDevice = db.getSerieIdDeviceLocal()
                if (idDevice.isEmpty()) {
                    idDevice = getAddressMacDivice.getDeviceId(context)
                }
                val deviceModel = getAddressMacDivice.getDeviceManufacturer()

                // Aquí se envia el idDevice y el deviceModel para obtener los datos
                progressGif.clearColorFilter();
                updateProgressGif(context, progressGif, R.drawable.icon_get)
                animateProgressMessage(progressCallback, "Obteniendo datos del servidor")

                obtenerDatosNube(baseUrl, idDevice, deviceModel) { syncResult ->
                    when (syncResult) {
                        is SyncResult.Success -> {
                            CoroutineScope(Dispatchers.IO).launch {
                                updateProgressGif(
                                    context,
                                    progressGif,
                                    R.drawable.icon_escaneo_file
                                )
                                animateProgressMessage(
                                    progressCallback,
                                    "Comparando datos locales y remotos"
                                )
                                // Verificar si hay cambios pendientes para subir
                                val pesosLocales = db.getAllPesosNotSync()
                                val pesosLocalesEliminar = db.getAllPesosEliminar()
                                val ventasLocales = db.getAllDataPesoPollosNotSync()
                                val configLocales = db.getConfigCaptureNotSync()
                                var needSync2: Boolean

                                if (pesosLocales.isEmpty() && pesosLocalesEliminar.isEmpty() && ventasLocales.isEmpty() && configLocales.isEmpty()) {
                                    progressCallback.onProgressUpdate("No hay datos para sincronizar")
                                    needSync2 = false
                                } else {
                                    needSync2 = true
                                }

                                if (needSync2) {
                                    delay(1000)
                                    updateProgressGif(context, progressGif, R.drawable.icon_post)
                                    delay(1000)
                                    animateProgressMessage(progressCallback, "Procesando Datos")
                                    delay(1000)
                                    // Verificar Pesos temporales
                                    if (pesosLocales.isNotEmpty()) {
                                        subirPesosLocales(
                                            baseUrl,
                                            pesosLocales
                                        ) { pesosUploadResult ->
                                            if (pesosUploadResult) {
                                                progressCallback.onProgressUpdate("Pesos temporales sincronizados correctamente")
                                            } else {
                                                progressCallback.onProgressUpdate("Error al sincronizar pesos temporales")
                                                callback(false)
                                            }
                                        }
                                    }
                                    delay(1000)
                                    if (pesosLocalesEliminar.isNotEmpty()) {
                                        elimiarPesosLocales(
                                            baseUrl,
                                            pesosLocalesEliminar
                                        ) { pesosUploadResult ->
                                            if (pesosUploadResult) {
                                                progressCallback.onProgressUpdate("Pesos temporales sincronizados correctamente")
                                            } else {
                                                progressCallback.onProgressUpdate("Error al sincronizar pesos temporales")
                                                callback(false)
                                            }
                                        }
                                    }
                                    delay(1000)
                                    if (configLocales.isNotEmpty()) {
                                        subirConfigLocales(
                                            baseUrl,
                                            configLocales
                                        ) { configUploadResult ->
                                            if (configUploadResult) {
                                                progressCallback.onProgressUpdate("Configuraciones bluetooth sincronizados correctamente")
                                            } else {
                                                progressCallback.onProgressUpdate("Error al sincronizar configuracioens bluetooth")
                                                callback(false)
                                            }
                                        }
                                    }
                                    delay(1000)
                                    if (ventasLocales.isNotEmpty()) {
                                        animateProgressMessage(
                                            progressCallback,
                                            "Subiendo ventas locales al servidor"
                                        )
                                        subirVentasLocales(
                                            baseUrl,
                                            idDevice,
                                            deviceModel,
                                            ventasLocales
                                        ) { uploadResult ->
                                            if (uploadResult) {
                                                progressCallback.onProgressUpdate("Ventas sincronizadas correctamente")
                                            } else {
                                                progressCallback.onProgressUpdate("Error al sincronizar ventas")
                                                callback(false)
                                            }
                                        }
                                    }
                                }

                                delay(1000)
                                updateProgressGif(context, progressGif, R.drawable.icon_employee)
                                animateProgressMessage(progressCallback, "Verificando sesión")
                                delay(1000)
                                updateProgressGif(
                                    context,
                                    progressGif,
                                    R.drawable.icon_scan_user
                                )
                                animateProgressMessage(progressCallback, "Autenticando")
                                if (isLoggedIn) {
                                    delay(1000)
                                    updateProgressGif(
                                        context,
                                        progressGif,
                                        R.drawable.icon_login_success
                                    )
                                    progressCallback.onProgressUpdate("¡Sesión exitosa!")
                                    delay(1000)
                                    animateProgressMessage(progressCallback, "Redirigiendo")
                                } else {
                                    delay(1000)
                                    progressCallback.onProgressUpdate("¡Sesión cerrada!")
                                    delay(1000)
                                    progressCallback.onProgressUpdate("Por favor, inicie sesión")
                                }
                                delay(1000)
                                handleSyncResult(syncResult, isLoggedIn, callback)
                            }
                        }

                        is SyncResult.Error -> {
                            Log.d("DataSyncManager", "Error: ${syncResult.message}")
                            handleSyncResult(syncResult, isLoggedIn, callback)
                        }
                    }
                }
            }
        } else {
            callback(false)
        }
    }

    fun obtenerDatosNube(
        baseUrl: String,
        macDevice: String,
        deviceModel: String,
        callback: (SyncResult) -> Unit
    ) {
        val urlString = "${baseUrl}controllers/PesoPollosController.php?op=getAllDataSynchronized"
        // Crea un objeto JSON con el campo mac
        val jsonBody = JSONObject().apply {
            put("mac", macDevice)
            put("deviceModel", deviceModel)
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                apiService.makePostRequest(urlString, jsonBody) { response, error ->
                    if (error != null) {
                        Log.e("DataSyncManager", "Error: ${error.message}")
                        callback(SyncResult.Error("Ocurrio un error al obtener los datos del servidor"))
                        return@makePostRequest
                    }

                    try {
                        val jsonResponse = JSONObject(response)
                        val dataString = jsonResponse.getString("data")
                        val data = JSONObject(dataString)

                        // Procesar los datos recibidos
                        val ventasNube = data.getJSONArray("ventas")
                        val detallesVentasNube = data.getJSONArray("detallesVentas")
                        val usuariosNube = data.getJSONArray("usuarios")
                        val clientesNube = data.getJSONArray("clientes")
                        val establecimientosNube = data.getJSONArray("establecimientos")
                        val galponesNube = data.getJSONArray("galpones")
                        val serieNube = data.getJSONArray("series")
                        val tempPesos = data.getJSONArray("tempPesos")
                        val confCapture = data.getJSONArray("confCapture")


                        // Procesar los datos y guardar en la base de datos local
                        val needsSync = dataProcessor.processServerData(
                            ventasNube,
                            detallesVentasNube,
                            usuariosNube,
                            clientesNube,
                            establecimientosNube,
                            galponesNube,
                            serieNube,
                            tempPesos,
                            confCapture
                        )

                        callback(SyncResult.Success(needsSync))

                    } catch (e: Exception) {
                        Log.e("DataSyncManager", "Error al procesar JSON: ${e.message}")
                        callback(SyncResult.Error("Error al procesar los datos de la nube"))
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                withContext(Dispatchers.Main) {
                    Log.e("DataSyncManager", "Error: ${ex.message}")
                    callback(SyncResult.Error("Error: ${ex.message}"))
                }
            }
        }
    }

    fun subirVentasLocales(
        baseUrl: String,
        macDevice: String,
        deviceModel: String,
        ventasLocales: List<DataPesoPollosEntity>,
        callback: (Boolean) -> Unit
    ) {
        val urlString = "${baseUrl}controllers/PesoPollosController.php?op=InsertarDataPesoPollos"

        // Crear JSON con todos los datos a enviar
        val jsonParam = JSONObject()
        val datosPesoPollos = JSONArray()
        val datosDetaPesoPollos = JSONArray()

        // Convertir cada venta (PesosEntity) a JSON y obtener sus detalles
        ventasLocales.forEach { venta ->
            // Añadir la venta al array datosPesoPollos
            val jsonPesoPollos = venta.toJson()
            datosPesoPollos.put(jsonPesoPollos)

            // Obtener detalles de la venta actual
            val detalles = db.obtenerDetaPesoPollosPorId(venta.id.toString())

            // Convertir cada detalle a JSON y añadirlo al array datosDetaPesoPollos
            detalles.forEach { detalle ->
                val jsonDetalle = detalle.toJson()
                datosDetaPesoPollos.put(jsonDetalle)
            }
        }

        // Añadir arrays al objeto JSON principal
        jsonParam.put("datosDetaPesoPollos", datosDetaPesoPollos)
        jsonParam.put("datosPesoPollos", datosPesoPollos)
        jsonParam.put("mac", macDevice)
        jsonParam.put("deviceModel", deviceModel)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                apiService.makePostRequest(urlString, jsonParam) { response, error ->
                    if (error != null) {
                        Log.e("DataSyncManager", "Error al subir ventas: ${error.message}")
                        callback(false)
                        return@makePostRequest
                    }

                    try {
                        Log.d("DataSyncManager", "Respuesta del servidor: $response")
                        val jsonResponse = JSONObject(response)
                        val success = jsonResponse.getString("status")

                        // INSERTAR EL CLIENTE ANTES DE ENVIAR LA VENTA

                        if (success == "success") {
                            // Marcar ventas como sincronizadas en la base de datos local
                            CoroutineScope(Dispatchers.IO).launch {
                                ventasLocales.forEach { venta ->
                                    db.updateStatusVentaSync(venta.id)
                                }

                                withContext(Dispatchers.Main) {
                                    callback(true)
                                }
                            }
                        } else {
                            val mensaje = jsonResponse.getString("mensaje")
                            Log.e("DataSyncManager", "Error de sincronización: $mensaje")
                            callback(false)
                        }
                    } catch (e: Exception) {
                        Log.e("DataSyncManager", "Error al procesar respuesta: ${e.message}")
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

    fun subirPesosLocales(
        baseUrl: String,
        pesosLocales: List<PesosEntity>,
        callback: (Boolean) -> Unit
    ) {
        val urlString = "${baseUrl}controllers/TempPesoPollosController.php?op=insertar"

        CoroutineScope(Dispatchers.IO).launch {
            var allSuccess = true

            pesosLocales.forEach { peso ->
                val jsonPeso = peso.toJson()
                val jsonParam = JSONObject()

                // Asegúrate de que los campos en jsonPeso coincidan con los esperados por el servidor
                jsonParam.put("idPeso", jsonPeso.optString("idPeso"))
                jsonParam.put("idNucleo", jsonPeso.optString("idNucleo"))
                jsonParam.put("idGalpon", jsonPeso.optString("idGalpon"))
                jsonParam.put("numeroDocCliente", jsonPeso.optString("numeroDocCliente"))
                jsonParam.put("nombreCompleto", jsonPeso.optString("nombreCompleto"))
                jsonParam.put("dataPesoJson", jsonPeso.optString("dataPesoJson"))
                jsonParam.put("dataDetaPesoJson", jsonPeso.optString("dataDetaPesoJson"))
                jsonParam.put("serieDevice", jsonPeso.optString("serieDevice"))
                jsonParam.put("fechaRegistro", jsonPeso.optString("fechaRegistro"))

                try {
                    apiService.makePostRequest(urlString, jsonParam) { response, error ->
                        if (error != null) {
                            Log.e("DataSyncManager", "Error al subir peso: ${error.message}")
                            allSuccess = false
                            return@makePostRequest
                        }

                        try {
                            Log.d("DataSyncManager", "Respuesta del servidor: $response")
                            val jsonResponse = JSONObject(response)
                            val success = jsonResponse.getString("status")

                            if (success == "success") {
                                // Marcar peso como sincronizado en la base de datos local
                                CoroutineScope(Dispatchers.IO).launch {
                                    db.updateStatusPesoSync(peso.serieDevice)
                                }
                            } else {
                                val mensaje = jsonResponse.getString("message")
                                Log.e("DataSyncManager", "Error de sincronización: $mensaje")
                                allSuccess = false
                            }
                        } catch (e: Exception) {
                            Log.e("DataSyncManager", "Error al procesar respuesta: ${e.message}")
                            allSuccess = false
                        }
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    Log.e("DataSyncManager", "Error: ${ex.message}")
                    allSuccess = false
                }
            }

            withContext(Dispatchers.Main) {
                callback(allSuccess)
            }
        }
    }

    fun elimiarPesosLocales(
        baseUrl: String,
        pesosLocales: List<PesosEntity>,
        callback: (Boolean) -> Unit
    ) {
        val urlString = "${baseUrl}controllers/TempPesoPollosController.php?op=removeBySerieDevice"

        CoroutineScope(Dispatchers.IO).launch {
            var allSuccess = true

            pesosLocales.forEach { peso ->
                val jsonPeso = peso.toJson()
                val jsonParam = JSONObject()
                jsonParam.put("serieDevice", jsonPeso.optString("serieDevice"))

                try {
                    apiService.makeGetRequest(urlString, jsonParam) { response, error ->
                        if (error != null) {
                            Log.e("DataSyncManager", "Error al subir peso: ${error.message}")
                            allSuccess = false
                            return@makeGetRequest
                        }

                        try {
                            Log.d("DataSyncManager", "Respuesta del servidor: $response")
                            val jsonResponse = JSONObject(response)
                            val success = jsonResponse.getString("status")

                            if (success == "success") {
                                // Update the local database to mark the weight as synchronized
                                CoroutineScope(Dispatchers.IO).launch {
                                    db.deletePesosBySerieDevice(peso.serieDevice)
                                }
                            } else {
                                val mensaje = jsonResponse.getString("message")
                                Log.e("DataSyncManager", "Error de sincronización: $mensaje")
                                allSuccess = false
                            }
                        } catch (e: Exception) {
                            Log.e("DataSyncManager", "Error al procesar respuesta: ${e.message}")
                            allSuccess = false
                        }
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    Log.e("DataSyncManager", "Error: ${ex.message}")
                    allSuccess = false
                }
            }

            // Call the callback on the main thread
            withContext(Dispatchers.Main) {
                callback(allSuccess)
            }
        }
    }


    fun subirConfigLocales(
        baseUrl: String,
        configLocales: List<CaptureDeviceEntity>,
        callback: (Boolean) -> Unit
    ) {
        val urlString = "${baseUrl}controllers/ConfigCaptureController.php?op=insertar"

        CoroutineScope(Dispatchers.IO).launch {
            var allSuccess = true

            configLocales.forEach { config ->
                val jsonConfig = config.toJson()
                val jsonParam = JSONObject()

                // Asegúrate de que los campos en jsonPeso coincidan con los esperados por el servidor
                jsonParam.put("_idCaptureDevice", jsonConfig.optString("_cc_idCaptureDevice"))
                jsonParam.put("_cadenaClave", jsonConfig.optString("_cc_cadenaClave"))
                jsonParam.put("_nombreDispositivo", jsonConfig.optString("_cc_nombreDispositivo"))
                jsonParam.put("_macDispositivo", jsonConfig.optString("_cc_macDispositivo"))
                jsonParam.put("_longitud", jsonConfig.optString("_cc_longitud"))
                jsonParam.put("_formatoPeo", jsonConfig.optString("_cc_formatoPeo"))
                jsonParam.put("_estado", jsonConfig.optString("_cc_estado"))
                jsonParam.put("_cadenaClaveCierre", jsonConfig.optString("_cc_cadenaClaveCierre"))
                jsonParam.put("_bloque", jsonConfig.optString("_cc_bloque"))

                try {
                    apiService.makePostRequest(urlString, jsonParam) { response, error ->
                        if (error != null) {
                            Log.e("DataSyncManager", "Error al subir peso: ${error.message}")
                            allSuccess = false
                            return@makePostRequest
                        }

                        try {
                            Log.d("DataSyncManager", "Respuesta del servidor: $response")
                            val jsonResponse = JSONObject(response)
                            val success = jsonResponse.getString("status")

                            if (success == "success") {
                                // Marcar peso como sincronizado en la base de datos local
                                CoroutineScope(Dispatchers.IO).launch {
                                    db.updateStatusCaptureSync(config._macDispositivo)
                                }
                            } else {
                                val mensaje = jsonResponse.getString("message")
                                Log.e("DataSyncManager", "Error de sincronización: $mensaje")
                                allSuccess = false
                            }
                        } catch (e: Exception) {
                            Log.e("DataSyncManager", "Error al procesar respuesta: ${e.message}")
                            allSuccess = false
                        }
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    Log.e("DataSyncManager", "Error: ${ex.message}")
                    allSuccess = false
                }
            }

            withContext(Dispatchers.Main) {
                callback(allSuccess)
            }
        }
    }

    private fun handleSyncResult(
        result: SyncResult,
        isLoggedIn: Boolean,
        callback: (Boolean) -> Unit
    ) {
        // Switch to the main thread before showing dialogs or updating UI
        CoroutineScope(Dispatchers.Main).launch {
            when (result) {
                is SyncResult.Success -> {
                    if (result.needsSync) {
//                        dialogManager.showSuccessDialog {
//                            callback(true)
//                        }
                        callback(true)
                    } else {
                        callback(true)
                    }
                }

                is SyncResult.Error -> {
                    dialogManager.showErrorDialog(null)
                    callback(false)
                }
            }
        }
    }

    private suspend fun animateProgressMessage(
        progressCallback: ProgressCallback,
        baseMessage: String,
        durationMillis: Long = 3000,
        dotIntervalMillis: Long = 500
    ) {
        val startTime = System.currentTimeMillis()
        var dots = 0

        while (System.currentTimeMillis() - startTime < durationMillis) {
            dots = (dots + 1) % 4 // Rotación de 0 a 3 puntos
            val message = "$baseMessage${".".repeat(dots)}"
            progressCallback.onProgressUpdate(message)
            delay(dotIntervalMillis)
        }
    }

    private suspend fun simulateProgressBar(
        progressCallback: ProgressCallback,
        baseMessage: String,
        steps: Int = 10,
        stepDurationMillis: Long = 500
    ) {
        for (step in 1..steps) {
            val progress = (step * 100 / steps).coerceAtMost(100) // Asegurar que no exceda 100%
            val message = "$baseMessage $progress%"
            progressCallback.onProgressUpdate(message)
            delay(stepDurationMillis)
        }
    }

    fun updateProgressGif(context: Context, progressGif: ImageView, gifResId: Int) {
        if (context is Activity && !context.isDestroyed) {
            context.runOnUiThread {
                Glide.with(context).asGif().load(gifResId).override(100, 100).into(progressGif)
            }
        }
    }
}