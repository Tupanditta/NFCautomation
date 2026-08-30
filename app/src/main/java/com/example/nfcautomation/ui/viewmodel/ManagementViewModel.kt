package com.example.nfcautomation.ui.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.chaquo.python.Python
import kotlinx.serialization.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

@Serializable
data class ManagementData(
    val tags: Map<String, String>,
    val workflows: Map<String, JsonObject>,
    val templates: List<ActionTemplate>,
    val settings: Map<String, JsonElement> = emptyMap(),
    val action_translations: Map<String, String> = emptyMap()
)

@Serializable
data class ActionTemplate(
    val action: String,
    val params: JsonObject? = null,
    val notas: String? = null
)

class ManagementViewModel : ViewModel() {
    companion object {
        private const val DELETE_PASSWORD = "TuPanditta"
    }

    var tags by mutableStateOf<Map<String, String>>(emptyMap())
    var workflows by mutableStateOf<Map<String, JsonObject>>(emptyMap())
    var templates by mutableStateOf<List<ActionTemplate>>(emptyList())
    var settings by mutableStateOf<Map<String, JsonElement>>(emptyMap())
    var actionTranslations by mutableStateOf<Map<String, String>>(emptyMap())
    
    // Callbacks para actualización en tiempo real
    var onDarkModeToggle: ((Boolean) -> Unit)? = null
    var onNotificationToggle: ((Boolean) -> Unit)? = null
    var onLanguageChange: ((String) -> Unit)? = null
    
    var isNfcReadingForTag by mutableStateOf(false)
    var pendingTagUid by mutableStateOf<String?>(null)
    
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    fun checkDeletePassword(password: String): Boolean {
        return password == DELETE_PASSWORD
    }

    fun loadData() {
        // Evitar recargar si ya tenemos datos (previene perder cambios locales tras recreate)
        if (settings.isNotEmpty() && workflows.isNotEmpty()) return

        try {
            val py = Python.getInstance()
            val module = py.getModule("bridge")
            val jsonResponse = module.callAttr("get_management_data").toString()
            
            val data = json.decodeFromString<ManagementData>(jsonResponse)
            
            // FILTRADO: Ocultar workflows de sistema (app_init)
            tags = data.tags
            workflows = data.workflows.filter { (_, data) ->
                val isSystem = data["system"]?.jsonPrimitive?.booleanOrNull ?: false
                !isSystem
            }
            templates = data.templates
            settings = data.settings.toMutableMap().apply {
                if (!containsKey("language")) put("language", JsonPrimitive("es"))
            }

            // LIMPIEZA AUTOMÁTICA de workflows "fantasma"
            cleanupWorkflows()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cleanupWorkflows() {
        val currentWorkflows = workflows.toMutableMap()
        val toRemove = currentWorkflows.keys.filter { it.startsWith("toggle:") || it.contains("worke") }
        
        if (toRemove.isNotEmpty()) {
            toRemove.forEach { currentWorkflows.remove(it) }
            workflows = currentWorkflows
            // Guardar cambios automáticamente tras la limpieza
            saveChanges()
        }
    }

    fun saveChanges() {
        try {
            val py = Python.getInstance()
            val module = py.getModule("bridge")
            
            // IMPORTANTE: Al guardar, debemos recuperar los workflows de sistema 
            // que ocultamos en loadData, para no borrarlos del archivo.
            val currentFullResponse = module.callAttr("get_management_data").toString()
            val fullData = json.decodeFromString<ManagementData>(currentFullResponse)
            
            val finalWorkflows = fullData.workflows.toMutableMap()
            // Sobrescribimos solo los que el usuario puede ver/editar
            workflows.forEach { (id, data) ->
                finalWorkflows[id] = data
            }
            // Eliminamos los que el usuario borró (que no sean de sistema)
            val systemIds = fullData.workflows.filter { it.value["system"]?.jsonPrimitive?.booleanOrNull == true }.keys
            val userIdsInState = workflows.keys
            finalWorkflows.keys.retainAll { it in userIdsInState || it in systemIds }

            val tagsJson = json.encodeToString(tags)
            val workflowsJson = json.encodeToString(finalWorkflows)
            val settingsJson = json.encodeToString(settings)
            
            module.callAttr("save_management_data", tagsJson, workflowsJson, settingsJson)
            loadData() // Recargar para confirmar
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateSetting(key: String, value: JsonElement) {
        settings = settings.toMutableMap().apply {
            put(key, value)
        }
        
        // Disparar callbacks para efectos inmediatos
        when (key) {
            "dark_mode" -> onDarkModeToggle?.invoke(value.jsonPrimitive.boolean)
            "notification_enabled" -> onNotificationToggle?.invoke(value.jsonPrimitive.boolean)
            "language" -> {
                onLanguageChange?.invoke(value.jsonPrimitive.content)
                saveChanges() // Guardar inmediatamente para persistir tras recreate()
            }
        }
    }

    fun updateTagWorkflow(uid: String, workflowId: String) {
        tags = tags.toMutableMap().apply {
            put(uid, workflowId)
        }
    }

    fun removeTag(uid: String) {
        tags = tags.toMutableMap().apply {
            remove(uid)
        }
    }

    fun addWorkflow(id: String, isMode: Boolean = false) {
        val currentWorkflows = workflows.toMutableMap()
        if (isMode) {
            val idOn = "${id}_ON"
            val idOff = "${id}_OFF"
            
            if (!currentWorkflows.containsKey(idOn)) {
                currentWorkflows[idOn] = buildJsonObject {
                    putJsonArray("mobile_actions") { }
                    put("is_mode", true)
                }
            }
            if (!currentWorkflows.containsKey(idOff)) {
                currentWorkflows[idOff] = buildJsonObject {
                    putJsonArray("mobile_actions") { }
                    put("is_mode", true)
                }
            }
        } else {
            if (!currentWorkflows.containsKey(id)) {
                currentWorkflows[id] = buildJsonObject {
                    putJsonArray("mobile_actions") { }
                }
            }
        }
        workflows = currentWorkflows
    }

    fun removeWorkflow(id: String) {
        val currentWorkflows = workflows.toMutableMap()
        val data = currentWorkflows[id]
        val isMode = data?.get("is_mode")?.jsonPrimitive?.booleanOrNull ?: false
        
        if (isMode) {
            // Si es un modo, intentamos borrar la pareja
            val baseId = id.replace("_ON", "").replace("_OFF", "")
            currentWorkflows.remove("${baseId}_ON")
            currentWorkflows.remove("${baseId}_OFF")
        } else {
            currentWorkflows.remove(id)
        }
        workflows = currentWorkflows
    }

    fun renameWorkflow(oldId: String, newId: String) {
        if (oldId == newId || newId.isBlank()) return
        val currentWorkflows = workflows.toMutableMap()
        val data = currentWorkflows[oldId] ?: return
        val isMode = data["is_mode"]?.jsonPrimitive?.booleanOrNull ?: false

        if (isMode) {
            // RENOMBRADO DUAL: Detectamos base y renombramos pareja
            val oldBase = oldId.replace("_ON", "").replace("_OFF", "")
            val newBase = newId.replace("_ON", "").replace("_OFF", "")
            
            val suffix = if (oldId.endsWith("_ON")) "_ON" else "_OFF"
            val otherSuffix = if (suffix == "_ON") "_OFF" else "_ON"
            
            // Renombrar actual
            currentWorkflows.remove("${oldBase}$suffix")
            currentWorkflows["${newBase}$suffix"] = data
            
            // Renombrar pareja
            val otherData = currentWorkflows["${oldBase}$otherSuffix"]
            if (otherData != null) {
                currentWorkflows.remove("${oldBase}$otherSuffix")
                currentWorkflows["${newBase}$otherSuffix"] = otherData
            }
            
            workflows = currentWorkflows
            updateTagsAfterRename(oldBase, newBase)
        } else {
            currentWorkflows.remove(oldId)
            currentWorkflows[newId] = data
            workflows = currentWorkflows
            updateTagsAfterRename(oldId, newId)
        }
    }

    private fun updateTagsAfterRename(oldId: String, newId: String) {
        val currentTags = tags.toMutableMap()
        var changed = false
        currentTags.forEach { (uid, target) ->
            // Caso 1: Vínculo directo (ej: night)
            if (target == oldId) {
                currentTags[uid] = newId
                changed = true
            } 
            // Caso 2: Vínculo toggle (ej: toggle:work_mode)
            else if (target == "toggle:$oldId") {
                currentTags[uid] = "toggle:$newId"
                changed = true
            }
        }
        if (changed) tags = currentTags
    }

    fun addActionToWorkflow(workflowId: String, template: ActionTemplate) {
        val currentWorkflows = workflows.toMutableMap()
        val wfData = currentWorkflows[workflowId]?.toMutableMap() ?: return
        
        val actions = wfData["mobile_actions"]?.jsonArray?.toMutableList() ?: mutableListOf()
        val newAction = buildJsonObject {
            put("action", template.action)
            template.params?.let { put("params", it) }
        }
        actions.add(newAction)
        
        wfData["mobile_actions"] = JsonArray(actions)
        currentWorkflows[workflowId] = JsonObject(wfData)
        workflows = currentWorkflows
    }

    fun removeActionFromWorkflow(workflowId: String, index: Int) {
        val currentWorkflows = workflows.toMutableMap()
        val wfData = currentWorkflows[workflowId]?.toMutableMap() ?: return
        
        val actions = wfData["mobile_actions"]?.jsonArray?.toMutableList() ?: return
        if (index in actions.indices) {
            actions.removeAt(index)
            wfData["mobile_actions"] = JsonArray(actions)
            currentWorkflows[workflowId] = JsonObject(wfData)
            workflows = currentWorkflows
        }
    }

    fun updateActionParam(workflowId: String, actionIndex: Int, paramKey: String, newValue: String) {
        val currentWorkflows = workflows.toMutableMap()
        val wfData = currentWorkflows[workflowId]?.toMutableMap() ?: return
        
        val actions = wfData["mobile_actions"]?.jsonArray?.toMutableList() ?: return
        if (actionIndex in actions.indices) {
            val action = actions[actionIndex].jsonObject.toMutableMap()
            val params = action["params"]?.jsonObject?.toMutableMap() ?: mutableMapOf()
            
            // Intentar inferir tipo (número o string)
            val jsonValue = newValue.toIntOrNull()?.let { JsonPrimitive(it) } 
                           ?: newValue.toBooleanStrictOrNull()?.let { JsonPrimitive(it) }
                           ?: JsonPrimitive(newValue)
            
            params[paramKey] = jsonValue
            action["params"] = JsonObject(params)
            actions[actionIndex] = JsonObject(action)
            
            wfData["mobile_actions"] = JsonArray(actions)
            currentWorkflows[workflowId] = JsonObject(wfData)
            workflows = currentWorkflows
        }
    }
}
