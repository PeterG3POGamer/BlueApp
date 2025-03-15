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
    var _cadenaClaveCierre: String,  // Cadena de clave del dispositivo Cierre
    var _bloque: String,             // Bloque entero o discrepante
    var _isSync: String              // Estado de Sincronizado
) {
    // Función para convertir el objeto a JSON
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("_cc_idCaptureDevice", _idCaptureDevice)
        json.put("_cc_cadenaClave", _cadenaClave)
        json.put("_cc_nombreDispositivo", _nombreDispositivo)
        json.put("_cc_macDispositivo", _macDispositivo)
        json.put("_cc_longitud", _longitud)
        json.put("_cc_formatoPeo", _formatoPeo)
        json.put("_cc_estado", _estado)
        json.put("_cc_cadenaClaveCierre", _cadenaClaveCierre)
        json.put("_cc_bloque", _bloque)
        json.put("_cc_isSync", _isSync)
        return json
    }
}
