# Plan de Implementación: Captura Modular de Ejecución

Este plan detalla la creación de un sistema modular para capturar los logs del motor Python y devolverlos a Android, permitiendo ver el detalle de la ejecución sin modificar el motor de lógica original.

## Cambios Propuestos

### [Component Name] Utilidades Python

#### [NEW] [log_capturer.py](file:///C:/Users/ander/Desktop/Kirby/NFCAutomation/app/src/main/python/mobile/utils/log_capturer.py)
Crearemos un nuevo módulo de utilidad para encapsular la lógica de captura:
- **Clase `LogCapturer`**: Funcionará como un *Context Manager* (usando `with`) para asegurar que la captura comience y termine limpiamente.
- **Intercepción**: Capturará los mensajes de log de nivel `INFO` o superior generados durante la ejecución.

#### [NEW] [__init__.py](file:///C:/Users/ander/Desktop/Kirby/NFCAutomation/app/src/main/python/mobile/utils/__init__.py)
- Archivo necesario para que la nueva carpeta `utils` sea tratada como un paquete Python.

### [Component Name] Puente Python (Android Interface)

#### [MODIFY] [bridge.py](file:///C:/Users/ander/Desktop/Kirby/NFCAutomation/app/src/main/python/bridge.py)
- Importar y utilizar la nueva clase `LogCapturer`.
- Envolver la ejecución del `Dispatcher` dentro del bloque `with LogCapturer(...)`.
- Formatear la respuesta final combinando el éxito de la operación con los detalles capturados del log.

## Verificación Plan

### Manual Verification
- Leer una etiqueta NFC real o simular el UID en los archivos JSON.
- Verificar que la UI de Android muestra no solo el mensaje de éxito, sino también la lista de acciones realizadas (ej: "Vibrando...", "Abriendo URL...").

---
> [!NOTE]
> Todos los nuevos archivos tendrán **docstrings en castellano** y código técnico en **inglés**, manteniendo la consistencia solicitada.
