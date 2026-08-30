package com.example.nfcautomation.ui.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import android.app.Application
import com.chaquo.python.Python
import kotlinx.serialization.json.*
import kotlinx.serialization.Serializable
import com.example.nfcautomation.utils.WidgetUtils
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek

@Serializable
data class AttendanceInfo(
    val subject: String,
    val date: String
)

@Serializable
data class ExecutionResult(
    val target: String? = null,
    val details: String? = null,
    val errors: List<String> = emptyList(),
    val summarized_actions: List<String> = emptyList(),
    val attendance_info: AttendanceInfo? = null,
    val active_mode: String? = null,
    val is_toggle: Boolean = false,
    val toggle_type: String? = null, // "ON" o "OFF"
    val critical_error: String? = null
)

enum class Screen {
    MENU, EXECUTION, MANAGEMENT, ATTENDANCE, SCHEDULE_EDITOR
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    var currentScreen by mutableStateOf(Screen.MENU)
    var activeMode by mutableStateOf<String?>(null)
    var availableWorkflows by mutableStateOf<List<String>>(emptyList())
    var translatedWorkflows by mutableStateOf<Map<String, String>>(emptyMap())
    var lastExecutionResult by mutableStateOf<ExecutionResult?>(null)
    var isNfcEnabled by mutableStateOf(true)

    // Ajustes globales
    var isDarkMode by mutableStateOf(true)
    var isNotificationEnabled by mutableStateOf(true)
    var currentLanguage by mutableStateOf("es")

    // Estados de Asistencia
    var attendanceSummary by mutableStateOf<JsonObject?>(null)
    var scheduleData by mutableStateOf<JsonObject?>(null)
    var currentSemester by mutableStateOf("AUTO")
    var attendanceStateFilter by mutableStateOf("ALL")
    var selectedTimeFilter by mutableStateOf("DAY")
    var timeMenuExpanded by mutableStateOf(false)
    
    // Anclaje temporal para navegación
    var anchorDate by mutableStateOf(LocalDate.parse("2027-04-12"))
    
    // Estado de Edición de Día
    var editingDaySchedule by mutableStateOf<List<JsonObject>>(emptyList())
    var isEditingDayException by mutableStateOf(false)
    var semesterSubjects by mutableStateOf<List<String>>(emptyList())
    
    // Estado de Exportación
    var isExporting by mutableStateOf(false)
    var exportResultFile by mutableStateOf<String?>(null)
    var exportErrorMessage by mutableStateOf<String?>(null)

    private val json = Json { ignoreUnknownKeys = true }

    init {
        // Carga inicial robusta
        refreshState()
    }

    fun refreshState() {
        try {
            val py = Python.getInstance()
            val module = py.getModule("bridge")
            
            // Inicialización de sistema para estabilizar I/O (invisible al usuario)
            if (availableWorkflows.isEmpty()) {
                module.callAttr("initialize_system")
            }

            // 0. Sincronizar ajustes globales
            val settingsJson = module.callAttr("get_app_settings").toString()
            val settings = json.parseToJsonElement(settingsJson).jsonObject
            isDarkMode = settings["dark_mode"]?.jsonPrimitive?.booleanOrNull ?: true
            isNotificationEnabled = settings["notification_enabled"]?.jsonPrimitive?.booleanOrNull ?: true
            currentLanguage = settings["language"]?.jsonPrimitive?.content ?: "es"

            // 1. Sincronizar modo activo
            val modeResult = module.callAttr("get_active_mode")
            val mode = modeResult?.toString()
            activeMode = if (mode == null || mode == "None" || mode == "null") null else mode
            
            // 2. Sincronizar lista de workflows
            val workflowsResult = module.callAttr("get_available_workflows")
            if (workflowsResult != null) {
                val workflowsList = workflowsResult.asList()
                val baseNames = workflowsList.map { it.toString() }.sorted()
                availableWorkflows = baseNames
                
                // 3. Traducir nombres para la UI
                val translations = mutableMapOf<String, String>()
                baseNames.forEach { name ->
                    translations[name] = module.callAttr("translate_workflow", name).toString()
                }
                translatedWorkflows = translations
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun processTag(tagId: String) {
        executePythonAction("execute", tagId)
    }

    fun executeManual(workflowId: String) {
        executePythonAction("execute_manual", workflowId)
    }

    fun deactivateCurrentMode() {
        try {
            val py = Python.getInstance()
            val module = py.getModule("bridge")
            
            // Llamamos a Python y obtenemos el resultado del apagado
            val jsonResponse = module.callAttr("force_work_mode_off").toString()
            val result = json.decodeFromString<ExecutionResult>(jsonResponse)
            
            // Actualizamos el estado global e inmediato de la pantalla
            lastExecutionResult = result
            activeMode = null // Tras forzar apagado, siempre es null
            refreshState() // Sincronizamos todo
        } catch (e: Exception) {
            e.printStackTrace()
            lastExecutionResult = ExecutionResult(critical_error = e.message)
        }
    }

    private fun executePythonAction(method: String, arg: String) {
        try {
            val py = Python.getInstance()
            val module = py.getModule("bridge")
            val jsonResponse = module.callAttr(method, arg).toString()
            
            val result = json.decodeFromString<ExecutionResult>(jsonResponse)
            lastExecutionResult = result
            activeMode = result.active_mode
            currentScreen = Screen.EXECUTION
        } catch (e: Exception) {
            lastExecutionResult = ExecutionResult(critical_error = e.message)
            currentScreen = Screen.EXECUTION
        }
    }

    fun goBackToMenu() {
        currentScreen = Screen.MENU
        refreshState()
    }

    // --- Lógica de Asistencia ---

    fun fetchAttendanceData() {
        try {
            val py = Python.getInstance()
            val module = py.getModule("bridge")
            val jsonResponse = module.callAttr(
                "get_attendance_data", 
                selectedTimeFilter, 
                currentSemester, 
                attendanceStateFilter,
                anchorDate.toString()
            ).toString()
            val result = json.parseToJsonElement(jsonResponse).jsonObject
            attendanceSummary = result
            
            // Si el semestre era "AUTO", actualizamos al detectado para la UI
            if (currentSemester == "AUTO") {
                val detected = result["detected_semester"]?.jsonPrimitive?.content ?: "q1"
                currentSemester = detected
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun fetchScheduleData() {
        try {
            val py = Python.getInstance()
            val module = py.getModule("bridge")
            val jsonResponse = module.callAttr("get_schedule_data", currentSemester).toString()
            scheduleData = json.parseToJsonElement(jsonResponse).jsonObject
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveSchedule(scheduleJson: String) {
        try {
            val py = Python.getInstance()
            val module = py.getModule("bridge")
            module.callAttr("save_schedule_data", scheduleJson, currentSemester)
            fetchScheduleData() // Recargar
            WidgetUtils.refreshWidgets(getApplication())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveDailyException(date: String, scheduleJson: String?) {
        try {
            val py = Python.getInstance()
            val module = py.getModule("bridge")
            module.callAttr("save_daily_exception", date, scheduleJson)
            fetchAttendanceData() // Recargar vista actual
            WidgetUtils.refreshWidgets(getApplication())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleAttendance(date: String, subject: String, currentStatus: String) {
        try {
            val py = Python.getInstance()
            val module = py.getModule("bridge")
            module.callAttr("toggle_attendance", date, subject, currentStatus)
            
            // Si estamos en la pantalla de ejecución y modificamos la asistencia que se acaba de registrar,
            // limpiamos la info para que desaparezca el card.
            if (lastExecutionResult?.attendance_info?.subject == subject) {
                lastExecutionResult = lastExecutionResult?.copy(attendance_info = null)
            }

            fetchAttendanceData() // Mantiene el contexto de selectedTimeFilter
            WidgetUtils.refreshWidgets(getApplication())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun fetchDaySchedule(date: String) {
        try {
            val py = Python.getInstance()
            val module = py.getModule("bridge")
            
            // 1. Cargar Horario del Día
            val jsonResponse = module.callAttr("get_day_schedule_data", date, currentSemester).toString()
            val result = json.parseToJsonElement(jsonResponse).jsonObject
            val schedArray = result["schedule"]?.jsonArray
            editingDaySchedule = schedArray?.map { it.jsonObject } ?: emptyList()
            isEditingDayException = result["is_exception"]?.jsonPrimitive?.booleanOrNull ?: false
            
            // 2. Cargar Asignaturas del Semestre para el selector
            val subjectsResponse = module.callAttr("get_subjects_for_semester", currentSemester).toString()
            val subjectsResult = json.parseToJsonElement(subjectsResponse).jsonObject
            semesterSubjects = subjectsResult["subjects"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isAcademicDay(date: String): Boolean {
        return try {
            val py = Python.getInstance()
            val module = py.getModule("bridge")
            module.callAttr("check_academic_day", date).toBoolean()
        } catch (e: Exception) {
            false
        }
    }

    fun exportAttendance(periodType: String, format: String = "EXCEL") {
        isExporting = true
        exportErrorMessage = null
        try {
            val py = Python.getInstance()
            val module = py.getModule("bridge")
            // Usamos el semestre actual para el reporte si no es YEAR
            val jsonResponse = module.callAttr("export_attendance_excel", periodType, currentSemester, format).toString()
            val result = json.parseToJsonElement(jsonResponse).jsonObject
            
            if (result.containsKey("error")) {
                exportErrorMessage = result["error"]?.jsonPrimitive?.content
            } else {
                exportResultFile = result["file_path"]?.jsonPrimitive?.content
            }
        } catch (e: Exception) {
            exportErrorMessage = e.message
        } finally {
            isExporting = false
        }
    }

    fun clearExportState() {
        exportResultFile = null
        exportErrorMessage = null
    }

    fun navigateTime(forward: Boolean) {
        anchorDate = when (selectedTimeFilter) {
            "DAY" -> if (forward) anchorDate.plusDays(1) else anchorDate.minusDays(1)
            "WEEK" -> if (forward) anchorDate.plusWeeks(1) else anchorDate.minusWeeks(1)
            "MONTH" -> if (forward) anchorDate.plusMonths(1) else anchorDate.minusMonths(1)
            else -> anchorDate
        }
        fetchAttendanceData()
    }

    fun jumpToSemester(semester: String) {
        currentSemester = semester
        anchorDate = if (semester == "q1") {
            LocalDate.parse("2026-09-02")
        } else {
            LocalDate.parse("2027-01-25")
        }
        fetchAttendanceData()
    }
}
