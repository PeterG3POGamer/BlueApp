package app.serlanventas.mobile.ui.DataBase.Entities

import org.json.JSONObject

data class CaptureDeviceEntity(
    var _idCaptureDevice: Int,       // ID del dispositivo de captura
    val _cadenaClave: String,        // Cadena de clave del dispositivo inicio
    val _nombreDispositivo: String,  // Nombre del dispositivo
    val _macDispositivo: String,     // Dirección MAC del dispositivo
    val _longitud: Int,              // Longitud del dispositivo
    val _formatoPeo: Int,            // Formato del PE
    var _estado: Int,                // ESTADO 1 O 0
    var _cadenaClaveCierre: String      // Cadena de clave del dispositivo Cierre
) {
    // Función para convertir el objeto a JSON
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("_CC_idCaptureDevice", _idCaptureDevice)
        json.put("_CC_cadenaClave", _cadenaClave)
        json.put("_CC_nombreDispositivo", _nombreDispositivo)
        json.put("_CC_macDispositivo", _macDispositivo)
        json.put("_CC_longitud", _longitud)
        json.put("_CC_formatoPeo", _formatoPeo)
        json.put("_CC_estado", _estado)
        json.put("_CC_cadenaClaveCierre", _cadenaClaveCierre)
        return json
    }
}
