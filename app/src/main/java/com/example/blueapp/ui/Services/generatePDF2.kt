package com.example.blueapp.ui.Services

import PrinterManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.example.blueapp.ui.DataBase.AppDatabase
import com.example.blueapp.ui.preliminar.FragmentPreliminar
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.DottedBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.property.HorizontalAlignment
import com.itextpdf.layout.property.TextAlignment
import com.itextpdf.layout.property.UnitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

suspend fun generateAndOpenPDF2(jsonDataPdf: JSONObject, context: Context) {
    try {
        val PESO_POLLO = jsonDataPdf.optJSONArray("PESO_POLLO") ?: JSONArray()
        val CLIENTE = jsonDataPdf.optJSONArray("CLIENTE") ?: JSONArray()
        val GALPON = jsonDataPdf.optJSONArray("GALPON") ?: JSONArray()
        val ESTABLECIMIENTO = jsonDataPdf.optJSONArray("ESTABLECIMIENTO") ?: JSONArray()
        val EMPRESA = jsonDataPdf.optJSONArray("EMPRESA") ?: JSONArray()
        val DETA_PESOPOLLOS = jsonDataPdf.optJSONArray("DETA_PESOPOLLO") ?: JSONArray()

        val nroRuc = EMPRESA.optJSONObject(0)?.optString("nroRuc", "N/A") ?: "N/A"
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
        val total_pagar = PESO_POLLO.optJSONObject(0)?.optString("total_pagar", "N/A") ?: "N/A"

        val fileName = "$nroRuc-$serie.pdf"
        val directory = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "PDFs")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = File(directory, fileName)

        // Establecer el ancho fijo en puntos
        val widthInPoints = 58f * 72 / 25.4f

        // Calcular la altura necesaria en función del número de elementos en la tabla de detalles
        val baseHeightInPoints = 120f * 72 / 25.4f // Altura base en puntos, ajusta esto según sea necesario
        val itemHeight = 10f // Altura estimada por fila de la tabla
        val additionalHeight = DETA_PESOPOLLOS.length() * itemHeight // Altura adicional basada en el número de filas
        val totalHeightInPoints = baseHeightInPoints + additionalHeight

        val pdfWriter = PdfWriter(file)
        val pdfDocument = PdfDocument(pdfWriter)
        pdfDocument.setDefaultPageSize(PageSize(widthInPoints, totalHeightInPoints))

        val document = Document(pdfDocument)
        document.setMargins(2f, 0f, 2f, 0f)

        // Cambiar la fuente a una más apropiada para tickets
        val font = PdfFontFactory.createFont(StandardFonts.COURIER)
        val boldFont = PdfFontFactory.createFont(StandardFonts.COURIER_BOLD)
        val titleFontSize = 10f
        val normalFontSize = 7f
        val smallFontSize = 6f
        val smallFontSize2 = 5f

        // Título
        document.add(Paragraph("NOTA DE VENTA")
            .setFont(boldFont)
            .setFontSize(titleFontSize)
            .setTextAlignment(TextAlignment.CENTER))

        // Información de la empresa
        document.add(Paragraph("RUC: $nroRuc")
            .setFont(font)
            .setFontSize(normalFontSize)
            .setTextAlignment(TextAlignment.CENTER))

        // Información general
        document.add(Paragraph("Serie: $serie   Fecha: $fecha")
            .setFont(font)
            .setFontSize(smallFontSize)
            .setTextAlignment(TextAlignment.CENTER))

        document.add(Paragraph("-".repeat(45))
            .setFont(font)
            .setFontSize(smallFontSize)
            .setTextAlignment(TextAlignment.CENTER))
            .setMargins(0f, 0f,0f,0f)

        // Información del cliente
        document.add(
            Paragraph().add(
                Text("N° Documento: ").setFont(boldFont).setFontSize(normalFontSize)
            ).add(
                Text(dni).setFont(font).setFontSize(normalFontSize)
            )
                .setMargins(0f, 0f,0f,0f)
        )

        document.add(
            Paragraph().add(
                Text("Cliente: ").setFont(boldFont).setFontSize(normalFontSize)
            ).add(
                Text(rs).setFont(font).setFontSize(normalFontSize)
            )
                .setMargins(0f, 0f,0f,0f)
        )

        document.add(Paragraph("-".repeat(45))
            .setFont(font)
            .setFontSize(smallFontSize)
            .setTextAlignment(TextAlignment.CENTER))
            .setMargins(0f, 0f,0f,0f)

        // Crear una tabla sin líneas y bordes visibles
        val detailsTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f, 1f, 1f, 3f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setHorizontalAlignment(HorizontalAlignment.CENTER)
            .setBorderBottom(DottedBorder(1f))

        arrayOf("#", "N° Jabas", "Pollos", "Peso", "Tipo").forEachIndexed { index, header ->
            val cell = Cell().add(Paragraph(header)
                .setFont(boldFont)
                .setFontSize(smallFontSize2))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.CENTER)
            cell.setBorderBottom(DottedBorder(1f))
            detailsTable.addHeaderCell(cell)
        }

        for (i in 0 until DETA_PESOPOLLOS.length()) {
            val item = DETA_PESOPOLLOS.optJSONObject(i) ?: JSONObject()
            detailsTable.addCell(Cell().add(Paragraph((i + 1).toString())
                .setFont(font)
                .setFontSize(smallFontSize2))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.CENTER)
                .setMargins(1f,1f,1f,1f)
                .setPadding(4f))
            arrayOf(
                item.optString("cantJabas", ""),
                item.optString("cantPollos", ""),
                item.optString("peso", ""),
                item.optString("tipo", "")
            ).forEach {
                detailsTable.addCell(Cell().add(Paragraph(it)
                    .setFont(font)
                    .setFontSize(smallFontSize2))
                    .setBorder(Border.NO_BORDER)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMargins(1f,1f,1f,1f)
                    .setPadding(4f))
            }
        }

        document.add(detailsTable)

        // Tabla de totales al lado derecho
        val totalsTable = Table(UnitValue.createPercentArray(floatArrayOf(60f, 40f)))
            .setWidth(UnitValue.createPercentValue(50f))
            .setHorizontalAlignment(HorizontalAlignment.RIGHT)

        fun addTotalRow(label: String, value: String, isBold: Boolean = false, isLast: Boolean = false) {
            val labelCell = Cell().add(Paragraph(label)
                .setFont(if (isBold) boldFont else font)
                .setFontSize(smallFontSize))
                .setBorderRight(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.LEFT)
                .setPaddingRight(0f)

            val valueCell = Cell().add(Paragraph(value)
                .setFont(if (isBold) boldFont else font)
                .setFontSize(smallFontSize))
                .setBorderLeft(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .setPaddingLeft(0f)

            if (!isLast) {
                labelCell.setBorderBottom(Border.NO_BORDER)
                valueCell.setBorderBottom(Border.NO_BORDER)
            }

            totalsTable.addCell(labelCell)
            totalsTable.addCell(valueCell)
        }

        addTotalRow("T. Jabas:", totalJabas)
        addTotalRow("T. Pollos:", totalPollos)
        addTotalRow("Ps. Bruto:", pesoBruto)
        addTotalRow("Tara:", tara)
        addTotalRow("Neto:", neto)
        addTotalRow("Precio/Kilo:", precio_kilo)
        addTotalRow("T. Pagar:", total_pagar, true, true)

        document.add(totalsTable)

        // Pie de página
        document.add(Paragraph("$nombre - $nomgal")
            .setFont(font)
            .setFontSize(smallFontSize2)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(5f))

        document.close()

        Log.d("PdfManager", "PDF creado en: ${file.absolutePath}")

        val formattedText = buildString {
            // Encabezado
            appendLine("TITLE:NOTA DE VENTA")
            appendLine("CENTER:RUC: $nroRuc")
            appendLine("CENTER:Serie: $serie   Fecha: $fecha")
            appendLine("LINE")

            // Datos del cliente
            appendLine("LEFT:N° Documento: $dni")
            appendLine("LEFT:Cliente: $rs")
            appendLine("LINE")

            // Encabezado de la tabla con 5 columnas
            appendLine("TABLE: ${"#"}| ${"N° Jabas".padStart(4)}| ${"N° Pollos".padStart(4)}| ${"Peso".padStart(5)}| ${"Tipo".padStart(8)}")
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
            appendLine("TOTAL:T. Pagar:${total_pagar.padEnd(36)}")
            appendLine("LINE")
            // Pie de página
            appendLine("CENTER:$nombre - $nomgal")
        }

        var db = AppDatabase(context)
        val impresora = db.getImpresoraById("1")
        if (impresora != null) {
            val printerIp = impresora.ip
            val printerPort = impresora.puerto.toInt() // Asegúrate de convertir el puerto a Int

            // Crear instancia de PrinterManager con los valores obtenidos
            val printerManager = PrinterManager(context, printerIp, printerPort)

            // Llamar a la función para imprimir el texto formateado
            withContext(Dispatchers.IO) {
                try {
                    printerManager.printFormattedText(formattedText)
                    // Mostrar notificación después de imprimir
                    showPdfGeneratedNotification(context, file)
                } catch (e: Exception) {
                    showPdfGeneratedNotification(context, file)
                }
            }
        }else{
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
        .setFullScreenIntent(pendingIntent, true) // Esto fuerza la notificación a aparecer como heads-up

//        .addAction(android.R.drawable.ic_menu_send, "Imprimir", printPendingIntent)
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