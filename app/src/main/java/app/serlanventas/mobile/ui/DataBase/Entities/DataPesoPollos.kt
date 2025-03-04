package app.serlanventas.mobile.ui.DataBase.Entities

import org.json.JSONObject

data class DataPesoPollosEntity(
    val id: Int,
    val serie: String,
    val fecha: String = "",
    var totalJabas: String,
    var totalPollos: String,
    var totalPeso: String,
    val tipo: String,
    val numeroDocCliente: String,
    var nombreCompleto: String? = null,
    val idGalpon: String,
    val idNucleo: String,
    val PKPollo: String,
    val totalPesoJabas: String,
    val totalNeto: String,
    val TotalPagar: String,
    val idUsuario: String,
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("_PP_id", id)
        json.put("_PP_serie", serie)
        json.put("_PP_fecha", fecha)
        json.put("_PP_totalJabas", totalJabas)
        json.put("_PP_totalPollos", totalPollos)
        json.put("_PP_totalPeso", totalPeso)
        json.put("_PP_tipo", tipo)
        json.put("_PP_docCliente", numeroDocCliente)
        json.put("_PP_IdGalpon", idGalpon)
        json.put("_PP_idNucleo", idNucleo)
        json.put("_PP_PKPollo", PKPollo)
        json.put("_PP_totalPesoJabas", totalPesoJabas)
        json.put("_PP_totalNeto", totalNeto)
        json.put("_PP_TotalPagar", TotalPagar)
        json.put("_PP_idUsuario", idUsuario)
        nombreCompleto?.let {
            json.put("_PP_nombreCompleto", it)
        }
        return json
    }
}