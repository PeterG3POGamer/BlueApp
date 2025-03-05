package app.serlanventas.mobile.ui.slideshow

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import app.serlanventas.mobile.ui.DataBase.AppDatabase
import app.serlanventas.mobile.ui.DataBase.Entities.CaptureDeviceEntity
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

data class BluetoothMessage(val rawData: String, val processedValue: String)

class BluetoothConnectionService(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter,
    private val onMessageReceived: (BluetoothMessage) -> Unit
) {
    private val TAG = "BluetoothConnectionService"
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var connectThread: ConnectThread? = null
    private var connectedThread: ConnectedThread? = null
    private var acceptThread: AcceptThread? = null
    private val openSockets = mutableListOf<BluetoothSocket>()
    private var serverSocket: BluetoothServerSocket? = null
    private var dividename: String = "Sin Conexión"

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        dividename = device.name.toString()
        connectThread = ConnectThread(device)
        connectThread?.start()
    }

    @SuppressLint("MissingPermission")
    fun startServer() {
        if (serverSocket == null) {
            serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("AppName", uuid)
        }
        acceptThread = AcceptThread()
        acceptThread?.start()
    }

    private fun connected(socket: BluetoothSocket) {
        connectedThread = ConnectedThread(socket)
        connectedThread?.start()
        openSockets.add(socket)
    }

    fun closeConnection(device: BluetoothDevice): Boolean {
        var result = false
        try {
            connectThread?.cancel()
            connectedThread?.cancel()
            acceptThread?.cancel()
            closeAllSockets()
            try {
                val method = device.javaClass.getMethod("removeBond")
                result = method.invoke(device) as Boolean
                Log.d(TAG, "closeConnection: Dispositivo desvinculado: ${device.address}")
            } catch (e: Exception) {
                Log.d(TAG, "closeConnection: Error al desvincular el dispositivo", e)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Error al cerrar la conexión Bluetooth", e)
        } finally {
            restartService()
        }
        return result
    }

    fun restartService() {
        closeAllSockets()
        acceptThread?.cancel()
        acceptThread = null
        startServer()
    }

    private fun closeAllSockets() {
        for (socket in openSockets) {
            try {
                socket.close()
            } catch (e: IOException) {
                Log.d(TAG, "closeAllSockets(): Error al cerrar el socket", e)
            }
        }
        openSockets.clear()
    }

    @SuppressLint("MissingPermission")
    private inner class ConnectThread(private val device: BluetoothDevice) : Thread() {
        private val socket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(uuid)
        }

        override fun run() {
            bluetoothAdapter.cancelDiscovery()
            if (device.bondState != BluetoothDevice.BOND_BONDED) {
                Log.d(TAG, "ConnectThread: El dispositivo no está emparejado. No se puede conectar.")
                return
            }
            socket?.let {
                try {
                    dividename = device.name.toString()
                    it.connect()
                    connected(it)
                } catch (e: IOException) {
                    Log.d(TAG, "ConnectThread: No se pudo conectar", e)
                    try {
                        it.close()
                    } catch (closeException: IOException) {
                        Log.d(TAG, "ConnectThread: No se pudo cerrar el socket después de un fallo de conexión", closeException)
                    }
                    return
                }
            }
        }

        fun cancel() {
            try {
                socket?.close()
            } catch (e: IOException) {
                Log.d(TAG, "ConnectThread: No se pudo cerrar el socket en cancel", e)
            }
        }
    }

    private inner class ConnectedThread(private val socket: BluetoothSocket) : Thread() {
        private val inputStream: InputStream = socket.inputStream
        private val outputStream: OutputStream = socket.outputStream
        private var buffer = ByteArray(1024)
        private var bufferPosition = 0
        private var ultimoValorCorrecto = "0.00"

        override fun run() {
            try {
                while (true) {
                    val bytesDisponibles = inputStream.available()
                    if (bytesDisponibles > 0) {
                        val bytes = inputStream.read(buffer, bufferPosition, Math.min(bytesDisponibles, buffer.size - bufferPosition))
                        if (bytes == -1) {
                            Log.d(TAG, "BluetoothService: Conexión Bluetooth cerrada")
                            break
                        }
                        bufferPosition += bytes
                        processReceivedData()
                    } else {
                        sleep(100)
                    }
                }
            } catch (e: IOException) {
                Log.d(TAG, "BluetoothService: Error al leer", e)
            } catch (e: InterruptedException) {
                Log.d(TAG, "BluetoothService: Thread interrumpido", e)
            } finally {
                cancel()
            }
        }

        private fun processReceivedData() {
            val datosActuales = String(buffer, 0, bufferPosition).trim()
            bufferPosition = 0

            Log.d(TAG, "$dividename -> ConnectedThread: Datos crudos recibidos: $datosActuales")

            val db = AppDatabase(context)
            val confCapture = db.obtenerConfCaptureActivo()

            val valorProcesado = if (confCapture != null) {
                procesarDatosSegunConfiguracion(datosActuales, confCapture)
            } else {
                "0.00"
            }

            onMessageReceived(BluetoothMessage(datosActuales, valorProcesado))
        }

        private fun procesarDatosSegunConfiguracion(datos: String, config: CaptureDeviceEntity): String {
            val _CadenaClave = config._cadenaClave ?: ""
            val _CadenaClaveCierre = config._cadenaClaveCierre ?: ""
            val _Longitud = config._longitud ?: 0
            val _Decimales = config._formatoPeo ?: 2

            val valores = extraerValores(datos, _CadenaClave, _CadenaClaveCierre)
            if (valores.isNotEmpty()) {
                val ultimoValor = valores.last()
                val valorProcesado = procesarValor(ultimoValor, _Longitud, _Decimales)
                if (valorProcesado.isNotEmpty()) {
                    ultimoValorCorrecto = valorProcesado
                    return valorProcesado
                }
            }

            return ultimoValorCorrecto
        }

        private fun extraerValores(datos: String, cadenaClave: String, cadenaClaveCierre: String): List<String> {
            val patron = when {
                cadenaClave.isNotEmpty() && cadenaClaveCierre.isNotEmpty() ->
                    "${Regex.escape(cadenaClave)}\\s*(\\d+(?:\\.\\d+)?)\\s*${Regex.escape(cadenaClaveCierre)}"
                cadenaClave.isNotEmpty() ->
                    "${Regex.escape(cadenaClave)}\\s*(\\d+(?:\\.\\d+)?)"
                cadenaClaveCierre.isNotEmpty() ->
                    "(\\d+(?:\\.\\d+)?)\\s*${Regex.escape(cadenaClaveCierre)}"
                else ->
                    "(\\d+(?:\\.\\d+)?)"
            }

            val regex = Regex(patron)
            return regex.findAll(datos).map { it.groupValues[1] }.toList()
        }

        private fun procesarValor(valor: String, longitud: Int, decimales: Int): String {
            try {
                val valorNumerico = valor.toDouble()
                val valorFormateado = String.format("%.${decimales}f", valorNumerico)
                val partes = valorFormateado.split(".")
                val parteEntera = partes[0]
                val parteDecimal = if (partes.size > 1) partes[1] else ""

                return if (longitud > 0 && parteEntera.length > longitud) {
                    val parteEnteraTruncada = parteEntera.substring(parteEntera.length - longitud)
                    "$parteEnteraTruncada.$parteDecimal"
                } else {
                    valorFormateado
                }
            } catch (e: Exception) {
                Log.d(TAG, "$dividename -> Error al convertir valor: $valor, ignorando")
                return ""
            }
        }

        fun write(bytes: ByteArray) {
            try {
                outputStream.write(bytes)
            } catch (e: IOException) {
                Log.d(TAG, "Error al escribir en el BluetoothSocket", e)
            }
        }

        fun cancel() {
            try {
                socket.close()
            } catch (e: IOException) {
                Log.d(TAG, "No se pudo cerrar el socket", e)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private inner class AcceptThread : Thread() {
        override fun run() {
            var socket: BluetoothSocket?
            while (!isInterrupted) {
                try {
                    socket = serverSocket?.accept()
                    Log.d(TAG, "AcceptThread: Nueva conexión aceptada: $socket")
                    socket?.let {
                        connected(it)
                    }
                } catch (e: IOException) {
                    Log.d(TAG, "AcceptThread: Error al aceptar conexión", e)
                    break
                }
            }
        }

        fun cancel() {
            interrupt()
        }
    }
}

