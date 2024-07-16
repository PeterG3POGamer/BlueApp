package com.example.blueapp.ui.DataBase.Entities

import org.json.JSONObject

data class DataDetaPesoPollosEntity(
    val idDetaPP: Int,
    val cantJabas: Int,
    val cantPollos: Int,
    val peso: Double,
    val tipo: String
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("_DPP_id", idDetaPP)
        json.put("_DPP_cantJabas", cantJabas)
        json.put("_DPP_cantPolllos", cantPollos)
        json.put("_DPP_peso", peso)
        json.put("_DPP_tipo", tipo)
        return json
    }
}