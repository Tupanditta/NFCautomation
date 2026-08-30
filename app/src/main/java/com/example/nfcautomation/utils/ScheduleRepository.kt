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
    val isException: Boolean = false
)

class ScheduleRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val configDir = File(context.filesDir, "config")

    fun getEffectiveSchedule(date: LocalDate): List<ClassSession> {
        // SIMULACIÓN: Si la fecha es hoy (2026), forzamos a Abril 2027 para la prueba del usuario
        val effectiveDate = if (date.year == 2026) LocalDate.parse("2027-04-12") else date
        val dateStr = effectiveDate.toString()
        
        // 1. Verificar si es festivo o fuera de semestre
        if (!isAcademicDay(effectiveDate)) return emptyList()

        // 2. Cargar horario base del semestre correspondiente
        val semester = getSemesterForDate(effectiveDate) ?: return emptyList()
        val baseSchedule = getBaseSchedule(semester)
        val dayKey = getDayKey(effectiveDate)
        val dayBase = baseSchedule[dayKey]?.jsonArray?.toList() ?: emptyList()

        // 3. Cargar excepciones
        val exceptions = getExceptions()
        
        return if (exceptions.containsKey(dateStr)) {
            val dayOverride = exceptions[dateStr]?.jsonArray?.toList() ?: emptyList()
            
            dayOverride.map { overrideItem ->
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
                itemObj.toSession(isRealException)
            }
        } else {
            dayBase.map { it.jsonObject.toSession(false) }
        }
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
            isException = isException
        )
    }
}
