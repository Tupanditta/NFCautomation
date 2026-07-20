# Walkthrough: Solución de Visibilidad en Pantalla de Bloqueo

Se ha optimizado la notificación persistente para garantizar que sea visible en la pantalla de bloqueo, cumpliendo con las estrictas políticas de notificaciones de Android 14 y Xiaomi HyperOS.

## Cambios Realizados

### 1. Elevación de Importancia del Canal
He actualizado el servicio para utilizar un nuevo canal de notificación con mayor prioridad:
- **Cambio Técnico**: He pasado de `IMPORTANCE_LOW` (que Android marca como "Silenciosa") a **`IMPORTANCE_DEFAULT`**.
- **Impacto**: Las notificaciones silenciosas suelen ocultarse de la pantalla de bloqueo por defecto en Xiaomi. Al subir la importancia, el sistema ahora la trata como una alerta activa que merece visibilidad inmediata.

### 2. Nuevo Identificador de Canal
- **Razón**: Android no permite cambiar la importancia de un canal una vez que el usuario lo ha visto.
- **Solución**: He creado un nuevo canal llamado `"Acceso Rápido NFC"`. Esto fuerza al sistema a aplicar los nuevos ajustes de visibilidad.

---

## ⚠️ Instrucciones para la Verificación

1.  Instala la actualización y abre la app.
2.  Bloquea el móvil y enciende la pantalla. **Ahora el mensaje debería aparecer.**
3.  Si por algún motivo sigue sin aparecer en tu Xiaomi, sigue este último paso manual:
    - Ve a **Ajustes > Aplicaciones > Gestionar aplicaciones > NFC Automation**.
    - Entra en **Notificaciones**.
    - Busca la categoría **Acceso Rápido NFC**.
    - Asegúrate de que el interruptor **"Notificaciones en la pantalla de bloqueo"** esté activado y configurado como "Mostrar todo el contenido".

> [!TIP]
> ¡Ahora ya tienes tu botón de acceso siempre a mano, incluso antes de poner la huella!
