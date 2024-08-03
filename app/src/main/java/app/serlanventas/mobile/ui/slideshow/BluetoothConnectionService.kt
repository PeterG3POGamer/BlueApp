package app.serlanventas.mobile.ui.slideshow

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import app.serlanventas.mobile.ui.Services.Logger
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

// BluetoothConnectionService.kt
class BluetoothConnectionService(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter,
    private val onMessageReceived: (String) -> Unit // Callback para manejar datos recibidos
) {

    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // UUID para SPP (Serial Port Profile)
    private var connectThread: ConnectThread? = null
    private var connectedThread: ConnectedThread? = null
    private var acceptThread: AcceptThread? = null
    private val openSockets = mutableListOf<BluetoothSocket>()
    private var serverSocket: BluetoothServerSocket? = null
    private val logger = Logger(context)
    private var dividename: String = "Sin Conexión"

    // Inicia un hilo para conectar con un dispositivo Bluetooth específico.
    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        dividename = device.name.toString()
        connectThread = ConnectThread(device)
        connectThread?.start()
    }

    // Inicia un hilo que escucha conexiones entrantes desde otros dispositivos Bluetooth.
    @SuppressLint("MissingPermission")
    fun startServer() {
        if (serverSocket == null) {
            serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("AppName", uuid)
        }
        acceptThread = AcceptThread()
        acceptThread?.start()
    }

    // Maneja una conexión establecida, iniciando un hilo para manejar la comunicación.
    private fun connected(socket: BluetoothSocket) {
        connectedThread = ConnectedThread(socket)
        connectedThread?.start()
        openSockets.add(socket)
    }

    // Cierra la conexión Bluetooth con un dispositivo específico y limpia recursos.
    fun closeConnection(device: BluetoothDevice): Boolean {
        try {
            connectThread?.cancelAndJoin()
        } catch (e: Exception) {
            logger.log("Error al cerrar connectThread", e)
        }

        try {
            connectedThread?.cancelAndJoin()
        } catch (e: Exception) {
            logger.log("Error al cerrar connectedThread", e)
        }

        try {
            acceptThread?.cancelAndJoin()
        } catch (e: Exception) {
            logger.log("Error al cerrar acceptThread", e)
        }

        connectThread = null
        connectedThread = null

        var result = false
        device.let {
            try {
                val method = device.javaClass.getMethod("removeBond")
                result = method.invoke(device) as Boolean
                logger.log("closeConnection: Dispositivo desvinculado: ${device.address}")
            } catch (e: Exception) {
                logger.log("closeConnection: Error al desvincular el dispositivo", e)
            }
        }

        restartService()
        return result
    }

    fun restartService() {
        closeAllSockets()
        acceptThread?.cancel()
        acceptThread = null
        startServer()
    }

    // Cierra todos los sockets abiertos y limpia la lista.
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

    // Extensión para cancelar un hilo y esperar a que termine.
    private fun Thread.cancelAndJoin() {
        try {
            interrupt()
            join(500)  // Espera hasta 500 ms para que el thread termine
        } catch (e: InterruptedException) {
            logger.log("Thread.cancelAndJoin(): Error al esperar a que el thread termine", e)
        }
    }

    @SuppressLint("MissingPermission")
    // Hilo que maneja la conexión con un dispositivo Bluetooth.
    private inner class ConnectThread(private val device: BluetoothDevice) : Thread() {
        private val socket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(uuid)
        }

        @SuppressLint("MissingPermission")
        override fun run() {
            // Cancelar descubrimiento para no interferir con la conexión
            bluetoothAdapter.cancelDiscovery()

            // Asegurar que el dispositivo esté emparejado antes de intentar conectar
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
                    // Intentar cerrar el socket
                    try {
                        it.close()
                    } catch (closeException: IOException) {
                        logger.log("ConnectThread: No se pudo cerrar el socket después de un fallo de conexión", closeException)
                    }
                    return
                }
            }
        }

        // Cierra el socket Bluetooth y cancela la conexión.
        fun cancel() {
            try {
                socket?.close()
            } catch (e: IOException) {
                logger.log("ConnectThread: No se pudo cerrar el socket en cancel", e)
            }
        }
    }

    // Hilo que maneja la comunicación con un dispositivo conectado.
    private inner class ConnectedThread(private val socket: BluetoothSocket) : Thread() {
        private val inputStream: InputStream = socket.inputStream
        private val outputStream: OutputStream = socket.outputStream

        private var buffer = ByteArray(1024)
        private var bufferPosition = 0
        private var dataAccumulator = StringBuilder()

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

        // Procesa los datos recibidos, acumulando hasta encontrar un mensaje completo.
        private fun processReceivedData() {
            val data = String(buffer, 0, bufferPosition)
            logger.log("$dividename -> ConnectedThread: Datos crudos recibidos: $data")

            for (char in data) {
                if (char == '\n' || char == '=' || char == ' ') {
                    // Fin de un mensaje, procesar el dato acumulado
                    var message = dataAccumulator.toString().trim()
                    if (message.isNotEmpty()) {
                        if (message.startsWith("=") || message.startsWith(":") || message.startsWith("->") || message.startsWith(">")) {
                            message = message.substring(1).trim()
                        }else if (message.endsWith("=") ){
                            message = message.substring(0, message.length - 1).trim()
                        }

                        logger.log("BluetoothService: Mensaje completo recibido: $message")
                        if (isValidDecimalNumber(message)) {
                            onMessageReceived(message)
                        } else {
                            logger.log("BluetoothService: Mensaje no es un número decimal válido: $message")
                            onMessageReceived(message)
                        }
                    }
                    dataAccumulator.clear()
                } else {
                    // Acumular el carácter
                    if (char.isDigit() || char == '.') {
                        logger.log("BluetoothService: Carácter acumulado: $char")
                        dataAccumulator.append(char)
                    } else {
                        logger.log("BluetoothService: Carácter ignorado: $char")
                    }
                }
            }

            // Limpiar el buffer
            bufferPosition = 0
        }

        // Verifica si un mensaje es un número decimal válido.
        private val decimalNumberRegex = "^-?\\d+(\\.\\d+)?\$".toRegex()
        private fun isValidDecimalNumber(message: String): Boolean {
            return message.matches(decimalNumberRegex)
        }

        // Escribe datos en el socket Bluetooth.
        fun write(bytes: ByteArray) {
            try {
                val message = String(bytes)

                // Filtrar y enviar solo datos decimales válidos
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

        // Cierra el socket y cancela la conexión.
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
    // Hilo que escucha conexiones entrantes y las acepta.
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
            // No cerramos el serverSocket aquí
        }
    }
}
