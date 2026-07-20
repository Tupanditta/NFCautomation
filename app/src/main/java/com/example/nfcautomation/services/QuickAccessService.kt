package com.example.nfcautomation.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.nfcautomation.MainActivity

/**
 * Servicio en primer plano que mantiene una notificación persistente 
 * para el acceso rápido a la aplicación desde cualquier pantalla o 
 * desde la pantalla de bloqueo.
 */
class QuickAccessService : Service() {

    companion object {
        // Cambiamos el ID del canal porque Android no permite cambiar la importancia 
        // de un canal una vez ha sido creado.
        private const val CHANNEL_ID = "nfc_automation_quick_access"
        private const val NOTIFICATION_ID = 101
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NFC Automation Activo")
            .setContentText("Tocar para abrir la aplicación")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            // Elevamos la prioridad e importancia para garantizar visibilidad en pantalla de bloqueo
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Acceso Rápido NFC",
                // IMPORTANCE_DEFAULT asegura que no se considere "Silenciosa" 
                // y aparezca en la pantalla de bloqueo.
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
