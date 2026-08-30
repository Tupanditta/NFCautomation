package com.example.nfcautomation.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.nfcautomation.MainActivity

/**
 * Servicio que gestiona una única notificación dinámica.
 * Permite el acceso rápido a la app y ofrece un botón para desactivar 
 * el modo activo (Trabajo o Clase) de forma exclusiva.
 */
class QuickAccessService : Service() {

    companion object {
        private const val CHANNEL_ID = "nfc_automation_main"
        private const val NOTIFICATION_ID = 101

        const val EXTRA_ACTION = "NOTIFICATION_ACTION"
        const val ACTION_DEACTIVATE = "DEACTIVATE_ACTIVE_MODE"

        /**
         * Actualiza la notificación única según el modo activo.
         * @param activeModeName Nombre técnico del modo (work_mode, class_mode) o null.
         */
        @JvmStatic
        fun updateStatusNotification(context: Context, activeModeName: String?) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // 1. Intent para abrir la aplicación (Clic normal)
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val openPendingIntent = PendingIntent.getActivity(
                context, 0, openIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // 2. Intent para desactivar el modo (Botón de acción)
            val deactivateIntent = Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_ACTION, ACTION_DEACTIVATE)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val deactivatePendingIntent = PendingIntent.getActivity(
                context, 1, deactivateIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // 3. Construcción de la notificación camaleónica
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(openPendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            if (activeModeName != null) {
                // Adaptamos el texto según el modo que Python nos diga
                val readableName = when (activeModeName) {
                    "work_mode" -> "work mode"
                    "class_mode" -> "class mode"
                    else -> null // Si no es reconocido, lo tratamos como nulo (oculto)
                }
                
                if (readableName != null) {
                    builder.setContentTitle("$readableName on")
                           .setContentText("desactivate $readableName")
                           .addAction(android.R.drawable.ic_menu_close_clear_cancel, "DESACTIVAR", deactivatePendingIntent)
                } else {
                    // Si el modo no es reconocido, mostramos estado base
                    builder.setContentTitle("NFC Automation Activo")
                           .setContentText("Tocar para abrir la aplicación")
                }
            } else {
                // Estado base: nada activo
                builder.setContentTitle("NFC Automation Activo")
                       .setContentText("Tocar para abrir la aplicación")
            }

            manager.notify(NOTIFICATION_ID, builder.build())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        
        // Iniciamos como Foreground Service atado al ID 101
        val dummyIntent = Intent(this, MainActivity::class.java)
        val dummyPendingIntent = PendingIntent.getActivity(this, 0, dummyIntent, PendingIntent.FLAG_IMMUTABLE)
        val initialNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NFC Automation")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(dummyPendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, initialNotification)
        
        // Pedimos a Python que sincronice el estado real inmediatamente
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Control Principal NFC",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
