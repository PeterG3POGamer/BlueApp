package app.serlanventas.mobile.ui.DataSyncManager

import NetworkUtils
import android.content.Context
import android.util.Log
import app.serlanventas.mobile.ui.DataBase.AppDatabase
import app.serlanventas.mobile.ui.Interfaces.ProgressCallback
import app.serlanventas.mobile.ui.Jabas.ManagerPost.showCustomToast
import app.serlanventas.mobile.ui.Services.getAddressMacDivice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class DataSyncManager(private val context: Context) {
    private val db = AppDatabase(context)
    private val apiService = ApiService()
    private val dataProcessor = DataProcessor(context, db)
    private val dataComparator = DataComparator(db)
    private val dialogManager = DialogManager(context)

    fun checkSincronizardData(
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

                sincronizarData(baseUrl, idDevice, deviceModel) { result ->
                    when (result) {
                        is SyncResult.Success -> {
                            if (result.needsSync) {
                                progressCallback.onProgressUpdate("Es necesario sincronizar datos...")
                                dialogManager.showSyncConfirmationDialog { shouldSync ->
                                    if (shouldSync) {
                                        progressCallback.onProgressUpdate("Sincronizando datos...")
                                        sincronizarData(
                                            baseUrl,
                                            idDevice,
                                            deviceModel
                                        ) { syncResult ->
                                            handleSyncResult(syncResult, isLoggedIn, callback)
                                        }
                                    } else {
                                        showCustomToast(
                                            context,
                                            "No se sincronizarán los datos.",
                                            "info"
                                        )
                                        callback(false)
                                    }
                                }
                            } else {
                                callback(true)
                            }
                        }

                        is SyncResult.Error -> {
                            dialogManager.showErrorDialog { retry ->
                                if (retry) {
                                    sincronizarData(baseUrl, idDevice, deviceModel) { syncResult ->
                                        handleSyncResult(syncResult, isLoggedIn, callback)
                                    }
                                } else {
                                    callback(true)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            callback(false)
        }
    }

    fun sincronizarData(
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
                        callback(SyncResult.Error("Error: ${error.message}"))
                        return@makePostRequest
                    }

                    try {
                        val jsonResponse = JSONObject(response)
                        val dataString = jsonResponse.getString("data")
                        val data = JSONObject(dataString)

                        // Comparar datos locales con los de la nube
                        val necesitaSincronizar = dataComparator.compararDatos(data)

                        if (necesitaSincronizar) {
                            dataProcessor.procesarDatos(data, callback)
                            callback(SyncResult.Success(true))
                        } else {
                            callback(SyncResult.Success(true))
                        }

//                        val syncVentaAndDetalle = SyncVentaAndDetalle(context)
//                        CoroutineScope(Dispatchers.IO).launch {
//                            syncVentaAndDetalle.procesarVentas { ventasSuccess ->
//                                if (ventasSuccess) {
//                                    callback(SyncResult.Success(true))
//                                }else {
//                                    callback(SyncResult.Success(true))
//                                }
//                            }
//                        }
                    } catch (e: Exception) {
                        Log.e("DataSyncManager", "Error al procesar JSON: ${e.message}")
                        callback(SyncResult.Error("Error al procesar JSON: ${e.message}"))
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

    private fun handleSyncResult(
        result: SyncResult,
        isLoggedIn: Boolean,
        callback: (Boolean) -> Unit
    ) {
        when (result) {
            is SyncResult.Success -> {
                if (result.needsSync) {
                    dialogManager.showSuccessDialog {
                        callback(true)
                    }
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