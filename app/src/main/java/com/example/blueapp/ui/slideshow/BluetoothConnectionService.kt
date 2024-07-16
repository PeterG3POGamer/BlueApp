package com.example.blueapp.ui.slideshow

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import android.os.Handler
import android.os.Looper
import com.example.blueapp.ui.ViewModel.SharedViewModel
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
//BluetoothConnectionService.kj
class BluetoothConnectionService(
    private val bluetoothAdapter: BluetoothAdapter,
    private val onMessageReceived: (String) -> Unit // Callback para manejar datos recibidos
) {

    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // UUID para SPP (Serial Port Profile)
    private var connectThread: ConnectThread? = null
    private var connectedThread: ConnectedThread? = null
    private var acceptThread: AcceptThread? = null

    fun connect(device: BluetoothDevice) {
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

    fun closeConnection() {
        connectThread?.cancel()
        connectedThread?.cancel()
        acceptThread?.cancel()
    }

    @SuppressLint("MissingPermission")
    private inner class ConnectThread(private val device: BluetoothDevice) : Thread() {
        private val socket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(uuid)
        }

        @SuppressLint("MissingPermission")
        override fun run() {
            bluetoothAdapter.cancelDiscovery()

            socket?.let {
                try {
                    it.connect()
                    connected(it)
                } catch (e: IOException) {
                    Log.e("Bluetooth", "No se pudo conectar", e)
                    try {
                        it.close()
                    } catch (e: IOException) {
                        Log.e("Bluetooth", "No se pudo cerrar el socket", e)
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
    }

    fun sendWeightData(weightInKg: Double) {
        connectedThread?.let { thread ->
            val weightString = "$weightInKg\n" // Añadimos '\n' para indicar final del mensaje
            thread.write(weightString.toByteArray())
        }
    }

    private inner class ConnectedThread(private val socket: BluetoothSocket) : Thread() {
        private val inputStream: InputStream = socket.inputStream
        private val outputStream: OutputStream = socket.outputStream

        private var buffer = ByteArray(1024)
        private var bufferPosition = 0

        override fun run() {
            try {
                while (true) {
                    val bytes = inputStream.read(buffer, bufferPosition, buffer.size - bufferPosition)
                    if (bytes == -1) {
                        // El socket Bluetooth se ha cerrado
                        Log.w("Bluetooth", "Conexión Bluetooth cerrada")
                        break
                    }

                    bufferPosition += bytes

                    // Procesar datos si hay un mensaje completo recibido
                    processReceivedData()
                }
            } catch (e: IOException) {
                Log.e("Bluetooth", "Error al leer", e)
            } finally {
                cancel()
            }
        }

        private fun processReceivedData() {
            // Convertir el buffer a String y buscar mensajes completos
            val data = String(buffer, 0, bufferPosition)
            val messages = data.split("\n")

            // Variable para mantener el fragmento del mensaje incompleto
            var incompleteMessage = ""

            // Procesar cada mensaje completo
            for (message in messages) {
                val cleanedMessage = message.trim()
                if (cleanedMessage.isNotEmpty() && cleanedMessage != "=") {
                    if (incompleteMessage.isNotEmpty()) {
                        // Combinar el mensaje incompleto anterior con el actual para formar un mensaje completo
                        val completeMessage = incompleteMessage + cleanedMessage
                        incompleteMessage = ""

                        Log.d("Bluetooth", "Mensaje completo recibido: $completeMessage")

                        // Validar si el mensaje es un número decimal válido con un solo decimal
                        if (isValidDecimalNumber(completeMessage)) {
                            onMessageReceived(completeMessage)
                        } else {
                            Log.d("Bluetooth", "Mensaje recibido no es un número decimal válido: $completeMessage")
                        }
                    } else {
                        // Mensaje completo recibido directamente
                        Log.d("Bluetooth", "Mensaje completo recibido: $cleanedMessage")

                        // Validar si el mensaje es un número decimal válido con un solo decimal
                        if (isValidDecimalNumber(cleanedMessage)) {
                            onMessageReceived(cleanedMessage)
                        } else {
                            Log.d("Bluetooth", "Mensaje recibido no es un número decimal válido: $cleanedMessage")
                        }
                    }
                } else {
                    Log.d("Bluetooth", "Mensaje recibido no deseado: $cleanedMessage, ignorado")
                }
            }

            // Mantener el fragmento de mensaje incompleto en el buffer
            bufferPosition = 0
        }

        private fun isValidDecimalNumber(message: String): Boolean {
            // Verificar si el mensaje es un número decimal con un solo decimal
            return message.matches("^-?\\d+\\.\\d\$".toRegex())
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
