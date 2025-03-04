package app.serlanventas.mobile.ui.slideshow

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import app.serlanventas.mobile.ui.DataBase.AppDatabase
import app.serlanventas.mobile.ui.Services.Logger
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
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var connectThread: ConnectThread? = null
    private var connectedThread: ConnectedThread? = null
    private var acceptThread: AcceptThread? = null
    private val openSockets = mutableListOf<BluetoothSocket>()
    private var serverSocket: BluetoothServerSocket? = null
    private val logger = Logger(context)
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
            connectThread?.cancelAndJoin()
            connectedThread?.cancelAndJoin()
            acceptThread?.cancelAndJoin()
            closeAllSockets()
            try {
                val method = device.javaClass.getMethod("removeBond")
                result = method.invoke(device) as Boolean
                logger.log("closeConnection: Dispositivo desvinculado: ${device.address}")
            } catch (e: Exception) {
                logger.log("closeConnection: Error al desvincular el dispositivo", e)
            }
        } catch (e: Exception) {
            logger.log("Error al cerrar la conexión Bluetooth", e)
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
                logger.log("closeAllSockets(): Error al cerrar el socket", e)
            }
        }
        openSockets.clear()
    }

    private fun Thread.cancelAndJoin() {
        try {
            interrupt()
            join(500)
        } catch (e: InterruptedException) {
            logger.log("Thread.cancelAndJoin(): Error al esperar a que el thread termine", e)
        }
    }

    @SuppressLint("MissingPermission")
    private inner class ConnectThread(private val device: BluetoothDevice) : Thread() {
        private val socket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(uuid)
        }

        override fun run() {
            bluetoothAdapter.cancelDiscovery()
            if (device.bondState != BluetoothDevice.BOND_BONDED) {
                logger.log("ConnectThread: El dispositivo no está emparejado. No se puede conectar.")
                return
            }
            socket?.let {
                try {
                    dividename = device.name.toString()
                    it.connect()
                    connected(it)
                } catch (e: IOException) {
                    logger.log("ConnectThread: No se pudo conectar", e)
                    try {
                        it.close()
                    } catch (closeException: IOException) {
                        logger.log("ConnectThread: No se pudo cerrar el socket después de un fallo de conexión", closeException)
                    }
                    return
                }
            }
        }

        fun cancel() {
            try {
                socket?.close()
            } catch (e: IOException) {
                logger.log("ConnectThread: No se pudo cerrar el socket en cancel", e)
            }
        }
    }

    private inner class ConnectedThread(private val socket: BluetoothSocket) : Thread() {
        private val inputStream: InputStream = socket.inputStream
        private val outputStream: OutputStream = socket.outputStream
        private var buffer = ByteArray(1024)
        private var bufferPosition = 0
        private var datosAcumulados = ""

        override fun run() {
            try {
                while (true) {
                    val bytes = inputStream.read(buffer, bufferPosition, buffer.size - bufferPosition)
                    if (bytes == -1) {
                        logger.log("BluetoothService: Conexión Bluetooth cerrada")
                        break
                    }
                    bufferPosition += bytes
                    processReceivedData()
                }
            } catch (e: IOException) {
                logger.log("BluetoothService: Error al leer", e)
            } finally {
                cancel()
            }
        }

        private fun processReceivedData() {
            val data = String(buffer, 0, bufferPosition).trim()
            logger.log("$dividename -> ConnectedThread: Datos crudos recibidos: $data")
            if (data.length == 1) {
                // Recibiendo carácter por carácter
                datosAcumulados += data
                logger.log("$dividename -> ConnectedThread: Acumulando carácter: $datosAcumulados")
                bufferPosition = 0
                return
            }

            // Procesar datos completos (ya sea acumulados o recibidos de golpe)
            val datosCompletos = if (datosAcumulados.isNotEmpty()) datosAcumulados + data else data
            datosAcumulados = ""

            val db = AppDatabase(context)
            val confCapture = db.obtenerConfCaptureActivo()

            val _CadenaClave = confCapture?._cadenaClave ?: ""
            val _Longitud = confCapture?._longitud ?: 1
            val _Decimales = confCapture?._formatoPeo ?: 2
            val _CadenaClaveCierre = confCapture?._cadenaClaveCierre ?: ""

            logger.log("$dividename -> Configuración: Clave='$_CadenaClave', Cierre='$_CadenaClaveCierre', Longitud=$_Longitud, Decimales=$_Decimales")

            val valores = extraerValores(datosCompletos, _CadenaClave, _CadenaClaveCierre)
            logger.log("$dividename -> Valores extraídos: $valores")

            if (valores.isNotEmpty()) {
                val ultimoValor = valores.last()
                val valorProcesado = procesarValor(ultimoValor, _Longitud, _Decimales)
                if (valorProcesado.isNotEmpty()) {
                    logger.log("$dividename -> ConnectedThread: Valor procesado final: $valorProcesado")
                    onMessageReceived(BluetoothMessage(datosCompletos, valorProcesado))
                } else {
                    logger.log("$dividename -> ConnectedThread: El valor no cumple con la configuración")
                    onMessageReceived(BluetoothMessage(datosCompletos, datosCompletos))
                }
            } else {
                logger.log("$dividename -> ConnectedThread: No se encontraron valores válidos según la configuración")
                onMessageReceived(BluetoothMessage(datosCompletos, datosCompletos))
            }

            bufferPosition = 0
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
            val partes = valor.split(".")
            var parteEntera = partes[0].takeLast(longitud)
            val parteDecimal = partes.getOrNull(1) ?: ""

            if (parteEntera.isEmpty() || parteEntera.length > longitud) {
                return ""
            }

            val valorNumerico = try {
                "$parteEntera.$parteDecimal".toDouble()
            } catch (e: Exception) {
                logger.log("$dividename -> Error al convertir valor: $valor, ignorando")
                return ""
            }

            return String.format("%.${decimales}f", valorNumerico)
        }

        fun write(bytes: ByteArray) {
            try {
                val message = String(bytes)
                if (message.matches("-?\\d+(\\.\\d+)?".toRegex())) {
                    outputStream.write(bytes)
                    logger.log("Datos enviados: ${String(bytes)}")
                } else {
                    logger.log("Datos no válidos: ${String(bytes)}, no se enviaron")
                }
            } catch (e: IOException) {
                logger.log("Error al escribir en el BluetoothSocket", e)
            }
        }

        fun cancel() {
            try {
                socket.close()
                logger.log("Socket cerrado")
            } catch (e: IOException) {
                logger.log("No se pudo cerrar el socket", e)
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
                    logger.log("AcceptThread: Nueva conexión aceptada: $socket")
                    socket?.let {
                        connected(it)
                    }
                } catch (e: IOException) {
                    logger.log("AcceptThread: Error al aceptar conexión", e)
                    break
                }
            }
        }

        fun cancel() {
            interrupt()
        }
    }
} 