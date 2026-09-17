package com.example.nfcautomation.utils

import android.content.Context
import kotlinx.serialization.json.*
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ClassSession(
    val subject: String,
    val start: String,
    val end: String,
    val room: String = "-",
    val floor: String = "-",
    val building: String = "-",
    val type: String = "THEORY",
    val isException: Boolean = false,
    val isDeleted: Boolean = false
)

class ScheduleRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val configDir = File(context.filesDir, "config")

    fun getEffectiveSchedule(date: LocalDate): List<ClassSession> {
        val dateStr = date.toString()
        val exceptions = getExceptions()
        val hasException = exceptions.containsKey(dateStr)
        
        // 1. Si no es día lectivo y no hay una excepción manual, no hay clases
        if (!isAcademicDay(date) && !hasException) return emptyList()

        // 2. Intentar cargar el horario base del semestre (si existe)
        val semester = getSemesterForDate(date)
        val dayKey = getDayKey(date)
        val baseSchedule = if (semester != null) getBaseSchedule(semester) else JsonObject(emptyMap())
        val dayBase = baseSchedule[dayKey]?.jsonArray?.toList() ?: emptyList()

        val result = if (hasException) {
            val dayOverride = exceptions[dateStr]?.jsonArray?.toList() ?: emptyList()
            
            dayOverride.mapNotNull { overrideItem ->
                val itemObj = overrideItem.jsonObject
                
                // DETERMINAR SI ES EXCEPCIÓN REAL (Comparación profunda, tolerante y normalizada)
                var isRealException = true
                for (baseItem in dayBase) {
                    val b = baseItem.jsonObject
                    
                    // Normalización robusta (Minúsculas y limpieza de nulos)
                    fun getNorm(element: JsonElement?, default: String): String {
                        val content = element?.jsonPrimitive?.contentOrNull?.trim()?.lowercase()
                        return if (content == null || content == "null" || content == "") default.lowercase() else content
                    }

                    val sameSubject = getNorm(b["subject"], "") == getNorm(itemObj["subject"], "")
                    val sameStart = getNorm(b["start"], "") == getNorm(itemObj["start"], "")
                    val sameEnd = getNorm(b["end"], "") == getNorm(itemObj["end"], "")
                    val sameRoom = getNorm(b["room"], "-") == getNorm(itemObj["room"], "-")
                    val sameFloor = getNorm(b["floor"], "-") == getNorm(itemObj["floor"], "-")
                    val sameBuilding = getNorm(b["building"], "-") == getNorm(itemObj["building"], "-")
                    val sameType = getNorm(b["type"], "THEORY") == getNorm(itemObj["type"], "THEORY")

                    if (sameSubject && sameStart && sameEnd && sameRoom && sameFloor && sameBuilding && sameType) {
                        isRealException = false
                        break
                    }
                }
                
                // REGLA: Si es una modificación manual y está marcada como borrada, se elimina completamente (Hard Delete)
                if (isRealException && (itemObj["deleted"]?.jsonPrimitive?.booleanOrNull == true)) {
                    return@mapNotNull null
                }

                itemObj.toSession(isRealException)
            }
        } else {
            dayBase.map { it.jsonObject.toSession(false) }
        }

        return result.sortedBy { it.start }
    }

    private fun isAcademicDay(date: LocalDate): Boolean {
        val calendar = getCalendarConfig() ?: return true
        val dateStr = date.toString()

        // Verificar festivos
        val holidays = calendar["holidays"]?.jsonArray ?: JsonArray(emptyList())
        for (h in holidays) {
            if (h is JsonPrimitive && h.content == dateStr) return false
            if (h is JsonObject) {
                val start = LocalDate.parse(h["start"]?.jsonPrimitive?.content)
                val end = LocalDate.parse(h["end"]?.jsonPrimitive?.content)
                if (!date.isBefore(start) && !date.isAfter(end)) return false
            }
        }

        // Verificar si hay semestre activo
        return getSemesterForDate(date) != null
    }

    private fun getSemesterForDate(date: LocalDate): String? {
        val calendar = getCalendarConfig() ?: return null
        val semesters = calendar["semesters"]?.jsonObject ?: return null
        
        for ((qId, bounds) in semesters) {
            val start = LocalDate.parse(bounds.jsonObject["start"]?.jsonPrimitive?.content)
            val end = LocalDate.parse(bounds.jsonObject["end"]?.jsonPrimitive?.content)
            if (!date.isBefore(start) && !date.isAfter(end)) return qId
        }
        return null
    }

    private fun getCalendarConfig(): JsonObject? {
        val file = File(configDir, "calendar_config.json")
        return if (file.exists()) json.parseToJsonElement(file.readText()).jsonObject else null
    }

    private fun getExceptions(): Map<String, JsonElement> {
        val file = File(configDir, "schedule_exceptions.json")
        return if (file.exists()) json.parseToJsonElement(file.readText()).jsonObject else emptyMap()
    }

    private fun getBaseSchedule(semester: String): JsonObject {
        val file = File(configDir, "schedule_${semester}.json")
        return if (file.exists()) json.parseToJsonElement(file.readText()).jsonObject else JsonObject(emptyMap())
    }

    private fun getDayKey(date: LocalDate): String {
        return when (date.dayOfWeek.name.lowercase()) {
            "monday" -> "lunes"
            "tuesday" -> "martes"
            "wednesday" -> "miercoles"
            "thursday" -> "jueves"
            "friday" -> "viernes"
            "saturday" -> "sabado"
            "sunday" -> "domingo"
            else -> "lunes"
        }
    }

    private fun JsonObject.toSession(isException: Boolean): ClassSession {
        return ClassSession(
            subject = this["subject"]?.jsonPrimitive?.content ?: "Unknown",
            start = this["start"]?.jsonPrimitive?.content ?: "00:00",
            end = this["end"]?.jsonPrimitive?.content ?: "00:00",
            room = this["room"]?.jsonPrimitive?.content ?: "-",
            floor = this["floor"]?.jsonPrimitive?.content ?: "-",
            building = this["building"]?.jsonPrimitive?.content ?: "-",
            type = this["type"]?.jsonPrimitive?.content ?: "THEORY",
            isException = isException,
            isDeleted = this["deleted"]?.jsonPrimitive?.booleanOrNull ?: false
        )
    }
}
