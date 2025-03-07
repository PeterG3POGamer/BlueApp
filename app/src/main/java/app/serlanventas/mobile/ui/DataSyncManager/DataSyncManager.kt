package app.serlanventas.mobile.ui.DataSyncManager

import NetworkUtils
import android.content.Context
import android.util.Log
import app.serlanventas.mobile.ui.DataBase.AppDatabase
import app.serlanventas.mobile.ui.DataBase.Entities.DataPesoPollosEntity
import app.serlanventas.mobile.ui.Interfaces.ProgressCallback
import app.serlanventas.mobile.ui.Services.getAddressMacDivice
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

    fun checkSincronizarData(
        baseUrl: String,
        isLoggedIn: Boolean,
        progressCallback: ProgressCallback,
        callback: (Boolean) -> Unit
    ) {
        if (NetworkUtils.isNetworkAvailable(context)) {
            CoroutineScope(Dispatchers.IO).launch {
                progressCallback.onProgressUpdate("Iniciando sincronización...")
                delay(1000)

                var idDevice = db.getSerieIdDeviceLocal()
                if (idDevice.isEmpty()) {
                    idDevice = getAddressMacDivice.getDeviceId(context)
                }
                val deviceModel = getAddressMacDivice.getDeviceManufacturer()

                // Aquí se envia el idDevice y el deviceModel para obtener los datos
                progressCallback.onProgressUpdate("Obteniendo datos del servidor...")
                obtenerDatosNube(baseUrl, idDevice, deviceModel) { syncResult ->
                    when (syncResult) {
                        is SyncResult.Success -> {
                            CoroutineScope(Dispatchers.IO).launch {
                                progressCallback.onProgressUpdate("Comparando datos locales y remotos...")
                                // Verificar si hay cambios pendientes para subir
                                val ventasLocales = db.getAllDataPesoPollosNotSync()

                                if (ventasLocales.isNotEmpty()) {
                                    progressCallback.onProgressUpdate("Subiendo ventas locales al servidor...")
                                    subirVentasLocales(
                                        baseUrl,
                                        idDevice,
                                        deviceModel,
                                        ventasLocales
                                    ) { uploadResult ->
                                        if (uploadResult) {
                                            progressCallback.onProgressUpdate("Ventas sincronizadas correctamente")
                                            handleSyncResult(syncResult, isLoggedIn, callback)
                                        } else {
                                            progressCallback.onProgressUpdate("Error al sincronizar ventas")
                                            callback(false)
                                        }
                                    }
                                } else {
                                    progressCallback.onProgressUpdate("No hay ventas pendientes por sincronizar")
                                    handleSyncResult(syncResult, isLoggedIn, callback)
                                }
                            }
                        }

                        is SyncResult.Error -> {
                            progressCallback.onProgressUpdate("Error: ${syncResult.message}")
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

                        // Procesar los datos y guardar en la base de datos local
                        val needsSync = dataProcessor.processServerData(
                            ventasNube,
                            detallesVentasNube,
                            usuariosNube,
                            clientesNube,
                            establecimientosNube,
                            galponesNube,
                            serieNube
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

    private fun handleSyncResult(
        result: SyncResult,
        isLoggedIn: Boolean,
        callback: (Boolean) -> Unit
    ) {
        // Switch to the main thread before showing dialogs or updating UI
        CoroutineScope(Dispatchers.Main).launch {
            delay(1000)
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
}