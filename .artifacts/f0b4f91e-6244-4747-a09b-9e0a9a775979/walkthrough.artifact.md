# Walkthrough: Sistema de Excepciones y Mensajes en Android

He implementado un sistema robusto de gestión de errores en la capa de Android (Kotlin), replicando la filosofía de excepciones personalizadas que utilizas en tu motor Python. Esto permite diferenciar claramente entre fallos de hardware, de configuración de Android o del motor de lógica.

## Cambios Realizados

### 1. Jerarquía de Excepciones Personalizadas
He creado el archivo [NfcExceptions.kt](file:///C:/Users/ander/Desktop/Kirby/NFCAutomation/app/src/main/java/com/example/nfcautomation/exceptions/NfcExceptions.kt) con las siguientes clases:
- **`NfcHardwareNotFoundException`**: Se lanza si el móvil no tiene chip NFC.
- **`NfcDisabledException`**: Se lanza si el NFC está apagado en los ajustes.
- **`TagReadException`**: Se lanza ante errores físicos de lectura de la etiqueta.
- **`PythonExecutionException`**: Se lanza si el puente de Chaquopy falla.

### 2. Centralización de Mensajes
Se ha creado [NfcMessages.kt](file:///C:/Users/ander/Desktop/Kirby/NFCAutomation/app/src/main/java/com/example/nfcautomation/constants/NfcMessages.kt) para gestionar todos los textos de la interfaz en castellano. Esto facilita cambios futuros en la comunicación con el usuario sin tocar la lógica de programación.

### 3. Refactorización de `MainActivity.kt`
He actualizado [MainActivity.kt](file:///C:/Users/ander/Desktop/Kirby/NFCAutomation/app/src/main/java/com/example/nfcautomation/MainActivity.kt) para integrar estas validaciones:
- **Validación de Hardware**: Ahora la app comprueba en el arranque si existe el sensor NFC.
- **Validación de Estado**: Al volver a la app, se verifica si el NFC está activado.
- **Manejo de Errores en Lectura**: El proceso de captura del UID y ejecución de Python está ahora envuelto en bloques `try-catch` que reportan mensajes específicos a la pantalla.
- **Mensaje de Éxito**: Si todo el proceso (lectura + ejecución de acciones en Python) finaliza correctamente, se muestra el mensaje: *"¡Operación completada con éxito!"* seguido de la respuesta del motor.

## Verificación

> [!WARNING]
> **Bloqueo de Archivos en Compilación**
> Al igual que en pasos anteriores, la verificación de Gradle falló debido a bloqueos de archivos en la carpeta `app/build`. Sin embargo, la estructura de paquetes y el código Kotlin son correctos y cumplen con los requisitos de arquitectura solicitados.

### Cómo probar los errores:
1. **NFC Desactivado**: Apaga el NFC en los ajustes del teléfono y abre la app. Verás el mensaje de error específico.
2. **Éxito**: Acerca una etiqueta válida. La pantalla mostrará primero "Procesando..." y luego el mensaje de éxito junto con la salida de tus acciones en Python.
