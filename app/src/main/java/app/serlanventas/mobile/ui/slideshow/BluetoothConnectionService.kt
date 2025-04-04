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
    private val onMessageReceived: (BluetoothMessage) -> Unit,
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
        private var valorCorrectoNumero = "0.00"
        private var ultimaConfig = ""

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
            val _Decimales = config._formatoPeo ?: 2 // Cantidad de numeros decimales
            val _bloque = config._bloque
            val _idConfig = config._idCaptureDevice

            if (ultimaConfig != _idConfig.toString()){
                ultimoValorCorrecto = "0.00"
                ultimaConfig = _idConfig.toString()
            }

            val valores = extraerValores(datos)
            if (valores.isNotEmpty()) {
                val ultimoValor = when (_bloque) {
                    "1" -> valores.joinToString(separator = "\n") { it } // Concatenar todos los valores
                    "2" -> valores.last() // Usar el último valor
                    else -> valores.last() // Por defecto, usar el último valor
                }
                val valorProcesado = procesarValor(ultimoValor, _Decimales)
                if (valorProcesado.isNotEmpty()) {
                    ultimoValorCorrecto = valorProcesado
                    return valorProcesado
                }
            }

            return ultimoValorCorrecto
        }

        private fun extraerValores(datos: String): List<String> {
            Log.d("BluetoothService", "$dividename -> ExtraerValores: Datos recibidos: $datos")
            val patron = "(\\d+(?:\\.\\d+)?)"

            val regex = Regex(patron)
            return regex.findAll(datos).map { it.groupValues[1] }.toList()
        }

        private fun procesarValor(valor: String, decimales: Int): String {
            try {
                val valorNumerico = valor.toDouble()
                val valorFormateado = String.format("%.${decimales}f", valorNumerico)
                return valorFormateado
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

