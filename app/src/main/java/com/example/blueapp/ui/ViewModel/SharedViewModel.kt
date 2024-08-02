package com.example.blueapp.ui.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    private val _pesoValue = MutableLiveData<String>()
    val pesoValue: LiveData<String> get() = _pesoValue

    private var _dataPesoPollosJson: String? = null
    private var _dataDetaPesoPollosJson: String? = null

    private var _idNucleoTemp: Int? = 0

    private var _idListPesos: Int? = 0

    private var _contadorJabas: Int? = 0

    private var _contadorJabasA: Int? = 0

    private var boton: Boolean = false

    private val _connectedDeviceName = MutableLiveData<String>()
    val connectedDeviceName: LiveData<String> = _connectedDeviceName

    var _connectedDeviceAddress: String? = ""

    fun updatePesoValue(message: String) {
        _pesoValue.postValue(message)
    }

    fun updateConnectedDeviceName(name: String) {
        _connectedDeviceName.value = name
    }
    fun updateConnectedDeviceAddress(address: String) {
        _connectedDeviceAddress = address
    }

    fun setDataPesoPollosJson(data: String) {
        _dataPesoPollosJson = data
    }

    fun setDataDetaPesoPollosJson(data: String) {
        _dataDetaPesoPollosJson = data
    }
    fun getConnectedDeviceAddress(): String? {
        return _connectedDeviceAddress
    }
    fun getDataPesoPollosJson(): String? {
        return _dataPesoPollosJson
    }

    fun getDataDetaPesoPollosJson(): String? {
        return _dataDetaPesoPollosJson
    }


    fun setBtnTrue() {
        boton = true
    }
    fun setBtnFalse() {
        boton = false
    }

    fun getbtnStatus(): Boolean {
        return boton
    }

    fun setIdNucleoTemp(data: Int) {
        _idNucleoTemp = data
    }

    fun setIdListPesos(data: Int) {
        _idListPesos = data
    }

    fun getIdNucleoTemp(data: Int) {
        _idNucleoTemp = data
    }

    fun setContadorJabas(data: Int) {
        _contadorJabas = data
    }

    fun getContadorJabas(): Int? {
        return _contadorJabas
    }

    fun getContadorJabasAntiguo(): Int? {
        return _contadorJabasA
    }

    fun setContadorJabasAntiguo(data: Int) {
        _contadorJabasA = data
    }

    fun getIdListPesos(): Int? {
        return _idListPesos
    }


    // Variables LiveData para los totales
    private val _totalJabas = MutableLiveData<Int>()
    val totalJabas: LiveData<Int> = _totalJabas

    private val _totalPollos = MutableLiveData<Int>()
    val totalPollos: LiveData<Int> = _totalPollos

    private val _totalPeso = MutableLiveData<Double>()
    val totalPeso: LiveData<Double> = _totalPeso

    private val _totalPesoNeto = MutableLiveData<Double>()
    val totalPesoNeto: LiveData<Double> = _totalPesoNeto

    fun setTotales(jabas: Int, pollos: Int, peso: Double, neto: Double) {
        _totalJabas.value = jabas
        _totalPollos.value = pollos
        _totalPeso.value = peso
        _totalPesoNeto.value = neto
    }
}
    