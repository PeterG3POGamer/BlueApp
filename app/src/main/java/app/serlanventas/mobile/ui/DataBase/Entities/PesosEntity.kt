package app.serlanventas.mobile.ui.DataBase.Entities

import org.json.JSONObject

data class PesosEntity(
    val id: Int = 0,
    val idNucleo: Int,
    val idGalpon: Int,
    val numeroDocCliente: String,
    val nombreCompleto: String?,
    val dataPesoJson: String,
    val dataDetaPesoJson: String,
    val idEstado: String,
    val isSync: String,
    val devicedName: String,
    val serieDevice: String,
    val fechaRegistro: String
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("idPeso", id)
        json.put("idGalpon", idGalpon)
        json.put("idNucleo", idNucleo)
        json.put("numeroDocCliente", numeroDocCliente)
        json.put("nombreCompleto", nombreCompleto)
        json.put("dataPesoJson", dataPesoJson)
        json.put("dataDetaPesoJson", dataDetaPesoJson)
        json.put("idEstado", idEstado)
        json.put("isSync", isSync)
        json.put("deviceName", devicedName)
        json.put("serieDevice", serieDevice)
        json.put("fechaRegistro", fechaRegistro)
        return json
    }
}
