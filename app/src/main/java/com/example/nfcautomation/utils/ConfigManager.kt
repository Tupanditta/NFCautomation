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
        android.util.Log.i("ConfigManager", "Asegurando configuración en: ${configDir.absolutePath}")
        
        if (!configDir.exists()) {
            val created = configDir.mkdirs()
            android.util.Log.i("ConfigManager", "Directorio creado: $created")
        }

        val filesToCopy = listOf("tags.json", "workflows.json", "states.json", "actions_template.json", "settings.json", "translations.json", "schedule_q1.json", "schedule_q2.json", "calendar_config.json", "time_logs.json")

        filesToCopy.forEach { fileName ->
            val targetFile = File(configDir, fileName)
            android.util.Log.i("ConfigManager", "Verificando archivo: $fileName (Existe: ${targetFile.exists()})")
            
            // SIMULACIÓN: Forzamos la actualización de horarios y logs para la prueba de año completo
            val isSimulationFile = fileName.startsWith("schedule_q") || fileName == "calendar_config.json" || fileName == "time_logs.json"
            val forceUpdate = fileName == "translations.json" || fileName == "actions_template.json" || isSimulationFile
            
            if (!targetFile.exists() || forceUpdate) {
                try {
                    android.util.Log.i("ConfigManager", "Copiando $fileName desde assets...")
                    context.assets.open("config/$fileName").use { inputStream ->
                        FileOutputStream(targetFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    android.util.Log.i("ConfigManager", "Copia de $fileName exitosa.")
                } catch (e: Exception) {
                    android.util.Log.e("ConfigManager", "Error al copiar $fileName: ${e.message}")
                    e.printStackTrace()
                }
            }
        }

        return configDir.absolutePath
    }
}
