# Plan de Implementación: Visibilidad en Pantalla de Bloqueo

Este plan corrige la ausencia de la notificación persistente en la pantalla de bloqueo, ajustando los niveles de prioridad e importancia para cumplir con las políticas de filtrado de **Xiaomi HyperOS** y **Android 14**.

## User Review Required

> [!IMPORTANT]
> - **Cambio de Canal**: Se creará un nuevo canal de notificación interno. Es posible que veas una breve duplicidad o que debas volver a aceptar el permiso si el sistema lo requiere.
> - **Nivel de Alerta**: Al subir la importancia a `DEFAULT`, la notificación ya no se agrupará en la sección "Silenciosas" de Android, garantizando su presencia en la pantalla de bloqueo.

## Proposed Changes

### Motor de Notificaciones (Kotlin)

#### [MODIFY] [QuickAccessService.kt](file:///C:/Users/ander/Desktop/Kirby/NFCAutomation/app/src/main/java/com/example/nfcautomation/services/QuickAccessService.kt)
- Cambiar `CHANNEL_ID` a `"nfc_automation_quick_access"`.
- Cambiar `NotificationManager.IMPORTANCE_LOW` por `NotificationManager.IMPORTANCE_DEFAULT`.
- Cambiar `NotificationCompat.PRIORITY_LOW` por `NotificationCompat.PRIORITY_DEFAULT`.

## Verification Plan

### Manual Verification
1.  **Visibilidad**: Bloquear el dispositivo y encender la pantalla. La notificación debe aparecer en el listado principal.
2.  **Interacción**: Tocar la notificación en la pantalla de bloqueo y verificar que solicita el desbloqueo para abrir la app.
3.  **Ajustes de Xiaomi**: Verificar en **Ajustes > Aplicaciones > NFC Automation > Notificaciones** que el permiso de **"Pantalla de bloqueo"** aparece como activo.
