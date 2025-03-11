package app.serlanventas.mobile.ui.DataBase.Entities

import org.json.JSONObject

data class DataDetaPesoPollosEntity(
    val idDetaPP: Int,
    val cantJabas: Int,
    val cantPollos: Int,
    val peso: Double,
    val tipo: String,
    val idPesoPollo: String,
    val fechaPeso: String
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("_DPP_id", idDetaPP)
        json.put("_DPP_cantJabas", cantJabas)
        json.put("_DPP_cantPolllos", cantPollos)
        json.put("_DPP_peso", peso)
        json.put("_DPP_tipo", tipo)
        json.put("_DPP_idPesoPollo", idPesoPollo)
        json.put("_DPP_fechaPeso", fechaPeso)
        return json
    }
}