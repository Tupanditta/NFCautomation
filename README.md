# 📱 NFC Automation & Attendance Tracker

![Android](https://img.shields.io/badge/Platform-Android-brightgreen.svg)
![Python](https://img.shields.io/badge/Language-Python%20%2F%20Kotlin-blue.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

**NFC Automation** es una herramienta de productividad avanzada para Android que combina la potencia de las etiquetas NFC con un sistema inteligente de gestión de asistencia académica. El proyecto integra un motor de lógica en **Python** dentro de una arquitectura nativa en **Kotlin**, permitiendo una automatización y un seguimiento de datos sin precedentes.

---

## 🚀 Características Principales

### 🎯 Automatización NFC
- **Flujos de Trabajo Complejos**: Ejecuta múltiples acciones con un solo toque (vibración, volumen, DND, temporizadores, etc.).
- **Modos de Operación**: Soporte para modos "Toggle" (ON/OFF) vinculados a una sola etiqueta.
- **Acceso Rápido**: Notificación persistente para ejecución instantánea desde la pantalla de bloqueo.
- **Detección Resiliente**: Prioridad máxima en el filtrado de NFC (1000) para evitar diálogos del sistema.

### 🎓 Control de Asistencia Académica
- **Calendario Inteligente**: Gestión automática de cuatrimestres (Q1/Q2), festivos y periodos de exámenes.
- **Horarios Flexibles**: Soporte para clases de Teoría (T) y Práctica (P) con detalles de aula, piso y edificio.
- **Gestión de Excepciones**: Modifica el horario de un día específico sin alterar el calendario base (útil para cambios puntuales o recuperaciones).
- **Estadísticas en Tiempo Real**: Seguimiento visual de clases asistidas, perdidas y próximas, con cálculo automático de porcentajes.
- **Exportación de Datos**: Generación de informes en formato JSON y PDF para auditoría personal.

---

## 🏗️ Arquitectura del Sistema

*   **Capa de Interfaz (Kotlin/Compose)**: UI moderna con Material 3, widgets para la pantalla de inicio (Glance) y gestión de hardware.
*   **Motor de Lógica (Python/Chaquopy)**: El núcleo del sistema que procesa configuraciones, gestiona el calendario académico y coordina las acciones.
*   **Almacenamiento Local (JSON)**: Base de datos ligera y editable para configuraciones, logs y horarios.

---

## 🛠️ Acciones de Automatización

| Acción | Descripción | Parámetros |
| :--- | :--- | :--- |
| `set_volume` | Ajusta el volumen del sistema | `stream`, `level` |
| `enable_dnd` | Controla el modo "No Molestar" | `enabled` |
| `track_time` | Registra eventos de tiempo | `mode`, `event`, `details` |
| `toggle_flashlight` | Controla el LED de la cámara | `enabled` |
| `set_timer` | Inicia una cuenta atrás | `duration_seconds` |
| `vibrate` | Feedback háptico inmediato | `duration_ms` |
| `open_app` | Lanza aplicaciones instaladas | `alias` |

---

## 📂 Estructura de Configuración

La lógica y los datos residen en `app/src/main/assets/config/` y se sincronizan con el motor Python:

- `calendar_config.json`: Definición de semestres y festivos.
- `schedule_q1.json` / `schedule_q2.json`: Horarios base por cuatrimestre.
- `workflows.json`: Definición secuencial de automatizaciones.
- `time_logs.json`: Registro histórico de asistencias y eventos.

---

## ⚠️ Requisitos de Instalación (Xiaomi/HyperOS)

> [!IMPORTANT]
> Para garantizar el funcionamiento en segundo plano y el control de hardware, conceda estos permisos en **Ajustes > Aplicaciones > NFC Automation > Otros permisos**:
> - **Modificar ajustes del sistema** (Para DND/Volumen).
> - **Mostrar ventanas emergentes en segundo plano** (Para auto-abierto).
> - **Inicio automático**.
> - **Ahorro de batería > Sin restricciones**.

---

## 🛠️ Stack Tecnológico

- **Android SDK** (API 34+)
- **Jetpack Compose** (UI declarativa)
- **Chaquopy 15.0** (Python SDK para Android)
- **Python 3.11** (Lógica de negocio y procesamiento de datos)
- **Jetpack Glance** (App Widgets)

---
© 2026 NFC Automation Project - Desarrollado por TUPANDITTA
