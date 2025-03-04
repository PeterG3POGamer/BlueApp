package app.serlanventas.mobile.ui.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {

    // LiveData para la información de peso
    private val _pesoValue = MutableLiveData<String>()
    val pesoValue: LiveData<String> get() = _pesoValue
    // LiveData para la información de Crudo
    private val _rawData = MutableLiveData<String>()
    val rawData: LiveData<String> get() = _rawData

    // LiveData para la conexión del dispositivo
    private val _connectedDeviceName = MutableLiveData<String>()
    val connectedDeviceName: LiveData<String> get() = _connectedDeviceName

    private val _connectedDeviceAddress = MutableLiveData<String>()
    val connectedDeviceAddress: LiveData<String> get() = _connectedDeviceAddress

    // Variables para almacenar JSON de datos
    private var _dataPesoPollosJson: String? = null
    private var _dataDetaPesoPollosJson: String? = null

    // Variables para IDs y contadores
    private var _idNucleoTemp: Int? = null
    private var _idListPesos: Int? = null
    private var _contadorJabas: Int? = null
    private var _contadorJabasA: Int? = null

    // Estado del botón
    private var boton: Boolean = false

    // LiveData para los totales
    private val _totalJabas = MutableLiveData<Int>()
    val totalJabas: LiveData<Int> get() = _totalJabas

    private val _totalPollos = MutableLiveData<Int>()
    val totalPollos: LiveData<Int> get() = _totalPollos

    private val _totalPeso = MutableLiveData<Double>()
    val totalPeso: LiveData<Double> get() = _totalPeso

    private val _totalPesoNeto = MutableLiveData<Double>()
    val totalPesoNeto: LiveData<Double> get() = _totalPesoNeto

    // Métodos de actualización para los valores
    fun updatePesoValue(message: String) {
        _pesoValue.postValue(message)
    }

    fun updateRawData(rawData: String) {
        _rawData.postValue(rawData)
    }

    fun updateConnectedDeviceName(name: String) {
        _connectedDeviceName.postValue(name)
    }

    fun updateConnectedDeviceAddress(address: String) {
        _connectedDeviceAddress.postValue(address)
    }

    fun getConnectedDeviceAddress(): String? {
        return _connectedDeviceAddress.value
    }

    fun setDataPesoPollosJson(data: String) {
        _dataPesoPollosJson = data
    }

    fun setDataDetaPesoPollosJson(data: String) {
        _dataDetaPesoPollosJson = data
    }

    fun getDataPesoPollosJson(): String? {
        return _dataPesoPollosJson
    }

    fun getDataDetaPesoPollosJson(): String? {
        return _dataDetaPesoPollosJson
    }

    // Métodos para manejar el botón
    fun setBtnTrue() {
        boton = true
    }

    fun setBtnFalse() {
        boton = false
    }

    fun getBtnStatus(): Boolean {
        return boton
    }

    // Métodos para manejar IDs y contadores
    fun setIdNucleoTemp(data: Int) {
        _idNucleoTemp = data
    }

    fun getIdNucleoTemp(): Int? {
        return _idNucleoTemp
    }

    fun setIdListPesos(data: Int) {
        _idListPesos = data
    }

    fun getIdListPesos(): Int? {
        return _idListPesos
    }

    fun setContadorJabas(data: Int) {
        _contadorJabas = data
    }

    fun getContadorJabas(): Int? {
        return _contadorJabas
    }

    fun setContadorJabasAntiguo(data: Int) {
        _contadorJabasA = data
    }

    fun getContadorJabasAntiguo(): Int? {
        return _contadorJabasA
    }

    // Método para establecer los totales
    fun setTotales(jabas: Int, pollos: Int, peso: Double, neto: Double) {
        _totalJabas.postValue(jabas)
        _totalPollos.postValue(pollos)
        _totalPeso.postValue(peso)
        _totalPesoNeto.postValue(neto)
    }


}
