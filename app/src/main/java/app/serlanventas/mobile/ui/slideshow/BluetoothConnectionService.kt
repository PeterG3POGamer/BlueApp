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
        private var datosAcumulados = StringBuilder()
        private var ultimoTiempoRecibido = System.currentTimeMillis()
        private val tiempoEspera = 500L // Tiempo de espera en ms para considerar un mensaje completo
        private var patronesDetectados = 0
        private var bloqueDetectado = 2 // 0: No detectado, 1: Discrepante, 2: Entero
        private var contadorRecepciones = 0
        private var ultimoValorCorrecto = "0.00" // Almacena el último valor procesado correctamente

        override fun run() {
            try {
                while (true) {
                    val bytesDisponibles = inputStream.available()
                    if (bytesDisponibles > 0) {
                        val bytes = inputStream.read(buffer, bufferPosition, Math.min(bytesDisponibles, buffer.size - bufferPosition))
                        if (bytes == -1) {
                            logger.log("BluetoothService: Conexión Bluetooth cerrada")
                            break
                        }
                        bufferPosition += bytes
                        ultimoTiempoRecibido = System.currentTimeMillis()
                        processReceivedData(false)
                    } else {
                        // Verificar si ha pasado suficiente tiempo desde la última recepción
                        val tiempoActual = System.currentTimeMillis()
                        if (datosAcumulados.isNotEmpty() && tiempoActual - ultimoTiempoRecibido > tiempoEspera) {
                            processReceivedData(true)
                        }
                        sleep(100) // Pequeña pausa para no saturar la CPU
                    }
                }
            } catch (e: IOException) {
                logger.log("BluetoothService: Error al leer", e)
            } catch (e: InterruptedException) {
                logger.log("BluetoothService: Thread interrumpido", e)
            } finally {
                cancel()
            }
        }

        private fun detectarTipoBloque(data: String) {
            contadorRecepciones++

            // Obtener configuración
            val db = AppDatabase(context)
            val confCapture = db.obtenerConfCaptureActivo()
            val _CadenaClave = confCapture?._cadenaClave ?: "="
            val _CadenaClaveCierre = confCapture?._cadenaClaveCierre ?: ";"

            // Verificar si el dato contiene patrones completos
            if (data.contains(_CadenaClave) && data.contains(_CadenaClaveCierre)) {
                patronesDetectados++
            }

            // Después de 5 recepciones, determinar el tipo de bloque
            if (contadorRecepciones >= 5 && bloqueDetectado == 0) {
                bloqueDetectado = if (patronesDetectados >= 3) {
                    // Si la mayoría de las recepciones contienen patrones completos, es bloque entero
                    logger.log("$dividename -> Tipo de bloque detectado: ENTERO (2)")
                    2
                } else {
                    // Si pocas recepciones contienen patrones completos, es bloque discrepante
                    logger.log("$dividename -> Tipo de bloque detectado: DISCREPANTE (1)")
                    1
                }
            }
        }

        private fun processReceivedData(forzarProcesamiento: Boolean) {
            val data = String(buffer, 0, bufferPosition).trim()

            if (data.isEmpty() && !forzarProcesamiento) {
                return
            }

            logger.log("$dividename -> ConnectedThread: Datos crudos recibidos: $data")

            // Detectar tipo de bloque si aún no se ha determinado
            if (bloqueDetectado == 0) {
                detectarTipoBloque(data)
            }

            // Obtener configuración
            val db = AppDatabase(context)
            val confCapture = db.obtenerConfCaptureActivo()
            val _CadenaClave = confCapture?._cadenaClave ?: "="
            val _CadenaClaveCierre = confCapture?._cadenaClaveCierre ?: ";"
            val _Longitud = confCapture?._longitud ?: 6
            val _Decimales = confCapture?._formatoPeo ?: 2

            // Acumular datos
            datosAcumulados.append(data)
            bufferPosition = 0

            val datosActuales = datosAcumulados.toString()

            // Verificar si tenemos un patrón completo (inicio y cierre)
            val tieneInicio = datosActuales.contains(_CadenaClave)
            val tieneCierre = datosActuales.contains(_CadenaClaveCierre)

            // Mostrar siempre los datos crudos
            logger.log("$dividename -> ConnectedThread: Datos acumulados: $datosActuales")

            // Si aún no se ha detectado el tipo de bloque, usar modo discrepante por defecto
            val modoBloque = if (bloqueDetectado == 0) 1 else bloqueDetectado

            // Modo de bloques discrepantes
            if (modoBloque == 1) {
                // Verificar si tenemos un patrón completo
                if (tieneInicio && tieneCierre) {
                    // Tenemos al menos un patrón completo
                    procesarDatosCompletos(datosActuales, _CadenaClave, _CadenaClaveCierre, _Longitud, _Decimales, true)

                    // Limpiar buffer solo si se fuerza el procesamiento o si el último carácter es un cierre
                    if (forzarProcesamiento || datosActuales.endsWith(_CadenaClaveCierre)) {
                        datosAcumulados.clear()
                    } else {
                        // Mantener solo la parte después del último cierre
                        val ultimoCierreIndex = datosActuales.lastIndexOf(_CadenaClaveCierre) + _CadenaClaveCierre.length
                        if (ultimoCierreIndex < datosActuales.length) {
                            val restante = datosActuales.substring(ultimoCierreIndex)
                            datosAcumulados.clear()
                            datosAcumulados.append(restante)
                        } else {
                            datosAcumulados.clear()
                        }
                    }
                } else if (tieneCierre) {
                    // Solo tenemos cierre, podría ser parte de un patrón
                    procesarDatosCompletos(datosActuales, _CadenaClave, _CadenaClaveCierre, _Longitud, _Decimales, true)

                    // Mantener solo la parte después del último cierre
                    val ultimoCierreIndex = datosActuales.lastIndexOf(_CadenaClaveCierre) + _CadenaClaveCierre.length
                    if (ultimoCierreIndex < datosActuales.length) {
                        val restante = datosActuales.substring(ultimoCierreIndex)
                        datosAcumulados.clear()
                        datosAcumulados.append(restante)
                    } else {
                        datosAcumulados.clear()
                    }
                } else if (forzarProcesamiento) {
                    // Se fuerza el procesamiento pero no hay patrón completo
                    // Mostrar datos crudos y el último valor correcto
                    onMessageReceived(BluetoothMessage(datosActuales, ultimoValorCorrecto))
                    datosAcumulados.clear()
                } else {
                    // No tenemos ni inicio ni cierre, mostrar datos crudos y el último valor correcto
                    onMessageReceived(BluetoothMessage(datosActuales, ultimoValorCorrecto))
                }
            } else {
                // Modo de bloques enteros
                if (forzarProcesamiento || (tieneInicio && tieneCierre)) {
                    procesarDatosCompletos(datosActuales, _CadenaClave, _CadenaClaveCierre, _Longitud, _Decimales, false)
                    datosAcumulados.clear()
                } else {
                    // Mostrar solo datos crudos
                    onMessageReceived(BluetoothMessage(datosActuales, ""))
                }
            }
        }

        private fun procesarDatosCompletos(datosCompletos: String, cadenaClave: String, cadenaClaveCierre: String, longitud: Int, decimales: Int, esDiscrepante: Boolean) {
            logger.log("$dividename -> Procesando datos completos: $datosCompletos")

            // Intentar extraer valores según el patrón configurado
            val valores = extraerValores(datosCompletos, cadenaClave, cadenaClaveCierre)
            logger.log("$dividename -> Valores extraídos: $valores")

            if (valores.isNotEmpty()) {
                // Tomar el último valor encontrado
                val ultimoValor = valores.last()
                val valorProcesado = procesarValor(ultimoValor, longitud, decimales)

                if (valorProcesado.isNotEmpty()) {
                    logger.log("$dividename -> ConnectedThread: Valor procesado final: $valorProcesado")

                    // Actualizar el último valor correcto si estamos en modo discrepante
                    if (esDiscrepante) {
                        ultimoValorCorrecto = valorProcesado
                    }

                    onMessageReceived(BluetoothMessage(datosCompletos, valorProcesado))
                    return
                }

                // Si no se pudo procesar y estamos en modo discrepante, mostrar el último valor correcto
                if (esDiscrepante) {
                    logger.log("$dividename -> ConnectedThread: No se pudo procesar el valor, mostrando último valor correcto: $ultimoValorCorrecto")
                    onMessageReceived(BluetoothMessage(datosCompletos, ultimoValorCorrecto))
                } else {
                    // En modo entero, mostrar solo datos crudos
                    logger.log("$dividename -> ConnectedThread: No se pudo procesar el valor según la configuración")
                    onMessageReceived(BluetoothMessage(datosCompletos, ""))
                }
            } else {
                // Si no se encontraron valores con el patrón, intentar extraer cualquier número
                val numerosEncontrados = extraerCualquierNumero(datosCompletos)

                if (numerosEncontrados.isNotEmpty() && datosCompletos.contains(cadenaClaveCierre)) {
                    val ultimoNumero = numerosEncontrados.last()
                    val valorProcesado = procesarValor(ultimoNumero, longitud, decimales)

                    if (valorProcesado.isNotEmpty()) {
                        logger.log("$dividename -> ConnectedThread: Valor numérico encontrado: $valorProcesado")

                        // Actualizar el último valor correcto si estamos en modo discrepante
                        if (esDiscrepante) {
                            ultimoValorCorrecto = valorProcesado
                        }

                        onMessageReceived(BluetoothMessage(datosCompletos, valorProcesado))
                        return
                    }
                }

                // Si no se encontró ningún valor válido
                if (esDiscrepante) {
                    // En modo discrepante, mostrar el último valor correcto
                    logger.log("$dividename -> ConnectedThread: No se encontraron valores válidos, mostrando último valor correcto: $ultimoValorCorrecto")
                    onMessageReceived(BluetoothMessage(datosCompletos, ultimoValorCorrecto))
                } else {
                    // En modo entero, mostrar solo datos crudos
                    logger.log("$dividename -> ConnectedThread: No se encontraron valores válidos según la configuración")
                    onMessageReceived(BluetoothMessage(datosCompletos, ""))
                }
            }
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

        private fun extraerCualquierNumero(datos: String): List<String> {
            val regex = Regex("\\d+(?:\\.\\d+)?")
            return regex.findAll(datos).map { it.value }.toList()
        }

        private fun procesarValor(valor: String, longitud: Int, decimales: Int): String {
            try {
                val valorNumerico = valor.toDouble()

                // Formatear el número con los decimales especificados
                val valorFormateado = String.format("%.${decimales}f", valorNumerico)

                // Separar la parte entera y decimal
                val partes = valorFormateado.split(".")
                val parteEntera = partes[0]
                val parteDecimal = if (partes.size > 1) partes[1] else ""

                // Verificar si la parte entera cumple con la longitud configurada
                if (longitud > 0 && parteEntera.length > longitud) {
                    // Si la parte entera es más larga que la longitud configurada,
                    // truncar la parte entera a la longitud especificada desde la derecha
                    val parteEnteraTruncada = parteEntera.substring(parteEntera.length - longitud)
                    return "$parteEnteraTruncada.$parteDecimal"
                }

                // Si la parte entera es más corta o igual a la longitud configurada,
                // o si longitud es 0 (sin restricción), devolver el valor formateado
                return valorFormateado

            } catch (e: Exception) {
                logger.log("$dividename -> Error al convertir valor: $valor, ignorando")
                return ""
            }
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