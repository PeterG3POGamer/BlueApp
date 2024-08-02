package com.example.blueapp.ui.Services

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Logger(private val context: Context) {
    private val logFile: File = File(context.getExternalFilesDir(null), "bluetooth_service_log.txt")
    private val logFile2: File = File(context.getExternalFilesDir(null), "pesos_log.txt")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    init {
        if (!logFile.exists()) {
            logFile.createNewFile()
        }
        if (!logFile2.exists()) {
            logFile2.createNewFile()
        }
    }

    fun log(message: String, throwable: Throwable? = null) {
        val timestamp = dateFormat.format(Date())
        val logMessage = "$timestamp: $message"

        if (throwable != null) {
            Log.e("BluetoothServices", logMessage, throwable)
        } else {
            Log.d("BluetoothServices", logMessage)
        }

        try {
            logFile.appendText("$logMessage\n")
            if (throwable != null) {
                logFile.appendText("${throwable.stackTraceToString()}\n")
            }
        } catch (e: IOException) {
            Log.e("BluetoothServices", "Error al escribir en el archivo de log", e)
        }
    }

    fun log2(message: String, throwable: Throwable? = null) {
        val timestamp = dateFormat.format(Date())
        val logMessage = "$timestamp: $message"

        if (throwable != null) {
            Log.e("PesosFragment", logMessage, throwable)
        } else {
            Log.d("PesosFragment", logMessage)
        }

        try {
            logFile2.appendText("$logMessage\n")
            if (throwable != null) {
                logFile2.appendText("${throwable.stackTraceToString()}\n")
            }
        } catch (e: IOException) {
            Log.e("PesosFragment", "Error al escribir en el archivo de log", e)
        }
    }

}