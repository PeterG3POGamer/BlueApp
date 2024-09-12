package app.serlanventas.mobile.ui.login

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import app.serlanventas.mobile.R

class LogoutReceiver : BroadcastReceiver() {
    private lateinit var sharedPreferences: SharedPreferences

    override fun onReceive(context: Context, intent: Intent) {
        sharedPreferences.edit().clear().apply()
        showNotification(context)
    }

    private fun showNotification(context: Context) {
        val channelId = "logout_channel"
        val channelName = "Logout Notifications"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Sesión cerrada")
            .setContentText("Tu sesión ha sido cerrada debido a inactividad.")
            .setSmallIcon(R.drawable.ic_info) // Cambia esto por tu icono de notificación
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
