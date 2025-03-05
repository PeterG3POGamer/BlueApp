package app.serlanventas.mobile.ui.DataSyncManager.SyncVentaAndDetalle

import android.content.Context
import app.serlanventas.mobile.ui.DataBase.AppDatabase
import app.serlanventas.mobile.ui.DataSyncManager.ApiService
import app.serlanventas.mobile.ui.DataSyncManager.DataComparator
import app.serlanventas.mobile.ui.DataSyncManager.DataProcessor
import app.serlanventas.mobile.ui.DataSyncManager.DialogManager
import app.serlanventas.mobile.ui.Utilidades.Constants

class SyncVentaAndDetalle(private val context: Context) {

    private val db = AppDatabase(context)
    private val apiService = ApiService()
    private val dataProcessor = DataProcessor(context, db)
    private val dataComparator = DataComparator(db)
    private val dialogManager = DialogManager(context)
    private val baseUrl = Constants.getBaseUrl()

//    suspend fun procesarVentas(callback: (Boolean) -> Unit) {
//        try {
//            // Obtener todas las ventas locales no sincronizadas
//            val ventasNotSync = db.getAllDataPesoPollosNotSync()
//            val detalleNotSync = db.getAllDataDetaPesoPollos()
//
//            // Enviar cada venta al servidor y actualizar su estado
//            ventasNotSync.forEach { venta ->
//                enviarVenta(venta, detalleNotSync) { success, error ->
//                    if (success) {
//                        // Si la sincronización es exitosa, actualizar el estado de la venta
//                        val result = db.updateStatusVentaSync(venta.id)
//                        if (result <= 0) {
//                            Log.e("DataSyncManager", "Error al actualizar el estado de la venta ${venta.id}")
//                        }
//                    } else {
//                        Log.e("DataSyncManager", "Error al sincronizar venta ${venta.id}: ${error?.message}")
//                    }
//                }
//            }
//
//            // Llamar al callback con éxito si todas las ventas se procesaron
//            callback(true)
//        } catch (e: Exception) {
//            // Llamar al callback con error si ocurre una excepción
//            callback(false)
//        }
//    }

//    suspend fun enviarVenta(
//        venta: DataPesoPollosEntity,
//        detalle: List<DataDetaPesoPollosEntity>,
//        callback: (Boolean, Throwable?) -> Unit
//    ) {
//        val jsonDetaPesoPollos = JSONArray()
//        detalle.forEach { detaPesoPollo ->
//            jsonDetaPesoPollos.put(detaPesoPollo.toJson())
//        }
//
//        val jsonPesoPollos = venta.toJson()
//
//        val jsonBody = JSONObject()
//        jsonBody.put("datosDetaPesoPollos", jsonDetaPesoPollos)
//        jsonBody.put("datosPesoPollos", JSONArray().put(jsonPesoPollos))
//
//        val baseUrl = "${Constants.getBaseUrl()}controllers/PesoPollosController.php?op=InsertarDataPesoPollos"
//
//        apiService.sendVentaLocal(baseUrl, jsonBody) { response, error ->
//            if (error != null) {
//                callback(false, error)
//                return@sendVentaLocal
//            }
//
//            // Procesar la respuesta del servidor
//            try {
//                val jsonResponse = JSONObject(response!!)
//                val status = jsonResponse.optString("status")
//                if (status == "success") {
//                    // Si la sincronización es exitosa, actualizar el estado de la venta
//                    callback(true, null)
//                } else {
//                    val message = jsonResponse.optString("message")
//                    callback(false, Exception(message))
//                }
//            } catch (e: JSONException) {
//                callback(false, e)
//            }
//        }
//    }


    fun procesarDetalleVentas() {
        // Implementar la lógica para procesar los detalles de las ventas
    }
}
