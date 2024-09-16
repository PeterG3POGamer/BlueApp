package app.serlanventas.mobile.ui.login

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import app.serlanventas.mobile.LoginActivity
import app.serlanventas.mobile.R

class LogoutReceiver : BroadcastReceiver() {
    private lateinit var sharedPreferences: SharedPreferences

    override fun onReceive(context: Context, intent: Intent) {
        sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
        sharedPreferences.edit().putBoolean("isLoggedIn", false).apply()
        sharedPreferences.edit().putBoolean("isProduction", true).apply()
        showNotification(context)
    }

    private fun showNotification(context: Context) {
        val channelId = "logout_channel"
        val channelName = "Logout Notifications"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notificaciones relacionadas con el cierre de sesión"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(100, 200, 300, 400, 500)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Crear un PendingIntent para abrir la actividad de login
        val intent = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Sesión cerrada")
            .setContentText("Tu sesión ha sido cerrada debido a inactividad.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Tu sesión ha sido cerrada debido a inactividad. Toca aquí para iniciar sesión nuevamente y continuar usando la aplicación."))
            .setSmallIcon(R.drawable.ic_info) // Cambia esto por tu icono de notificación
            .setColor(Color.RED)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(100, 200, 300, 400, 500))
            .setLights(Color.RED, 3000, 3000)
            .build()

        notificationManager.notify(1, notification)
    }
}
