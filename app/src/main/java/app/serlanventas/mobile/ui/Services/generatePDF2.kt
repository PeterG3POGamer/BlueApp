package app.serlanventas.mobile.ui.Services

import PrinterManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import app.serlanventas.mobile.ui.DataBase.AppDatabase
import app.serlanventas.mobile.ui.preliminar.FragmentPreliminar
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.DashedBorder
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.property.HorizontalAlignment
import com.itextpdf.layout.property.TextAlignment
import com.itextpdf.layout.property.UnitValue
import com.itextpdf.layout.property.VerticalAlignment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

suspend fun generateAndOpenPDF2(jsonDataPdf: JSONObject, context: Context, paperWidth: Int = 80) {
    try {
        // Validar que el ancho del papel sea 58mm o 80mm
        val validPaperWidth = if (paperWidth == 80) 80 else 58

        val PESO_POLLO = jsonDataPdf.optJSONArray("PESO_POLLO") ?: JSONArray()
        val CLIENTE = jsonDataPdf.optJSONArray("CLIENTE") ?: JSONArray()
        val GALPON = jsonDataPdf.optJSONArray("GALPON") ?: JSONArray()
        val ESTABLECIMIENTO = jsonDataPdf.optJSONArray("ESTABLECIMIENTO") ?: JSONArray()
        val EMPRESA = jsonDataPdf.optJSONArray("EMPRESA") ?: JSONArray()
        val DETA_PESOPOLLOS = jsonDataPdf.optJSONArray("DETA_PESOPOLLO") ?: JSONArray()

        val nroRuc = EMPRESA.optJSONObject(0)?.optString("nroRuc", "N/A") ?: "N/A"
        val nombreComercial = EMPRESA.optJSONObject(0)?.optString("nombreComercial", "N/A") ?: "N/A"

        val serie = PESO_POLLO.optJSONObject(0)?.optString("serie", "N/A") ?: "N/A"
        val fecha = PESO_POLLO.optJSONObject(0)?.optString("fecha", "N/A") ?: "N/A"
        val dni = CLIENTE.optJSONObject(0)?.optString("dni", "N/A") ?: "N/A"
        val rs = CLIENTE.optJSONObject(0)?.optString("rs", "N/A") ?: "N/A"
        val nombre = ESTABLECIMIENTO.optJSONObject(0)?.optString("nombre", "N/A") ?: "N/A"
        val nomgal = GALPON.optJSONObject(0)?.optString("nomgal", "N/A") ?: "N/A"
        val totalJabas = PESO_POLLO.optJSONObject(0)?.optString("totalJabas", "N/A") ?: "N/A"
        val totalPollos = PESO_POLLO.optJSONObject(0)?.optString("totalPollos", "N/A") ?: "N/A"
        val pesoBruto = PESO_POLLO.optJSONObject(0)?.optString("totalPeso", "N/A") ?: "N/A"
        val tara = PESO_POLLO.optJSONObject(0)?.optString("tara", "N/A") ?: "N/A"
        val neto = PESO_POLLO.optJSONObject(0)?.optString("neto", "N/A") ?: "N/A"
        val precio_kilo = PESO_POLLO.optJSONObject(0)?.optString("precio_kilo", "N/A") ?: "N/A"
        val pesoPromedio = PESO_POLLO.optJSONObject(0)?.optString("pesoPromedio", "N/A") ?: "N/A"
        val total_pagar = PESO_POLLO.optJSONObject(0)?.optString("total_pagar", "N/A") ?: "N/A"

        // Agregar el formato al nombre del archivo
        val fileName = "$nroRuc-$serie-${validPaperWidth}mm.pdf"
        val directory = File(context.getExternalFilesDir(null), "PDFs")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = File(directory, fileName)

        // Verificar si el archivo ya existe para evitar regenerarlo
        if (file.exists()) {
            Log.d("PdfManager", "El archivo PDF ya existe, omitiendo generación: ${file.absolutePath}")

            // Preparar el texto formateado para impresión térmica
            val formattedText = buildString {
                // Encabezado
                appendLine("TITLE:NOTA DE VENTA")
                appendLine("CENTER:RUC: $nroRuc")
                appendLine("CENTER:$nombreComercial")
                appendLine("CENTER:Serie: $serie   Fecha: $fecha")
                appendLine("LINE")

                // Datos del cliente
                appendLine("LEFT:N° Documento: $dni")
                appendLine("LEFT:Cliente: $rs")
                appendLine("LINE")

                // Encabezado de la tabla con 5 columnas
                appendLine("TABLE: ${"#"}| ${"Jabas".padStart(4)}| ${"Pollos".padStart(4)}| ${"Peso".padStart(5)}| ${"Tipo".padStart(8)}")
                appendLine("LINE")

                // Datos de la tabla
                for (i in 0 until DETA_PESOPOLLOS.length()) {
                    val item = DETA_PESOPOLLOS.optJSONObject(i) ?: JSONObject()
                    appendLine("TABLE: ${(i + 1).toString()}| ${item.optString("cantJabas", "").padStart(6)}| ${item.optString("cantPollos", "").padStart(6)}| ${item.optString("peso", "").padStart(6).padEnd(6)}| ${item.optString("tipo", "").padStart(2)}")
                }

                // Línea final de la tabla
                appendLine("LINE")

                // Totales
                appendLine("TOTAL:T. Jabas:${totalJabas.padEnd(36)}")
                appendLine("TOTAL:T. Pollos:${totalPollos.padEnd(36)}")
                appendLine("TOTAL:Ps. Bruto:${pesoBruto.padEnd(36)}")
                appendLine("TOTAL:Tara:${tara.padEnd(36)}")
                appendLine("TOTAL:Neto:${neto.padEnd(36)}")
                appendLine("TOTAL:Precio/Kilo:${precio_kilo.padEnd(36)}")
                appendLine("TOTAL:Ps. Promedio:${pesoPromedio.padEnd(36)}")
                appendLine("TOTAL:T. PAGAR:${total_pagar.padEnd(36)}")
                appendLine("LINE")

                // Pie de página
                appendLine("CENTER:$nombre - $nomgal")
                appendLine("CENTER:¡GRACIAS POR SU COMPRA!")
            }

            // Continuar con el proceso de impresión y notificación
            var db = AppDatabase(context)
            val impresora = db.getImpresoraById("1")
            if (impresora != null) {
                val printerIp = impresora.ip
                val printerPort = impresora.puerto.toInt()

                val printerManager = PrinterManager(context, printerIp, printerPort)

                withContext(Dispatchers.IO) {
                    try {
                        printerManager.printFormattedText(formattedText)
                        showPdfGeneratedNotification(context, file)
                    } catch (e: Exception) {
                        showPdfGeneratedNotification(context, file)
                    }
                }
            } else {
                withContext(Dispatchers.IO) {
                    showPdfGeneratedNotification(context, file)
                }
            }
            return
        }

        // Establecer el ancho en puntos según el formato seleccionado
        val widthInPoints = validPaperWidth * 72 / 25.4f

        // Calcular la altura necesaria en función del número de elementos en la tabla de detalles
        val baseHeightInPoints = 200f * 72 / 25.4f
        val itemHeight = 15f
        val additionalHeight = DETA_PESOPOLLOS.length() * itemHeight
        val totalHeightInPoints = baseHeightInPoints + additionalHeight + 200f

        val pdfWriter = PdfWriter(file)
        val pdfDocument = PdfDocument(pdfWriter)
        // Configurar una página única sin cortes
        pdfDocument.setDefaultPageSize(PageSize(widthInPoints, totalHeightInPoints))
        // Desactivar la división automática de páginas
        pdfDocument.defaultPageSize = PageSize(widthInPoints, totalHeightInPoints)

        val document = Document(pdfDocument)

        // ===== CONFIGURACIÓN DE MÁRGENES =====
        // AJUSTE MANUAL: Aumenta estos valores si necesitas más espacio en los bordes
        // Margen izquierdo: aumentado para dar más espacio a las etiquetas (N° Doc, Cliente, T.Jabas)
        // Margen derecho: aumentado para evitar cortes en los valores
        val leftMargin = if (validPaperWidth == 80) 20f else 15f  // Aumentado para dar más espacio a las etiquetas
        val rightMargin = if (validPaperWidth == 80) 25f else 20f // Aumentado aún más para el lado derecho
        val topMargin = 10f
        val bottomMargin = 10f

        document.setMargins(leftMargin, rightMargin, topMargin, bottomMargin)
        // ===== FIN CONFIGURACIÓN DE MÁRGENES =====

        // Usar fuentes en negrita para mejor impresión térmica
        val boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)

        // Usar un único tamaño de fuente para todo el documento
        val uniformFontSize = 7f

        // Título con fondo gris claro para destacar
        val titleCell = Cell()
            .add(Paragraph("NOTA DE VENTA")
                .setFont(boldFont)
                .setFontSize(uniformFontSize)
                .setTextAlignment(TextAlignment.CENTER)
                .setBold())
            .setBorder(Border.NO_BORDER)
            .setBackgroundColor(ColorConstants.LIGHT_GRAY)
            .setPadding(3f)

        val titleTable = Table(UnitValue.createPercentArray(floatArrayOf(100f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .addCell(titleCell)

        document.add(titleTable)

        // Información de la empresa en una tabla compacta
        val infoTable = Table(UnitValue.createPercentArray(floatArrayOf(100f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginTop(0f)
            .setMarginBottom(0f)

        infoTable.addCell(Cell()
            .add(Paragraph("RUC: $nroRuc")
                .setFont(boldFont)
                .setFontSize(uniformFontSize)
                .setTextAlignment(TextAlignment.CENTER)
                .setBold())
            .setBorder(Border.NO_BORDER)
            .setPadding(1f))

        infoTable.addCell(Cell()
            .add(Paragraph(nombreComercial)
                .setFont(boldFont)
                .setFontSize(uniformFontSize)
                .setTextAlignment(TextAlignment.CENTER)
                .setBold())
            .setBorder(Border.NO_BORDER)
            .setPadding(1f))

        infoTable.addCell(Cell()
            .add(Paragraph("Serie: $serie   Fecha: $fecha")
                .setFont(boldFont)
                .setFontSize(uniformFontSize)
                .setTextAlignment(TextAlignment.CENTER)
                .setBold())
            .setBorder(Border.NO_BORDER)
            .setPadding(1f))

        document.add(infoTable)

        // ===== AJUSTE DE LÍNEAS SEPARADORAS =====
        // AJUSTE MANUAL: Cambia el valor de lineLength para hacer las líneas más cortas o largas
        // Línea separadora más gruesa pero más corta para evitar los bordes
        val lineLength = if (validPaperWidth == 80) 25 else 20 // Reducido para evitar llegar a los bordes
        // ===== FIN AJUSTE DE LÍNEAS SEPARADORAS =====

        document.add(Paragraph("=".repeat(lineLength))
            .setFont(boldFont)
            .setFontSize(uniformFontSize)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(0f)
            .setMarginBottom(0f))

        // ===== AJUSTE DE TABLA DE CLIENTE =====
        // AJUSTE MANUAL: Modifica estos valores para ajustar el ancho de las columnas
        // Primera columna (45f): etiquetas como "N° Documento:" y "Cliente:"
        // Segunda columna (55f): valores como el DNI y nombre del cliente
        val clienteTableColWidths = floatArrayOf(45f, 55f) // Aumentado el ancho de la primera columna
        // ===== FIN AJUSTE DE TABLA DE CLIENTE =====

        // Información del cliente en una tabla de dos columnas
        val clienteTable = Table(UnitValue.createPercentArray(clienteTableColWidths))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginTop(0f)
            .setMarginBottom(0f)

        clienteTable.addCell(Cell()
            .add(Paragraph("N° Documento:")
                .setFont(boldFont)
                .setFontSize(uniformFontSize)
                .setBold())
            .setBorder(Border.NO_BORDER)
            .setPadding(1f))

        clienteTable.addCell(Cell()
            .add(Paragraph(dni)
                .setFont(boldFont)
                .setFontSize(uniformFontSize)
                .setBold())
            .setBorder(Border.NO_BORDER)
            .setPadding(1f))

        clienteTable.addCell(Cell()
            .add(Paragraph("Cliente:")
                .setFont(boldFont)
                .setFontSize(uniformFontSize)
                .setBold())
            .setBorder(Border.NO_BORDER)
            .setPadding(1f))

        clienteTable.addCell(Cell()
            .add(Paragraph(rs)
                .setFont(boldFont)
                .setFontSize(uniformFontSize)
                .setBold())
            .setBorder(Border.NO_BORDER)
            .setPadding(1f))

        document.add(clienteTable)

        // Línea separadora más gruesa pero más corta
        document.add(Paragraph("=".repeat(lineLength))
            .setFont(boldFont)
            .setFontSize(uniformFontSize)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(0f)
            .setMarginBottom(0f))

        // ===== AJUSTE DE TABLA DE DETALLES =====
        // AJUSTE MANUAL: Modifica estos valores para ajustar el ancho de las columnas de la tabla de detalles
        // Valores actuales: 10% para #, 20% para Jabas, 20% para Pollos, 20% para Peso, 30% para Tipo
        val detailsTableColWidths = floatArrayOf(10f, 20f, 20f, 20f, 30f)
        // ===== FIN AJUSTE DE TABLA DE DETALLES =====

        // Tabla de detalles con diseño más compacto y texto más grueso
        val detailsTable = Table(UnitValue.createPercentArray(detailsTableColWidths))
            .setWidth(UnitValue.createPercentValue(100f))
            .setHorizontalAlignment(HorizontalAlignment.CENTER)
            .setMarginTop(0f)
            .setMarginBottom(0f)

        // Encabezados de la tabla
        val headers = arrayOf("#", "Jabas", "Pollos", "Peso", "Tipo")
        headers.forEach { header ->
            detailsTable.addHeaderCell(Cell()
                .add(Paragraph(header)
                    .setFont(boldFont)
                    .setFontSize(uniformFontSize)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold())
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(SolidBorder(1f))
                .setPadding(1f)
                .setVerticalAlignment(VerticalAlignment.MIDDLE))
        }

        // Filas de datos con texto más grueso
        for (i in 0 until DETA_PESOPOLLOS.length()) {
            val item = DETA_PESOPOLLOS.optJSONObject(i) ?: JSONObject()

            // Número de fila
            detailsTable.addCell(Cell()
                .add(Paragraph((i + 1).toString())
                    .setFont(boldFont)
                    .setFontSize(uniformFontSize)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold())
                .setBorder(Border.NO_BORDER)
                .setPadding(1f)
                .setVerticalAlignment(VerticalAlignment.MIDDLE))

            // Datos de la fila
            arrayOf(
                item.optString("cantJabas", ""),
                item.optString("cantPollos", ""),
                item.optString("peso", ""),
                item.optString("tipo", "")
            ).forEach { value ->
                detailsTable.addCell(Cell()
                    .add(Paragraph(value)
                        .setFont(boldFont)
                        .setFontSize(uniformFontSize)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setBold())
                    .setBorder(Border.NO_BORDER)
                    .setPadding(1f)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE))
            }
        }

        document.add(detailsTable)

        // Línea separadora más gruesa pero más corta
        document.add(Paragraph("=".repeat(lineLength))
            .setFont(boldFont)
            .setFontSize(uniformFontSize)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(0f)
            .setMarginBottom(0f))

        // ===== AJUSTE DE TABLA DE TOTALES =====
        // AJUSTE MANUAL: Modifica estos valores para ajustar el ancho de las columnas de totales
        // Primera columna (50f): etiquetas como "T. Jabas:", "T. Pollos:", etc.
        // Segunda columna (50f): valores de los totales
        val totalsTableColWidths = floatArrayOf(50f, 50f) // Aumentado el ancho de la primera columna
        // ===== FIN AJUSTE DE TABLA DE TOTALES =====

        // Tabla de totales con diseño mejorado y texto más grueso
        val totalsTable = Table(UnitValue.createPercentArray(totalsTableColWidths))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginTop(0f)
            .setMarginBottom(0f)

        // Función para agregar filas de totales
        fun addTotalRow(label: String, value: String) {
            totalsTable.addCell(Cell()
                .add(Paragraph(label)
                    .setFont(boldFont)
                    .setFontSize(uniformFontSize)
                    .setTextAlignment(TextAlignment.LEFT)
                    .setBold())
                .setBorder(Border.NO_BORDER)
                .setPadding(1f))

            totalsTable.addCell(Cell()
                .add(Paragraph(value)
                    .setFont(boldFont)
                    .setFontSize(uniformFontSize)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBold())
                .setBorder(Border.NO_BORDER)
                .setPadding(1f))
        }

        // Agregar filas de totales
        addTotalRow("T. Jabas:", totalJabas)
        addTotalRow("T. Pollos:", totalPollos)
        addTotalRow("Ps. Bruto:", pesoBruto)
        addTotalRow("Tara:", tara)
        addTotalRow("Neto:", neto)
        addTotalRow("Precio/Kilo:", precio_kilo)
        addTotalRow("Ps. Promedio:", pesoPromedio)

        // ===== AJUSTE DE CELDA DE TOTAL A PAGAR =====
        // AJUSTE MANUAL: Puedes modificar el padding o el borde para ajustar la apariencia
        // ===== FIN AJUSTE DE CELDA DE TOTAL A PAGAR =====

        // Total a pagar destacado con borde más grueso
        val totalCell = Cell(1, 2)
            .add(Paragraph("TOTAL A PAGAR: $total_pagar")
                .setFont(boldFont)
                .setFontSize(uniformFontSize)
                .setTextAlignment(TextAlignment.CENTER)
                .setBold())
            .setBorder(DashedBorder(0.5f))
            .setBackgroundColor(ColorConstants.LIGHT_GRAY)
            .setPadding(3f)

        totalsTable.addCell(totalCell)

        document.add(totalsTable)

        // Pie de página con texto más grueso
        document.add(Paragraph("$nombre - $nomgal")
            .setFont(boldFont)
            .setFontSize(uniformFontSize)
            .setTextAlignment(TextAlignment.CENTER)
            .setBold()
            .setMarginTop(3f))

        document.add(Paragraph("¡GRACIAS POR SU COMPRA!")
            .setFont(boldFont)
            .setFontSize(uniformFontSize)
            .setTextAlignment(TextAlignment.CENTER)
            .setBold()
            .setMarginTop(1f))

        document.close()

        Log.d("PdfManager", "PDF creado en: ${file.absolutePath}")

        // Formato para impresión térmica con texto más grueso
        val formattedText = buildString {
            // Encabezado
            appendLine("TITLE:NOTA DE VENTA")
            appendLine("CENTER:RUC: $nroRuc")
            appendLine("CENTER:$nombreComercial")
            appendLine("CENTER:Serie: $serie   Fecha: $fecha")
            appendLine("LINE")

            // Datos del cliente
            appendLine("LEFT:N° Documento: $dni")
            appendLine("LEFT:Cliente: $rs")
            appendLine("LINE")

            // Encabezado de la tabla con 5 columnas
            appendLine("TABLE: ${"#"}| ${"Jabas".padStart(4)}| ${"Pollos".padStart(4)}| ${"Peso".padStart(5)}| ${"Tipo".padStart(8)}")
            appendLine("LINE")

            // Datos de la tabla
            for (i in 0 until DETA_PESOPOLLOS.length()) {
                val item = DETA_PESOPOLLOS.optJSONObject(i) ?: JSONObject()
                appendLine("TABLE: ${(i + 1).toString()}| ${item.optString("cantJabas", "").padStart(6)}| ${item.optString("cantPollos", "").padStart(6)}| ${item.optString("peso", "").padStart(6).padEnd(6)}| ${item.optString("tipo", "").padStart(2)}")
            }

            // Línea final de la tabla
            appendLine("LINE")

            // Totales
            appendLine("TOTAL:T. Jabas:${totalJabas.padEnd(36)}")
            appendLine("TOTAL:T. Pollos:${totalPollos.padEnd(36)}")
            appendLine("TOTAL:Ps. Bruto:${pesoBruto.padEnd(36)}")
            appendLine("TOTAL:Tara:${tara.padEnd(36)}")
            appendLine("TOTAL:Neto:${neto.padEnd(36)}")
            appendLine("TOTAL:Precio/Kilo:${precio_kilo.padEnd(36)}")
            appendLine("TOTAL:Ps. Promedio:${pesoPromedio.padEnd(36)}")
            appendLine("TOTAL:T. PAGAR:${total_pagar.padEnd(36)}")
            appendLine("LINE")
            // Pie de página
            appendLine("CENTER:$nombre - $nomgal")
            appendLine("CENTER:¡GRACIAS POR SU COMPRA!")
        }

        // Obtener información de la impresora y enviar a imprimir
        var db = AppDatabase(context)
        val impresora = db.getImpresoraById("1")
        if (impresora != null) {
            val printerIp = impresora.ip
            val printerPort = impresora.puerto.toInt()

            val printerManager = PrinterManager(context, printerIp, printerPort)

            withContext(Dispatchers.IO) {
                try {
                    printerManager.printFormattedText(formattedText)
                    showPdfGeneratedNotification(context, file)
                } catch (e: Exception) {
                    showPdfGeneratedNotification(context, file)
                }
            }
        } else {
            withContext(Dispatchers.IO) {
                showPdfGeneratedNotification(context, file)
            }
        }

    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("PdfManager", "Error al generar o compartir el PDF: ${e.message}")
        Toast.makeText(context, "Error al generar o compartir el PDF", Toast.LENGTH_SHORT).show()
    }
}

private fun showPdfGeneratedNotification(context: Context, file: File) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "pdf_channel"
    val channelName = "PDF Notifications"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
            enableLights(true)
            enableVibration(true)
            setShowBadge(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
    }

    val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    val printIntent = Intent(context, FragmentPreliminar::class.java).apply {
        action = "PRINT_PDF"
        putExtra("PDF_PATH", file.absolutePath)
    }
    val printPendingIntent = PendingIntent.getActivity(context, 1, printIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.star_big_on)
        .setContentTitle("Nota de Venta generada correctamente")
        .setContentText("Toca para abrir la nota de venta ${file.name}")
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setSound(defaultSoundUri)
        .setVibrate(longArrayOf(1000, 1000, 1000, 1000))
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setFullScreenIntent(pendingIntent, true)
        .build()

    notificationManager.notify(1, notification)

    // Vibrar el dispositivo
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        val vibrator = vibratorManager.defaultVibrator
        vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(1000)
        }
    }

    sharePdfFile(context, file)
}

private fun sharePdfFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooserIntent = Intent.createChooser(shareIntent, "Compartir PDF con")

    if (shareIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(chooserIntent)
    } else {
        Toast.makeText(context, "No se encontró ninguna aplicación para compartir PDF", Toast.LENGTH_SHORT).show()
        Log.e("PDF Share", "No se encontró ninguna aplicación para compartir PDF")
    }
}

