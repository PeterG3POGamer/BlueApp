package app.serlanventas.mobile.ui.DataSyncManager

import android.content.Context
import android.util.Log
import app.serlanventas.mobile.ui.DataBase.AppDatabase
import app.serlanventas.mobile.ui.DataBase.Entities.CaptureDeviceEntity
import app.serlanventas.mobile.ui.DataBase.Entities.ClienteEntity
import app.serlanventas.mobile.ui.DataBase.Entities.DataDetaPesoPollosEntity
import app.serlanventas.mobile.ui.DataBase.Entities.DataPesoPollosEntity
import app.serlanventas.mobile.ui.DataBase.Entities.GalponEntity
import app.serlanventas.mobile.ui.DataBase.Entities.NucleoEntity
import app.serlanventas.mobile.ui.DataBase.Entities.PesosEntity
import app.serlanventas.mobile.ui.DataBase.Entities.SerieDeviceEntity
import app.serlanventas.mobile.ui.DataBase.Entities.UsuarioEntity
import org.json.JSONArray
import org.json.JSONObject

class DataProcessor(private val context: Context, private val db: AppDatabase) {
    var needsSync = false

    // Procesa los datos recibidos del servidor y los guarda en la base de datos local
    fun processServerData(
        ventasNube: JSONArray,
        detallesVentasNube: JSONArray,
        usuariosNube: JSONArray,
        clientesNube: JSONArray,
        establecimientosNube: JSONArray,
        galponesNube: JSONArray,
        serieNube: JSONArray,
        tempPesos: JSONArray,
        confCapture: JSONArray
    ): Boolean {
        try {
            needsSync = false
            // Procesar usuarios
            processUsuarios(usuariosNube)

            // Procesar establecimientos
            processEstablecimientos(establecimientosNube)

            // Procesar galpones
            processGalpones(galponesNube)

            // Procesar clientes
            processClientes(clientesNube)

            // Procesar series
            processSeries(serieNube)

            // Procesar TempPesos
            processTempPesos(tempPesos)

            // Procesar CoonfCapture
            processConfCapture(confCapture)

            // Procesar ventas y sus detalles
            needsSync = processVentas(ventasNube, detallesVentasNube)

            return needsSync
        } catch (e: Exception) {
            Log.e("DataProcessor", "Error procesando datos: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    private fun processUsuarios(usuariosNube: JSONArray): Boolean {
        try {
            db.beginTransaction()
            try {
                // Implementar lógica para guardar usuarios en la base de datos local
                for (i in 0 until usuariosNube.length()) {
                    val usuario = usuariosNube.getJSONObject(i)
                    var idUsuario = usuario.getString("idUsuario")

                    // Extraer datos del usuario según tu estructura JSON
                    val usuariosEntity = UsuarioEntity(
                        idUsuario = idUsuario,
                        userName = usuario.getString("nombres") + " " + usuario.getString("apellidos"),
                        pass = usuario.getString("pass"),
                        idRol = usuario.getString("idRol"),
                        rolName = usuario.getString("rolname"),
                        idEstablecimiento = usuario.getString("idEstablecimiento")
                    )

                    val existeUsuario = db.getUsuarioById(idUsuario)
                    if (existeUsuario == null) {
                        db.insertUsuario(usuariosEntity)
                    } else {
//                    db.updateUsuario(usuariosEntity)
                    }
                }
                db.setTransactionSuccessful()

                return true
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e("DataProcessor", "Error procesando usuarios: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    private fun processEstablecimientos(establecimientosNube: JSONArray): Boolean {
        try {
            db.beginTransaction()
            try {
                for (i in 0 until establecimientosNube.length()) {
                    val establecimiento = establecimientosNube.getJSONObject(i)
                    val nombre = establecimiento.getString("nucleoName")

                    val nucleoEntity = NucleoEntity(
                        idEstablecimiento = establecimiento.getString("idEstablecimiento"),
                        nombre = establecimiento.getString("nucleoName"),
                        idEmpresa = establecimiento.getString("idEmpresa"),

                        )

                    val nucleoExistente = db.getNucleoByName(nombre)
                    if (nucleoExistente == null) {
                        db.insertNucleo(nucleoEntity)
                    } else {
                        db.updateNucleo(nucleoEntity)
                    }
                }
                db.setTransactionSuccessful()

                return true
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e("DataProcessor", "Error procesando establecimientos: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    private fun processGalpones(galponesNube: JSONArray): Boolean {
        try {
            db.beginTransaction()
            try {
                for (i in 0 until galponesNube.length()) {
                    val galpon = galponesNube.getJSONObject(i)
                    val nombre = galpon.getString("nombreGalpon")
                    val galponEntity = GalponEntity(
                        idGalpon = galpon.getInt("idGalpon"),
                        nombre = nombre,
                        idEstablecimiento = galpon.getString("idEstablecimiento")
                    )

                    val existeGalpon = db.getGalponByName(nombre)
                    if (existeGalpon == null) {
                        db.insertGalpon(galponEntity)
                    } else {
                        db.updateGalpon(galponEntity)
                    }
                }
                db.setTransactionSuccessful()

                return true
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e("DataProcessor", "Error procesando galpones: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    private fun processClientes(clientesNube: JSONArray): Boolean {
        try {
            db.beginTransaction()
            try {
                // Implementar lógica para guardar usuarios en la base de datos local
                for (i in 0 until clientesNube.length()) {
                    val cliente = clientesNube.getJSONObject(i)
                    var idCliente = cliente.getString("idCliente")


                    // Extraer datos del usuario según tu estructura JSON
                    val clienteEntity = ClienteEntity(
                        numeroDocCliente = idCliente,
                        nombreCompleto = cliente.getString("rs"),
                        fechaRegistro = ""
                    )

                    val existeCliente = db.getClienteById(idCliente)
                    if (existeCliente == null) {
                        db.insertCliente(clienteEntity)
                    } else {
                        db.updateCliente(clienteEntity)
                    }
                }
                db.setTransactionSuccessful()

                return true
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e("DataProcessor", "Error procesando clientes: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    private fun processSeries(serieNube: JSONArray): Boolean {
        try {
            db.beginTransaction()
            try {
                for (i in 0 until serieNube.length()) {
                    val serie = serieNube.getJSONObject(i)

                    val nombre = serie.getString("num")

                    val serieDeviceEntity = SerieDeviceEntity(
                        idSerieDevice = serie.getInt("idSerie"),
                        codigo = nombre,
                        mac = serie.getString("macDevice"),
                    )

                    val existeSerie = db.getSerieDevice()

                    if (existeSerie == null) {
                        db.insertSerieDevice(serieDeviceEntity)

                    }
                }
                db.setTransactionSuccessful()

                return true
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e("DataProcessor", "Error procesando series: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    private fun processTempPesos(tempPesos: JSONArray): Boolean {
        var needsSync = false
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
                    }else{
                        db.updatePesoBySerieDevice(serieDevice, serieDeviceEntity)
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

    private fun processConfCapture(confCapture: JSONArray): Boolean {
        var needsSync = false
        try {
            db.beginTransaction()
            try {
                for (i in 0 until confCapture.length()) {
                    val configCap = confCapture.getJSONObject(i)

                    val mac = configCap.getString("macDispositivo")
                    val estado = configCap.getInt("estado")

                    val confCaptureEntity = CaptureDeviceEntity(
                        _idCaptureDevice = 0,
                        _cadenaClave = configCap.getString("cadenaClave"),
                        _nombreDispositivo = configCap.getString("nombreDispositivo"),
                        _macDispositivo = configCap.getString("macDispositivo"),
                        _longitud = configCap.getInt("longitud"),
                        _formatoPeo = configCap.getInt("formatoPeo"),
                        _estado = configCap.getInt("estado"),
                        _cadenaClaveCierre = configCap.getString("cadenaClaveCierre"),
                        _bloque = configCap.getString("bloque"),
                        _isSync = "1"
                    )

                    val existeConfig = db.obtenerConfCapturePorMac(mac)

                    if (existeConfig == null) {
                        db.insertarConfCapture(confCaptureEntity)
                    }else{
                        db.actualizarConfCapture(confCaptureEntity)
                    }

                    if (estado > 0){
                        db.actualizarEstadoPorMac(mac)
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

    private fun processVentas(ventaNube: JSONArray, detallesVentaNube: JSONArray): Boolean {
        var needsSync = false
        try {
            db.beginTransaction()
            try {
                // Mapear detalles por idPesoPollo para acceso rápido
                val detallesMap = HashMap<String, MutableList<JSONObject>>()

                for (i in 0 until detallesVentaNube.length()) {
                    val detalle = detallesVentaNube.getJSONObject(i)
                    val idPesoPollo = detalle.getString("idPesoPollo")

                    if (!detallesMap.containsKey(idPesoPollo)) {
                        detallesMap[idPesoPollo] = mutableListOf()
                    }

                    detallesMap[idPesoPollo]?.add(detalle)
                }

                // Procesar pesos (ventas)
                for (i in 0 until ventaNube.length()) {
                    val venta = ventaNube.getJSONObject(i)
                    val idVenta = venta.getInt("idPesoPollos")

                    // Extraer datos del peso
                    var serieNum = venta.getString("serie")
                    var partes = serieNum.split("-")
                    var serie = partes[0] // "CA48"
                    var correlativo = partes[1]
                    // Verificar si el peso ya existe localmente

                    val existeVenta = db.getAllDataPesoPollosBySerie(serie, correlativo)

                    if (existeVenta.isEmpty()) {
                        guardarVentaAndDetalles(
                            venta,
                            detallesMap[idVenta.toString()] ?: mutableListOf()
                        )
                    }
                }

                // Verificar si hay pesos locales que no están en la nube (necesitan sincronización)
                val pesosLocales = db.getAllDataPesoPollosNotSync()
                if (pesosLocales.isNotEmpty()) {
                    needsSync = true
                }
                db.setTransactionSuccessful()
                return needsSync
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e("DataProcessor", "Error procesando pesos y detalles: ${e.message}")
            e.printStackTrace()
            return false
        }
    }


    // FUNCION AUXILIAR PARA VENTAS
    private fun guardarVentaAndDetalles(venta: JSONObject, detalles: List<JSONObject>) {
        try {
            db.beginTransaction()
            try {

                val idVenta = venta.getInt("idPesoPollos")

                val serieNum = venta.getString("serie")
                val partes = serieNum.split("-")
                val serie = partes[0] // "CA48"
                val correlativo = partes[1]

                val pesoEntity = DataPesoPollosEntity(
                    id = idVenta,
                    serie = serie,
                    numero = correlativo,
                    fecha = venta.getString("fecha"),
                    totalJabas = venta.getString("totalJabas"),
                    totalPollos = venta.getString("totalPollos"),
                    totalPeso = venta.getString("totalPeso"),
                    totalPesoJabas = venta.getString("tara"),
                    totalNeto = venta.getString("neto"),
                    PKPollo = venta.getString("precio_kilo"),
                    TotalPagar = venta.getString("total_pagar"),
                    tipo = venta.getString("tipo"),
                    numeroDocCliente = venta.getString("docCliente"),
                    nombreCompleto = venta.getString("nomCliente"),
                    idGalpon = venta.getString("IdGalpon"),
                    idNucleo = venta.getString("idNucleo"),
                    idUsuario = venta.getString("idUsuario"),
                    idEstado = "1"
                )

                db.insertDataPesoPollos(pesoEntity)

                // Guardar detalles del peso
                for (detalleJson in detalles) {
                    val idDetaPP = detalleJson.getInt("idDetaPP")
                    val cantJabas = detalleJson.getInt("cantJabas")
                    val cantPollos = detalleJson.getInt("cantPollos")
                    val pesoValue = detalleJson.getDouble("peso")
                    val tipo = detalleJson.getString("tipo")
                    val idPesoPollo = detalleJson.getString("idPesoPollo")
                    val fechaPeso = detalleJson.getString("fechaPeso")

                    val detalleEntity = DataDetaPesoPollosEntity(
                        idDetaPP = idDetaPP,
                        cantJabas = cantJabas,
                        cantPollos = cantPollos,
                        peso = pesoValue,
                        tipo = tipo,
                        idPesoPollo = idPesoPollo,
                        fechaPeso = fechaPeso
                    )

                    db.insertDataDetaPesoPollos(detalleEntity)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e("DataProcessor", "Error guardando peso y detalles: ${e.message}")
            e.printStackTrace()
        }
    }
}