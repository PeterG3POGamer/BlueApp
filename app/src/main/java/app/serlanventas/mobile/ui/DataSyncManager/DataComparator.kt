package app.serlanventas.mobile.ui.DataSyncManager

import android.util.Log
import app.serlanventas.mobile.ui.DataBase.AppDatabase
import app.serlanventas.mobile.ui.DataBase.Entities.ClienteEntity
import app.serlanventas.mobile.ui.DataBase.Entities.DataPesoPollosEntity
import app.serlanventas.mobile.ui.DataBase.Entities.GalponEntity
import app.serlanventas.mobile.ui.DataBase.Entities.NucleoEntity
import app.serlanventas.mobile.ui.DataBase.Entities.UsuarioEntity
import org.json.JSONArray
import org.json.JSONObject

class DataComparator(private val db: AppDatabase) {

    fun compararDatos(data: JSONObject): Boolean {
        var necesitaSincronizar = false

        // Comparar usuarios
        val usuariosNube = data.getJSONArray("usuarios")
        val usuariosLocales = db.getAllUsuarios()
        if (!compararListas(usuariosNube, usuariosLocales)) {
            necesitaSincronizar = true
            Log.d(
                "DataComparator",
                "Usuarios no coinciden Nube: ${usuariosNube.length()} Locales: ${usuariosLocales.size}"
            )
        }

        // Comparar establecimientos
        val establecimientosNube = data.getJSONArray("establecimientos")
        val establecimientosLocales = db.getAllNucleos()
        if (!compararListas(establecimientosNube, establecimientosLocales)) {
            necesitaSincronizar = true
            Log.d(
                "DataComparator",
                "Establecimientos no coinciden Nube: ${establecimientosNube.length()} Locales: ${establecimientosLocales.size}"
            )
        }

        // Comparar galpones
        val galponesNube = data.getJSONArray("galpones")
        val galponesLocales = db.getAllGalpones()
        if (!compararListas(galponesNube, galponesLocales)) {
            necesitaSincronizar = true
            Log.d(
                "DataComparator",
                "Galpones no coinciden Nube: ${galponesNube.length()} Locales: ${galponesLocales.size}"
            )
        }

        // Comparar clientes
        val clientesNube = data.getJSONArray("clientes")
        val clientesLocales = db.getAllClientes()
        if (!compararListas(clientesNube, clientesLocales)) {
            necesitaSincronizar = true
            Log.d(
                "DataComparator",
                "Clientes no coinciden Nube: ${clientesNube.length()} Locales: ${clientesLocales.size}"
            )
        }

//        val ventasLocalesNotSync = db.getAllDataPesoPollosNotSync()
//        if (ventasLocalesNotSync.isNotEmpty()) {
//            necesitaSincronizar = true
//            Log.d("DataComparator", "Ventas que faltan sincronizar ${ventasLocalesNotSync.size}")
//        } else {
//            val ventasNube = data.getJSONArray("ventas")
//            val ventasLocales = db.getAllDataPesoPollos()
//            if (compararListasVentas(ventasNube, ventasLocales)) {
//                necesitaSincronizar = true
//                Log.d(
//                    "DataComparator",
//                    "Ventas que faltan enviar ${ventasLocales.size}, Ventas que faltan recibir ${ventasNube.length()}"
//                )
//            }
//        }

        return necesitaSincronizar
    }

    private fun <T> compararListas(listaNube: JSONArray, listaLocal: List<T>): Boolean {
        if (listaNube.length() != listaLocal.size) {
            return false
        }

        for (i in 0 until listaNube.length()) {
            val itemNube = listaNube.getJSONObject(i)
            val itemLocal = listaLocal[i]

            when (itemLocal) {
                is UsuarioEntity -> {
                    if (!compararUsuarios(itemNube, itemLocal)) {
                        return false
                    }
                }

                is NucleoEntity -> {
                    if (!compararEstablecimientos(itemNube, itemLocal)) {
                        return false
                    }
                }

                is GalponEntity -> {
                    if (!compararGalpones(itemNube, itemLocal)) {
                        return false
                    }
                }

                is ClienteEntity -> {
                    if (!compararClientes(itemNube, itemLocal)) {
                        return false
                    }
                }
            }
        }
        return true
    }

    private fun compararUsuarios(usuarioNube: JSONObject, usuarioLocal: UsuarioEntity): Boolean {
        return usuarioNube.getString("idUsuario") == usuarioLocal.idUsuario &&
                usuarioNube.getString("nombres") == usuarioLocal.userName &&
                usuarioNube.getString("pass") == usuarioLocal.pass &&
                usuarioNube.getString("idRol") == usuarioLocal.idRol &&
                usuarioNube.getString("rolname") == usuarioLocal.rolName &&
                usuarioNube.getString("idEstablecimiento") == usuarioLocal.idEstablecimiento
    }

    private fun compararEstablecimientos(
        establecimientoNube: JSONObject,
        establecimientoLocal: NucleoEntity
    ): Boolean {
        return establecimientoNube.getString("idEstablecimiento") == establecimientoLocal.idEstablecimiento &&
                establecimientoNube.getString("nucleoName") == establecimientoLocal.nombre &&
                establecimientoNube.getString("idEmpresa") == establecimientoLocal.idEmpresa
    }

    private fun compararGalpones(galponNube: JSONObject, galponLocal: GalponEntity): Boolean {
        return galponNube.getInt("idGalpon") == galponLocal.idGalpon &&
                galponNube.getString("nombreGalpon") == galponLocal.nombre &&
                galponNube.getString("idEstablecimiento") == galponLocal.idEstablecimiento
    }

    private fun compararClientes(clienteNube: JSONObject, clienteLocal: ClienteEntity): Boolean {
        return clienteNube.getString("idCliente") == clienteLocal.numeroDocCliente &&
                clienteNube.getString("nomtit") == clienteLocal.nombreCompleto
    }

    private fun compararListasVentas(
        ventasNube: JSONArray,
        ventasLocales: List<DataPesoPollosEntity>
    ): Boolean {
        if (ventasNube.length() != ventasLocales.size) {
            return true
        }

        for (i in 0 until ventasNube.length()) {
            val ventaNube = ventasNube.getJSONObject(i)
            val ventaLocal = ventasLocales[i]

            if (!compararVentas(ventaNube, ventaLocal)) {
                return false
            }
        }
        return true
    }

    private fun compararVentas(ventaNube: JSONObject, ventaLocal: DataPesoPollosEntity): Boolean {
        return ventaNube.getInt("idPesoPollos") == ventaLocal.id &&
                ventaNube.getString("serie") + "-" + ventaNube.getString("numero") == ventaLocal.serie
    }
}