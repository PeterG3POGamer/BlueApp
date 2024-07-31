package com.example.blueapp.ui.slideshow

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

//BluetoothConnectionService.kj
class BluetoothConnectionService(
    private val bluetoothAdapter: BluetoothAdapter,
    private val onMessageReceived: (String) -> Unit // Callback para manejar datos recibidos
) {

    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // UUID para SPP (Serial Port Profile)
    private var connectThread: ConnectThread? = null
    private var connectedThread: ConnectedThread? = null
    private var acceptThread: AcceptThread? = null
    private val openSockets = mutableListOf<BluetoothSocket>()

    fun connect(device: BluetoothDevice) {
//        closeConnection(device)
        connectThread = ConnectThread(device)
        connectThread?.start()
    }

    fun startServer() {
        acceptThread = AcceptThread()
        acceptThread?.start()
    }

    fun write(out: ByteArray) {
        connectedThread?.write(out)
    }

    fun closeConnection(device: BluetoothDevice): Boolean {
        try {
            connectThread?.cancelAndJoin()
        } catch (e: Exception) {
            Log.e("Bluetooth", "Error al cerrar connectThread", e)
        }

        try {
            connectedThread?.cancelAndJoin()
        } catch (e: Exception) {
            Log.e("Bluetooth", "Error al cerrar connectedThread", e)
        }

//        try {
//            acceptThread?.cancelAndJoin()
//        } catch (e: Exception) {
//            Log.e("Bluetooth", "Error al cerrar acceptThread", e)
//        }

        connectThread = null
        connectedThread = null
//        acceptThread = null

        // Cerrar todos los sockets abiertos
        closeAllSockets()

        var result = false
        device.let {
            try {
                val method = device.javaClass.getMethod("removeBond")
                result = method.invoke(device) as Boolean
                Log.d("Bluetooth", "Dispositivo desvinculado: ${device.address}")
            } catch (e: Exception) {
                Log.e("Bluetooth", "Error al desvincular el dispositivo", e)
            }
        }
        return result
    }

    private fun closeAllSockets() {
        for (socket in openSockets) {
            try {
                socket.close()
            } catch (e: IOException) {
                Log.e("Bluetooth", "Error al cerrar el socket", e)
            }
        }
        openSockets.clear()
    }

    private fun Thread.cancelAndJoin() {
        try {
            interrupt()
            join(500)  // Espera hasta 500 ms para que el thread termine
        } catch (e: InterruptedException) {
            Log.e("Bluetooth", "Error al esperar a que el thread termine", e)
        }
    }

    @SuppressLint("MissingPermission")
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
                Log.e("Bluetooth", "El dispositivo no está emparejado. No se puede conectar.")
                return
            }

            socket?.let {
                try {
                    it.connect()
                    connected(it)
                } catch (e: IOException) {
                    Log.e("Bluetooth", "No se pudo conectar", e)
                    // Intentar cerrar el socket
                    try {
                        it.close()
                    } catch (closeException: IOException) {
                        Log.e("Bluetooth", "No se pudo cerrar el socket después de un fallo de conexión", closeException)
                    }
                    return
                }
            }
        }

        fun cancel() {
            try {
                socket?.close()
            } catch (e: IOException) {
                Log.e("Bluetooth", "No se pudo cerrar el socket en cancel", e)
            }
        }
    }

    private fun connected(socket: BluetoothSocket) {
        connectedThread = ConnectedThread(socket)
        connectedThread?.start()
        openSockets.add(socket)
    }

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
                        Log.w("Bluetooth", "Conexión Bluetooth cerrada")
                        break
                    }

                    bufferPosition += bytes

                    processReceivedData()
                }
            } catch (e: IOException) {
                Log.e("Bluetooth", "Error al leer", e)
            } finally {
                cancel()
            }
        }

        private fun processReceivedData() {
            val data = String(buffer, 0, bufferPosition)
            Log.d("Bluetooth", "Datos crudos recibidos: $data")

            for (char in data) {
                if (char == '\n') {
                    // Fin de un mensaje, procesar el dato acumulado
                    val message = dataAccumulator.toString().trim()
                    if (message.isNotEmpty()) {
                        Log.d("Bluetooth", "Mensaje completo recibido: $message")
                        if (isValidDecimalNumber(message)) {
                            onMessageReceived(message)
                        } else {
                            Log.d("Bluetooth", "Mensaje no es un número decimal válido: $message")
                        }
                    }
                    dataAccumulator.clear()
                } else {
                    // Acumular el carácter
                    dataAccumulator.append(char)
                }
            }

            // Limpiar el buffer
            bufferPosition = 0
        }

        private fun isValidDecimalNumber(message: String): Boolean {
            return message.matches("^-?\\d+(\\.\\d+)?\$".toRegex())
        }


        fun write(bytes: ByteArray) {
            try {
                val message = String(bytes)

                // Filtrar y enviar solo datos decimales válidos
                if (message.matches("-?\\d+(\\.\\d+)?".toRegex())) {
                    outputStream.write(bytes)
                    Log.d("Bluetooth", "Datos enviados: ${String(bytes)}")
                } else {
                    Log.d("Bluetooth", "Datos no válidos: ${String(bytes)}, no se enviaron")
                }
            } catch (e: IOException) {
                Log.e("Bluetooth", "Error al escribir en el BluetoothSocket", e)
            }
        }

        fun cancel() {
            try {
                socket.close()
                Log.d("Bluetooth", "Socket cerrado")
            } catch (e: IOException) {
                Log.e("Bluetooth", "No se pudo cerrar el socket", e)
            }
        }
    }


    @SuppressLint("MissingPermission")
    private inner class AcceptThread : Thread() {
        private val serverSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetoothAdapter.listenUsingRfcommWithServiceRecord("AppName", uuid)
        }

        override fun run() {
            var socket: BluetoothSocket? = null

            while (true) {
                try {
                    socket = serverSocket?.accept()
                } catch (e: IOException) {
                    Log.e("Bluetooth", "Error al aceptar conexión", e)
                    break
                }

                socket?.let {
                    connected(it)
                    return@run  // Salir del método run después de haber aceptado y conectado un socket
                }
            }
        }

        fun cancel() {
            try {
                serverSocket?.close()
            } catch (e: IOException) {
                Log.e("Bluetooth", "No se pudo cerrar el socket en AcceptThread", e)
            }
        }
    }
}
