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


// BluetoothConnectionService.kt
class BluetoothConnectionService(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter,
    private val onMessageReceived: (BluetoothMessage) -> Unit // Callback para manejar datos recibidos
) {

    private val uuid: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // UUID para SPP (Serial Port Profile)
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
        var result = false
        try {
            // Cancelar y unir hilos de conexión
            connectThread?.cancelAndJoin()
            connectedThread?.cancelAndJoin()
            acceptThread?.cancelAndJoin()

            // Cerrar todos los sockets abiertos
            closeAllSockets()

            // Intentar desvincular el dispositivo
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
            // Reiniciar el servicio
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
//                    50:DA:D6:B4:39:14
//                    Redmi Note 11
                    dividename = device.name.toString()
                    it.connect()
                    connected(it)
                } catch (e: IOException) {
                    logger.log("ConnectThread: No se pudo conectar", e)
                    // Intentar cerrar el socket
                    try {
                        it.close()
                    } catch (closeException: IOException) {
                        logger.log(
                            "ConnectThread: No se pudo cerrar el socket después de un fallo de conexión",
                            closeException
                        )
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
                    val bytes =
                        inputStream.read(buffer, bufferPosition, buffer.size - bufferPosition)
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

        // Procesa los datos recibidos, extrayendo los valores después de la clave definida
        // Variable para almacenar datos entre llamadas
        private var datosAcumulados = ""
        private var esperandoFinalizacion = false

        // Procesa los datos recibidos, extrayendo los valores después de la clave definida
        private fun processReceivedData() {
            // Convertir el buffer de bytes a una cadena limpia
            val data = String(buffer, 0, bufferPosition).trim()
            logger.log("$dividename -> ConnectedThread: Datos crudos recibidos: $data")

            // Acumulamos los datos recibidos
            datosAcumulados += data

            val db = AppDatabase(context)
            val confCapture = db.obtenerConfCaptureActivo()

            // Definir valores predeterminados
            val defaultCadenaClave = "Valor:" // Es el patrón que se usará para dividir
            val defaultLongitud = 1  // Longitud de los enteros que se acumulan
            val defaultDecimales = 1 // Número de decimales
            val defaultNumLecturas = 1 // Número de lecturas a tomar en cuenta

            // Obtener la configuración de captura o los valores predeterminados si no existe
            val _CadenaClave = confCapture?._cadenaClave ?: defaultCadenaClave
            val _Longitud = confCapture?._longitud ?: defaultLongitud
            val _Decimales = confCapture?._formatoPeo ?: defaultDecimales
            val _NumLecturas = confCapture?._numLecturas ?: defaultNumLecturas

            // Verificar si tenemos un patrón de inicio
            if (datosAcumulados.contains("=") && !esperandoFinalizacion) {
                esperandoFinalizacion = true
                logger.log("$dividename -> ConnectedThread: Detectado inicio de valor, acumulando datos...")
                // Limpiamos el buffer y esperamos más datos
                bufferPosition = 0
                return
            }

            // Verificar si debemos seguir esperando más datos
            val tieneFinDeLinea = datosAcumulados.contains("\r\n") || datosAcumulados.contains("\n")
            val esLoBastanteLargo = datosAcumulados.length >= 20  // Un límite razonable

            if (esperandoFinalizacion && !tieneFinDeLinea && !esLoBastanteLargo) {
                logger.log("$dividename -> ConnectedThread: Continuando acumulación, datos hasta ahora: $datosAcumulados")
                // Limpiamos el buffer y esperamos más datos
                bufferPosition = 0
                return
            }

            // Si llegamos aquí, procesamos los datos acumulados
            logger.log("$dividename -> ConnectedThread: Procesando datos completos: $datosAcumulados")

            // Normaliza los datos eliminando saltos de línea y espacios innecesarios
            val datosLimpios = datosAcumulados.replace("\n", " ").replace("\r", " ").trim()

            // Divide los datos en partes usando la clave establecida
            val partes = datosLimpios.split(_CadenaClave).map { it.trim() }

            // Variable para acumular TODOS los dígitos encontrados primero
            var todosLosDigitos = ""

            // Primero acumulamos TODOS los dígitos encontrados después de cada cadena clave
            for (i in 1 until partes.size) { // Empezamos desde 1 para omitir la parte antes de la primera clave
                val parte = partes[i]
                if (parte.isNotEmpty()) {
                    // Extraer solo los dígitos (incluido el punto decimal) de la parte
                    val soloDigitos = parte.filter { it.isDigit() || it == '.' }
                    todosLosDigitos += soloDigitos
                }
            }

            // Si no se encontró ningún valor después de la cadena clave, buscar después del "="
            if (todosLosDigitos.isEmpty()) {
                val indiceInicio = datosLimpios.indexOf("=")
                if (indiceInicio >= 0) {
                    val despuesDelInicio = datosLimpios.substring(indiceInicio + 1)
                    todosLosDigitos = despuesDelInicio.filter { it.isDigit() || it == '.' }
                } else {
                    // Si no hay cadena clave ni =, extraer cualquier número
                    todosLosDigitos = datosLimpios.filter { it.isDigit() || it == '.' }
                }
            }

            logger.log("$dividename -> ConnectedThread: Todos los dígitos acumulados: $todosLosDigitos")

            // Ahora procesamos el valor acumulado según la configuración
            if (todosLosDigitos.isNotEmpty()) {
                // Separamos la parte entera y decimal si existe
                val partes = todosLosDigitos.split(".")
                var parteEntera = partes[0]
                val parteDecimal = if (partes.size > 1) partes[1] else ""

                // Aplicamos la restricción de longitud a la parte entera DESPUÉS de acumular
                if (parteEntera.length > _Longitud) {
                    // Tomamos los últimos dígitos según la longitud configurada
                    parteEntera = parteEntera.takeLast(_Longitud)
                }

                // Reconstruimos el número con la parte decimal
                var valorNumericoFinal = parteEntera
                if (parteDecimal.isNotEmpty()) {
                    valorNumericoFinal += ".$parteDecimal"
                }

                logger.log("$dividename -> ConnectedThread: Valor numérico final: $valorNumericoFinal")

                // Convertimos a double para aplicar el formato
                val numero = try {
                    valorNumericoFinal.toDouble()
                } catch (e: Exception) {
                    logger.log("$dividename -> Error al convertir valor: $valorNumericoFinal, usando 0.0")
                    0.0
                }

                // Aplicamos el formato según los decimales configurados
                val formatoPeso = "%.${_Decimales}f"
                val valorFormateado = String.format(formatoPeso, numero)

                logger.log("$dividename -> ConnectedThread: Valor formateado final: $valorFormateado")

                // Enviamos el resultado
                onMessageReceived(BluetoothMessage(datosAcumulados, valorFormateado))
            } else {
                logger.log("$dividename -> ConnectedThread: No se encontraron valores numéricos después de la cadena clave")
            }

            // Reiniciamos las variables de acumulación
            datosAcumulados = ""
            esperandoFinalizacion = false

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
