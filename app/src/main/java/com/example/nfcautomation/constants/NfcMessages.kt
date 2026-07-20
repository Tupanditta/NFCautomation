package com.example.nfcautomation.constants

/**
 * Objeto que centraliza todos los mensajes de usuario relacionados 
 * con la gestión del NFC y Python en Android.
 */
object NfcMessages {
    const val WAITING_TAG = "Esperando etiqueta NFC..."
    const val TAG_DETECTED = "Tag detectado: "
    const val PROCESSING = "Procesando..."
    const val SUCCESS = "¡Lectura y vinculación con el tag NFC exitosa!"
    
    // Mensajes de Error
    const val ERR_HARDWARE_NOT_FOUND = "Error: Este dispositivo no dispone de hardware NFC."
    const val ERR_NFC_DISABLED = "Error: El NFC está desactivado. Por favor, actívalo en los ajustes."
    const val ERR_TAG_READ = "Error: No se pudo leer el ID de la etiqueta."
    const val ERR_PYTHON_EXECUTION = "Error en el motor de lógica (Python): "
    const val ERR_UNEXPECTED = "Error inesperado: "
}
