package com.example.nfcautomation.utils

import android.content.Context
import java.io.File
import java.io.FileOutputStream

object ConfigManager {

    /**
     * Asegura que los archivos de configuración existan en el almacenamiento interno.
     * Si no existen, los copia desde Assets.
     * @return La ruta absoluta a la carpeta de configuración interna.
     */
    fun ensureConfigExists(context: Context): String {
        val configDir = File(context.filesDir, "config")
        val prefs = context.getSharedPreferences("config_prefs", Context.MODE_PRIVATE)
        // Forzamos actualización a v6 para asegurar que workflows.json incluya TrackTime
        val currentConfigVersion = "prod_v6_stable"
        val isConfigInitialized = prefs.getBoolean(currentConfigVersion, false)

        android.util.Log.i("ConfigManager", "Verificando configuración. Inicializada: $isConfigInitialized")
        
        if (!configDir.exists()) {
            configDir.mkdirs()
        }

        val filesToCopy = listOf("tags.json", "workflows.json", "states.json", "actions_template.json", "settings.json", "translations.json", "schedule_q1.json", "schedule_q2.json", "calendar_config.json", "time_logs.json", "campus_config.json")

        filesToCopy.forEach { fileName ->
            val targetFile = File(configDir, fileName)
            
            // REGLA DE PROTECCIÓN:
            // 1. Los recursos estáticos (traducciones, iconos, calendario oficial) se actualizan siempre.
            // 2. Los datos del usuario (horarios, etiquetas, logs) SOLO se copian si el archivo NO existe.
            val isStaticResource = fileName == "translations.json" || fileName == "actions_template.json" || fileName == "campus_config.json" || fileName == "calendar_config.json"
            
            if (!targetFile.exists() || isStaticResource) {
                try {
                    context.assets.open("config/$fileName").use { inputStream ->
                        FileOutputStream(targetFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    android.util.Log.d("ConfigManager", "Copiado/Actualizado: $fileName")
                } catch (e: Exception) {
                    android.util.Log.e("ConfigManager", "Error al copiar $fileName: ${e.message}")
                }
            }
        }

        // Marcar como inicializado con la nueva versión
        if (!isConfigInitialized) {
            prefs.edit().putBoolean(currentConfigVersion, true).apply()
        }

        return configDir.absolutePath
    }
}
