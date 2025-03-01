package app.serlanventas.mobile.ui.DataSyncManager

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import app.serlanventas.mobile.R
import app.serlanventas.mobile.ui.DataBase.AppDatabase
import app.serlanventas.mobile.ui.DataBase.Entities.ClienteEntity
import app.serlanventas.mobile.ui.DataBase.Entities.GalponEntity
import app.serlanventas.mobile.ui.DataBase.Entities.NucleoEntity
import app.serlanventas.mobile.ui.DataBase.Entities.UsuarioEntity
import app.serlanventas.mobile.ui.Jabas.ManagerPost.showCustomToast
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class DataSyncManager(private val context: Context) {

    private val db = AppDatabase(context)

    fun sincronizarData(baseUrl: String, callback: (Boolean) -> Unit, isSincronizar: (Boolean) -> Unit) {
        val urlString = "${baseUrl}controllers/PesoPollosController.php?op=getAllDataSynchronized"

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
                        val dataString = jsonResponse.getString("data")
                        val data = JSONObject(dataString)

                        // Comparar datos locales con los de la nube
                        val necesitaSincronizar = compararDatos(data)

                        withContext(Dispatchers.Main) {
                            isSincronizar(necesitaSincronizar)
                        }

                        if (necesitaSincronizar) {
                            // Procesar usuarios
                            procesarUsuarios(data.getJSONArray("usuarios"))

                            // Procesar establecimientos
                            procesarEstablecimientos(data.getJSONArray("establecimientos"))

                            // Procesar galpones
                            procesarGalpones(data.getJSONArray("galpones"))

                            // Procesar clientes
                            procesarClientes(data.getJSONArray("clientes"))
                        }

                        withContext(Dispatchers.Main) {
                            callback(true)
                        }
                    } catch (e: JSONException) {
                        withContext(Dispatchers.Main) {
                            Log.e("DataSyncManager", "Error al convertir la respuesta a JSON: $inputStream", e)
                            callback(false)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Log.e("DataSyncManager", "Error al enviar datos: $responseCode")
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

    fun showSyncConfirmationDialog(onSync: (Boolean) -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Sincronización de Datos")
            .setMessage("¿Deseas sincronizar los datos ahora? Si no sincronizas, no tendrás los datos actualizados.")
            .setPositiveButton("Sincronizar") { dialog, _ ->
                onSync(true)
                dialog.dismiss()
            }
            .setNegativeButton("Omitir") { dialog, _ ->
                onSync(false)
                dialog.dismiss()
            }
            .show()
    }

    private fun compararDatos(data: JSONObject): Boolean {
        var necesitaSincronizar = false

        // Comparar usuarios
        val usuariosNube = data.getJSONArray("usuarios")
        val usuariosLocales = db.getAllUsuarios()
        if (!compararListas(usuariosNube, usuariosLocales)) {
            necesitaSincronizar = true
        }

        // Comparar establecimientos
        val establecimientosNube = data.getJSONArray("establecimientos")
        val establecimientosLocales = db.getAllNucleos()
        if (!compararListas(establecimientosNube, establecimientosLocales)) {
            necesitaSincronizar = true
        }

        // Comparar galpones
        val galponesNube = data.getJSONArray("galpones")
        val galponesLocales = db.getAllGalpones()
        if (!compararListas(galponesNube, galponesLocales)) {
            necesitaSincronizar = true
        }

        // Comparar clientes
        val clientesNube = data.getJSONArray("clientes")
        val clientesLocales = db.getAllClientes()
        if (!compararListas(clientesNube, clientesLocales)) {
            necesitaSincronizar = true
        }

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

    private fun compararEstablecimientos(establecimientoNube: JSONObject, establecimientoLocal: NucleoEntity): Boolean {
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

    private fun procesarUsuarios(usuarios: JSONArray) {
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

                if (insertResult != -1L) {
                    // insertado correctamente no devolver ningún mensaje
                } else {
                    showCustomToast(context, "Error al guardar el usuario", "error")
                }
            }
        }
    }

    private fun procesarEstablecimientos(establecimientos: JSONArray) {
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

                if (insertResult != -1L) {
                    // insertado correctamente no devolver ningún mensaje
                } else {
                    showCustomToast(context, "Error al guardar el nucleo", "error")
                }
            }
        }
    }

    private fun procesarGalpones(galpones: JSONArray) {
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

                if (insertResult != -1L) {
                    // insertado correctamente no devolver ningún mensaje
                } else {
                    showCustomToast(context, "Error al guardar el nucleo", "error")
                }
            }
        }
    }

    private fun procesarClientes(clientes: JSONArray) {
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

                if (insertResult != -1L) {
                    // insertado correctamente no devolver ningún mensaje
                } else {
                    showCustomToast(context, "Error al guardar el cliente", "error")
                }
            }
        }
    }

    fun showSuccessDialog() {
        // Inflar el diseño personalizado
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_success, null)

        // Configurar el diálogo
        val dialog = AlertDialog.Builder(context)
            .setView(view) // Usar el diseño personalizado
            .create()

        // Personalizar elementos del diseño
        val gifSuccess = view.findViewById<ImageView>(R.id.gifSuccess)
        val titleSuccess = view.findViewById<TextView>(R.id.titleSuccess)
        val messageSuccess = view.findViewById<TextView>(R.id.messageSuccess)
        val btnAceptar = view.findViewById<Button>(R.id.btnAceptar)

        // Cargar el GIF usando Glide
        Glide.with(context)
            .asGif()
            .load(R.drawable.success_animation)
            .into(gifSuccess)

        // Acción del botón "Aceptar"
        btnAceptar.setOnClickListener {
            dialog.dismiss() // Cerrar el diálogo
        }

        // Mostrar el diálogo
        dialog.show()
    }

    fun showErrorDialog(onRetry: ((Boolean) -> Unit)?) {
        AlertDialog.Builder(context)
            .setTitle("Error")
            .setMessage("Hubo un problema al sincronizar los datos. ¿Quieres reintentar o restaurar?")
            .setPositiveButton("Reintentar") { dialog, _ ->
                onRetry?.invoke(true)
                dialog.dismiss()
            }
            .setNegativeButton("Salir") { dialog, _ ->
                onRetry?.invoke(false)
                dialog.dismiss()
            }
            .show()
    }
}
