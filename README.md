# 📱 NFC Automation

![Android](https://img.shields.io/badge/Platform-Android-brightgreen.svg)
![Python](https://img.shields.io/badge/Language-Python%20%2F%20Kotlin-blue.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

**NFC Automation** es una herramienta de automatización avanzada para Android que permite ejecutar flujos de trabajo complejos mediante el uso de etiquetas NFC. El proyecto integra un motor de lógica en **Python** dentro de una arquitectura nativa en **Kotlin**, permitiendo una extensibilidad sin precedentes.

---

## 🏗️ Arquitectura del Sistema

*   **Capa de Interfaz y Hardware (Kotlin/Compose)**: Gestiona la detección física de etiquetas NFC, el ciclo de vida de la aplicación y el acceso rápido desde la pantalla de bloqueo.
*   **Motor de Lógica (Python/Chaquopy)**: El núcleo del sistema que procesa configuraciones, valida datos y coordina la ejecución de acciones modulares.
*   **Configuración Declarativa (JSON)**: Permite definir comportamientos complejos sin necesidad de modificar el código fuente de la aplicación.

---

## 🚀 Acciones Implementadas

El motor cuenta con una librería de acciones modulares listas para usar:

### 🛠️ Control de Dispositivo
- **Volumen Modular (`set_volume`)**: Ajusta canales específicos (`music`, `notification`, `ring`, `alarm`).
- **No Molestar (`enable_dnd`)**: Activa/desactiva el modo silencio con auto-guía de permisos.
- **Temporizador (`set_timer`)**: Configuración inmediata de cuentas atrás.
- **Linterna (`toggle_flashlight`)**: Control directo del LED.
- **Punto de Acceso (`toggle_hotspot`)**: Acceso directo a la configuración de Hotspot.

### 🌐 Conectividad y Utilidades
- **Apertura de Apps (`open_app`)**: Uso de alias amigables como `spotify` o `youtube`.
- **Navegación URL (`open_url`)**: Soporte para `https://`, `geo:` y `tel:`.
- **Portapapeles (`text_clipboard`)**: Copiado automático de cadenas de texto.
- **Logs de Eventos (`log_event`)**: Registro de hitos de ejecución.

### 📳 Respuesta Háptica
- **Vibración Simple (`vibrate`)**: Pulsos rápidos de confirmación.
- **Vibración Continua (`activate_vibration`)**: Patrones rítmicos personalizables con duración total definida.
- **Detener Vibración (`desactivate_vibration`)**: Limpieza inmediata de efectos activos.

---

## ✨ Características Principales

*   **Acceso Rápido Persistente**: Notificación de baja prioridad pero permanente en la pantalla de bloqueo.
*   **Ejecución Resiliente**: Arquitectura de errores que garantiza la continuidad del flujo aunque falle una acción.
*   **Prioridad NFC Optimizada**: Filtros configurados con prioridad máxima (1000) para minimizar diálogos del sistema.
*   **Adaptación HyperOS**: Lógica diseñada específicamente para las restricciones de energía y permisos de Xiaomi.

---

## 📂 Estructura de Configuración

Toda la lógica reside en `app/src/main/python/mobile/config/`:

| Archivo | Función |
| :--- | :--- |
| `tags.json` | Mapeo de UIDs físicos a nombres de Workflows. |
| `workflows.json` | Lista secuencial de acciones a ejecutar. |
| `actions_template.json` | Guía de referencia para parámetros y sintaxis. |

---

## ⚠️ Requisitos Críticos (Xiaomi/HyperOS)

> [!IMPORTANT]
> Para el correcto funcionamiento de la detección automática y el control de hardware, se deben conceder manualmente los siguientes permisos en **Info. de la aplicación > Otros permisos**:
> - **Modificar ajustes del sistema** (Obligatorio para el modo DND).
> - **Mostrar ventanas emergentes mientras se ejecuta en segundo plano** (Obligatorio para el auto-abierto).
> - **Inicio automático** (Para que el NFC pueda despertar a la app).
> - **Ahorro de batería > Sin restricciones**.

---

## 🛠️ Tecnologías Utilizadas

- **Kotlin** & **Jetpack Compose** para la interfaz moderna.
- **Python 3.11** para la lógica de negocio.
- **Chaquopy** como puente de integración.
- **Material 3** para el diseño visual.

---
© 2026 NFC Automation Project
