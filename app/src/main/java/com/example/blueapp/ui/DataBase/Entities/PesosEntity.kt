package com.example.blueapp.ui.DataBase.Entities

import org.json.JSONObject

data class PesosEntity(
    val id: Int = 0,
    val idNucleo: Int,
    val idGalpon: Int,
    val numeroDocCliente: String,
    val nombreCompleto: String?,
    val dataPesoJson: String,
    val dataDetaPesoJson: String,
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
        json.put("fechaRegistro", fechaRegistro)
        return json
    }
}
