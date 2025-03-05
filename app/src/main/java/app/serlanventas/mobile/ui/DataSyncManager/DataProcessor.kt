package app.serlanventas.mobile.ui.DataSyncManager

import android.content.Context
import app.serlanventas.mobile.ui.DataBase.AppDatabase
import app.serlanventas.mobile.ui.DataBase.Entities.ClienteEntity
import app.serlanventas.mobile.ui.DataBase.Entities.GalponEntity
import app.serlanventas.mobile.ui.DataBase.Entities.NucleoEntity
import app.serlanventas.mobile.ui.DataBase.Entities.SerieDeviceEntity
import app.serlanventas.mobile.ui.DataBase.Entities.UsuarioEntity
import app.serlanventas.mobile.ui.Jabas.ManagerPost.showCustomToast
import org.json.JSONArray
import org.json.JSONObject

class DataProcessor(private val context: Context, private val db: AppDatabase) {

    fun procesarDatos(data: JSONObject, callback: (SyncResult) -> Unit) {
        procesarUsuarios(data.getJSONArray("usuarios")) { success ->
            if (success) {
                procesarEstablecimientos(data.getJSONArray("establecimientos")) { success ->
                    if (success) {
                        procesarGalpones(data.getJSONArray("galpones")) { success ->
                            if (success) {
                                procesarClientes(data.getJSONArray("clientes")) { success ->
                                    procesarSerieDevice(data.getJSONArray("series")) { success ->
                                        callback(SyncResult.Success(success))
                                    }
                                }
                            } else {
                                callback(SyncResult.Error("Error al procesar galpones"))
                            }
                        }
                    } else {
                        callback(SyncResult.Error("Error al procesar establecimientos"))
                    }
                }
            } else {
                callback(SyncResult.Error("Error al procesar usuarios"))
            }
        }
    }

    private fun procesarUsuarios(usuarios: JSONArray, callback: (Boolean) -> Unit) {
        for (i in 0 until usuarios.length()) {
            val usuario = usuarios.getJSONObject(i)

            val nuevoUsuario = UsuarioEntity(
                idUsuario = usuario.getString("idUsuario"),
                userName = usuario.getString("nombres"),
                pass = usuario.getString("pass"),
                idRol = usuario.getString("idRol"),
                rolName = usuario.getString("rolname"),
                idEstablecimiento = usuario.getString("idEstablecimiento"),
            )
            val usuarioExistente = db.getUsuarioById(usuario.getString("idUsuario"))

            if (usuarioExistente == null) {
                val insertResult = db.insertUsuario(nuevoUsuario)

                if (insertResult == -1L) {
                    showCustomToast(context, "Error al guardar el usuario", "error")
                    callback(false)
                    return
                }
            }
        }
        callback(true)
    }

    private fun procesarEstablecimientos(establecimientos: JSONArray, callback: (Boolean) -> Unit) {
        for (i in 0 until establecimientos.length()) {
            val establecimiento = establecimientos.getJSONObject(i)

            val nuevoNucleo = NucleoEntity(
                idEstablecimiento = establecimiento.getString("idEstablecimiento"),
                nombre = establecimiento.getString("nucleoName"),
                idEmpresa = establecimiento.getString("idEmpresa"),
            )
            val nucleoExistente = db.getNucleoByName(establecimiento.getString("nucleoName"))

            if (nucleoExistente == null) {
                val insertResult = db.insertNucleo(nuevoNucleo)

                if (insertResult == -1L) {
                    showCustomToast(context, "Error al guardar el nucleo", "error")
                    callback(false)
                    return
                }
            }
        }
        callback(true)
    }

    private fun procesarGalpones(galpones: JSONArray, callback: (Boolean) -> Unit) {
        for (i in 0 until galpones.length()) {
            val galpon = galpones.getJSONObject(i)

            val nuevoGalpon = GalponEntity(
                idGalpon = galpon.getInt("idGalpon"),
                nombre = galpon.getString("nombreGalpon"),
                idEstablecimiento = galpon.getString("idEstablecimiento"),
            )
            val galponExistente = db.getGalponByName(galpon.getString("nombreGalpon"))

            if (galponExistente == null) {
                val insertResult = db.insertGalpon(nuevoGalpon)

                if (insertResult == -1L) {
                    showCustomToast(context, "Error al guardar el galpon", "error")
                    callback(false)
                    return
                }
            }
        }
        callback(true)
    }

    private fun procesarClientes(clientes: JSONArray, callback: (Boolean) -> Unit) {
        for (i in 0 until clientes.length()) {
            val cliente = clientes.getJSONObject(i)

            val nuevoCliente = ClienteEntity(
                numeroDocCliente = cliente.getString("idCliente"),
                nombreCompleto = cliente.getString("nomtit"),
                fechaRegistro = ""
            )
            val clienteExistente = db.getClienteById(cliente.getString("idCliente"))

            if (clienteExistente == null) {
                val insertResult = db.insertCliente(nuevoCliente)

                if (insertResult == -1L) {
                    showCustomToast(context, "Error al guardar el cliente", "error")
                    callback(false)
                    return
                }
            }
        }
        callback(true)
    }

    fun procesarSerieDevice(data: JSONArray, callback: (Boolean) -> Unit) {
        for (i in 0 until data.length()) {
            val serie = data.getJSONObject(i)

            val existeDevice = db.getSerieDevice()
            if (existeDevice == null){
                db.deleteAllSeries()

                val nuevoSerieDevice = SerieDeviceEntity(
                    idSerieDevice = serie.getInt("idSerie"),
                    codigo = serie.getString("num"),
                    mac = serie.getString("macDevice"),
                )

                val insertResult = db.insertSerieDevice(nuevoSerieDevice)

                if (insertResult == -1L) {
                    showCustomToast(context, "Error al guardar la serie", "error")
                    callback(false)
                    return
                }
            }
        }
        callback(true)
    }
}