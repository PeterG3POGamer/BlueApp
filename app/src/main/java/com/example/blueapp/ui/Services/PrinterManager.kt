import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.Socket
import java.nio.charset.Charset

class PrinterManager(
    private val context: Context,
    private val printerIp: String,
    private val printerPort: Int
) {
    companion object {
//        private const val PRINTER_IP = "192.168.100.87"
//        private const val PRINTER_PORT = 9100
        private const val MAX_CHARS_PER_LINE = 48
        private val CHARSET = Charset.forName("CP437")
    }

    suspend fun printFormattedText(formattedText: String) {
        withContext(Dispatchers.IO) {
            try {
                Socket(printerIp, printerPort).use { socket ->
                    val outputStream: OutputStream = socket.getOutputStream()

                    // Inicializar impresora
                    outputStream.write(byteArrayOf(0x1B, 0x40))

                    // Configurar tamaño de fuente
                    outputStream.write(byteArrayOf(0x1D, 0x21, 0x00)) // Tamaño normal

                    // Enviar texto formateado
                    for (line in formattedText.split("\n")) {
                        when {
                            line.startsWith("TITLE:") -> {
                                outputStream.write(byteArrayOf(0x1B, 0x21, 0x10)) // Negrita
                                outputStream.write(byteArrayOf(0x1B, 0x61, 0x01)) // Centrar
                                writeText(outputStream, line.substringAfter("TITLE:").trim())
                                outputStream.write(byteArrayOf(0x1B, 0x21, 0x00)) // Normal
                            }
                            line.startsWith("CENTER:") -> {
                                outputStream.write(byteArrayOf(0x1B, 0x61, 0x01)) // Centrar
                                writeText(outputStream, line.substringAfter("CENTER:").trim())
                            }
                            line.startsWith("LEFT:") -> {
                                outputStream.write(byteArrayOf(0x1B, 0x61, 0x00)) // Alinear izquierda
                                writeText(outputStream, line.substringAfter("LEFT:").trim())
                            }
                            line.startsWith("RIGHT:") -> {
                                outputStream.write(byteArrayOf(0x1B, 0x61, 0x02)) // Alinear derecha
                                writeText(outputStream, line.substringAfter("RIGHT:").trim())
                            }
                            line == "LINE" -> {
                                outputStream.write(byteArrayOf(0x1B, 0x61, 0x00)) // Alinear izquierda
                                writeText(outputStream, "- ".repeat(MAX_CHARS_PER_LINE / 2))
                            }
                            line.startsWith("TABLE:") -> {
                                outputStream.write(byteArrayOf(0x1B, 0x61, 0x01)) // Centrar el texto
                                val tableData = line.substringAfter("TABLE:").split("|")
                                val formattedLine = tableData.mapIndexed { index, cell ->
                                    when (index) {
                                        0 -> cell.padEnd(3)
                                        1 -> cell.padEnd(9)
                                        2 -> cell.padEnd(9)
                                        3 -> cell.padEnd(9)
                                        else -> cell.padEnd(12)
                                    }
                                }.joinToString("").padEnd(MAX_CHARS_PER_LINE)
                                writeText(outputStream, formattedLine)
                            }
                            line.startsWith("TOTAL:") -> {
                                outputStream.write(byteArrayOf(0x1B, 0x61, 0x00)) // Alinear izquierda
                                val (label, value) = line.substringAfter("TOTAL:").split(":", limit = 2)
                                val formattedLine = "${label.padEnd(MAX_CHARS_PER_LINE - value.length)}$value"
                                writeText(outputStream, formattedLine)
                            }
                            else -> writeText(outputStream, line)

                        }
                        outputStream.write("\n".toByteArray(CHARSET))
                    }

                    // Cortar papel
                    outputStream.write(byteArrayOf(0x1D, 0x56, 0x41, 0x10))
                    outputStream.flush()

                    Log.d("PrinterManager", "Texto formateado enviado a la impresora")
                }
            } catch (e: Exception) {
                Log.e("PrinterManager", "Error al imprimir: ${e.message}")
                throw e
            }
        }
    }

    private fun writeText(outputStream: OutputStream, text: String) {
        outputStream.write(text.toByteArray(CHARSET))
    }
}