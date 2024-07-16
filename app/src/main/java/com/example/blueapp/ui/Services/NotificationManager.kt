package com.example.blueapp.ui.Services
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.blueapp.R

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channelId = "DATA_TRANSFER_CHANNEL"
        val channelName = "Data Transfer"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channelDescription = "Notificaciones de progreso para la transferencia de datos"

        val notificationChannel = NotificationChannel(channelId, channelName, importance).apply {
            description = channelDescription
            enableVibration(true)
            enableLights(true)
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)
    }
}

fun createNotificationChannel2(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channelId = "DATA_SAVE_LOCAL"
        val channelName = "Data Transfer"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channelDescription = "Notificaciones de progreso para la transferencia de datos"

        val notificationChannel = NotificationChannel(channelId, channelName, importance).apply {
            description = channelDescription
            enableVibration(true)
            enableLights(true)
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)
    }
}

@SuppressLint("MissingPermission")
fun showNotification(context: Context, title: String, message: String) {
    createNotificationChannel(context)

    val channelId = "DATA_TRANSFER_CHANNEL"
    val notificationId = 2

    val soundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_info) // Cambiar por el ícono adecuado
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH) // Prioridad alta para heads-up
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Visibilidad pública para heads-up
        .setSound(soundUri) // Sonido de notificación
        .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000)) // Patrón de vibración (opcional)
        .setAutoCancel(true) // Cierra la notificación cuando el usuario la selecciona

    with(NotificationManagerCompat.from(context)) {
        notify(notificationId, builder.build())

        // Cerrar la notificación después de unos segundos
        val delayMillis = 5000L
        Thread {
            Thread.sleep(delayMillis)
            cancel(notificationId)
        }.start()
    }
}

@SuppressLint("MissingPermission")
fun showProgressNotification(context: Context, progress: Int, estimatedTimeLeft: String) {
    val builder = NotificationCompat.Builder(context, "DATA_TRANSFER_CHANNEL")
        .setSmallIcon(R.drawable.baseline_content_paste_go_24) // Usa un ícono apropiado para la notificación
        .setContentTitle("Transferencia de Datos")
        .setContentText("Progreso: $progress% - Tiempo restante: $estimatedTimeLeft")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setProgress(100, progress, false)
        .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000)) // Patrón de vibración (opcional)


    with(NotificationManagerCompat.from(context)) {
        notify(1, builder.build())
    }
}

@SuppressLint("MissingPermission")
fun showSuccessNotification(context: Context) {
    createNotificationChannel2(context)
    val builder = NotificationCompat.Builder(context, "DATA_SAVE_LOCAL")
        .setSmallIcon(R.drawable.outline_data_alert_24) // Icono de éxito
        .setContentTitle("Datos Guardados")
        .setContentText("Los datos han sido guardados localmente.")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setAutoCancel(true) // Cerrar la notificación al hacer clic en ella
        .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000)) // Patrón de vibración (opcional)

    with(NotificationManagerCompat.from(context)) {
        notify(3, builder.build())
    }
}
