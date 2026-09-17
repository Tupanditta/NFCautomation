package com.example.nfcautomation.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nfcautomation.R
import com.example.nfcautomation.ui.viewmodel.ActionTemplate
import com.example.nfcautomation.ui.viewmodel.ManagementViewModel
import kotlinx.serialization.json.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagementScreen(viewModel: ManagementViewModel, onBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingWorkflowId by remember { mutableStateOf<String?>(null) }
    val tabs = listOf(
        stringResource(R.string.tab_tags),
        stringResource(R.string.tab_workflows),
        stringResource(R.string.tab_config)
    )

    var showNewWorkflowDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    if (editingWorkflowId != null) {
        WorkflowEditorView(
            workflowId = editingWorkflowId!!,
            viewModel = viewModel,
            onBack = { editingWorkflowId = null }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.dialog_back))
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.saveChanges() }) {
                            Icon(Icons.Default.Save, contentDescription = stringResource(R.string.dialog_save), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            },
            floatingActionButton = {
                if (selectedTab != 2) {
                    FloatingActionButton(onClick = { 
                        if (selectedTab == 0) viewModel.isNfcReadingForTag = true 
                        else showNewWorkflowDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> TagsList(viewModel)
                    1 -> WorkflowsList(viewModel, onEditActions = { editingWorkflowId = it })
                    2 -> AppSettingsList(viewModel)
                }
            }
        }
    }

    if (showNewWorkflowDialog) {
        RenameWorkflowDialog(
            currentId = "",
            onRename = { id, isMode -> viewModel.addWorkflow(id, isMode) },
            onDismiss = { showNewWorkflowDialog = false }
        )
    }

    if (viewModel.isNfcReadingForTag) {
        NfcReadingDialog(onDismiss = { viewModel.isNfcReadingForTag = false })
    }
}

@Composable
fun AppSettingsList(viewModel: ManagementViewModel) {
    val isNotificationEnabled = viewModel.settings["notification_enabled"]?.jsonPrimitive?.booleanOrNull ?: true
    val isDarkMode = viewModel.settings["dark_mode"]?.jsonPrimitive?.booleanOrNull ?: true
    val currentLang = viewModel.settings["language"]?.jsonPrimitive?.content ?: "es"

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text(stringResource(R.string.section_system), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.notif_perm_title)) },
                supportingContent = { Text(stringResource(R.string.notif_perm_desc)) },
                trailingContent = {
                    Switch(
                        checked = isNotificationEnabled,
                        onCheckedChange = { viewModel.updateSetting("notification_enabled", JsonPrimitive(it)) }
                    )
                }
            )
            HorizontalDivider()
        }

        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.dark_mode_title)) },
                supportingContent = { Text(stringResource(R.string.dark_mode_desc)) },
                trailingContent = {
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.updateSetting("dark_mode", JsonPrimitive(it)) }
                    )
                }
            )
            HorizontalDivider()
        }

        item {
            var expanded by remember { mutableStateOf(false) }
            val langName = when(currentLang) {
                "en" -> stringResource(R.string.lang_en)
                "eu" -> stringResource(R.string.lang_eu)
                else -> stringResource(R.string.lang_es)
            }

            ListItem(
                headlineContent = { Text(stringResource(R.string.language_label)) },
                supportingContent = { Text(langName) },
                trailingContent = {
                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.Language, null)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.lang_es)) },
                                onClick = { viewModel.updateSetting("language", JsonPrimitive("es")); expanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.lang_en)) },
                                onClick = { viewModel.updateSetting("language", JsonPrimitive("en")); expanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.lang_eu)) },
                                onClick = { viewModel.updateSetting("language", JsonPrimitive("eu")); expanded = false }
                            )
                        }
                    }
                }
            )
            HorizontalDivider()
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
            Text(stringResource(R.string.section_info), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.version_label)) },
                supportingContent = { Text("1.0.0 Stable") }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.developer_label)) },
                supportingContent = { Text("TUPANDITTA") }
            )
        }
    }
}

@Composable
fun TagsList(viewModel: ManagementViewModel) {
    val workflowOptions = viewModel.workflows.keys.toList()
    var tagToDelete by remember { mutableStateOf<String?>(null) }
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(viewModel.tags.toList()) { (uid, currentWorkflow) ->
            var showSelector by remember { mutableStateOf(false) }
            
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = uid, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        TextButton(onClick = { showSelector = true }, contentPadding = PaddingValues(0.dp)) {
                            val displayText = if (currentWorkflow == stringResource(R.string.select_automation)) {
                                currentWorkflow
                            } else {
                                currentWorkflow.replace("toggle:", "${stringResource(R.string.label_mode)}: ").uppercase()
                            }
                            Text(
                                text = displayText, 
                                fontWeight = FontWeight.Bold,
                                color = if (currentWorkflow == stringResource(R.string.select_automation)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = { tagToDelete = uid }) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (showSelector) {
                WorkflowSelectorDialog(
                    options = workflowOptions,
                    onSelected = { selectedValue ->
                        if (!selectedValue.startsWith("toggle:") && !workflowOptions.contains(selectedValue)) {
                            viewModel.addWorkflow(selectedValue)
                        }
                        viewModel.updateTagWorkflow(uid, selectedValue)
                        showSelector = false
                    },
                    onDismiss = { showSelector = false }
                )
            }
        }
    }

    if (tagToDelete != null) {
        DeleteTagConfirmationDialog(
            uid = tagToDelete!!,
            onConfirm = { password ->
                if (viewModel.checkDeletePassword(password)) {
                    viewModel.removeTag(tagToDelete!!)
                    tagToDelete = null
                }
            },
            onDismiss = { tagToDelete = null }
        )
    }
}

@Composable
fun DeleteTagConfirmationDialog(uid: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_delete_tag_title)) },
        text = {
            Column {
                Text(stringResource(R.string.dialog_delete_tag_msg))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "UID: $uid", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.dialog_password)) },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text(stringResource(R.string.dialog_delete_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        }
    )
}

@Composable
fun WorkflowSelectorDialog(options: List<String>, onSelected: (String) -> Unit, onDismiss: () -> Unit) {
    var showNewWfDialog by remember { mutableStateOf(false) }
    var newWfId by remember { mutableStateOf("") }

    if (showNewWfDialog) {
        AlertDialog(
            onDismissRequest = { showNewWfDialog = false },
            title = { Text(stringResource(R.string.dialog_new_wf_title)) },
            text = {
                OutlinedTextField(
                    value = newWfId,
                    onValueChange = { newWfId = it.lowercase().replace(" ", "_") },
                    label = { Text(stringResource(R.string.dialog_wf_name)) },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    onSelected(newWfId)
                    showNewWfDialog = false
                }) { Text(stringResource(R.string.dialog_create_assign)) }
            },
            dismissButton = {
                TextButton(onClick = { showNewWfDialog = false }) { Text(stringResource(R.string.dialog_back)) }
            }
        )
    } else {
        val filteredOptions = options.map { it.replace("_ON", "").replace("_OFF", "") }.distinct()

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.dialog_assign_title)) },
            text = {
                LazyColumn {
                    item {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.dialog_create_new), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.fillMaxWidth().clickable { showNewWfDialog = true }
                        )
                        HorizontalDivider()
                    }
                    items(filteredOptions) { option ->
                        val isToggle = options.contains("${option}_ON")
                        ListItem(
                            headlineContent = { Text(option.uppercase()) },
                            supportingContent = { if (isToggle) Text(stringResource(R.string.label_mode_toggle)) },
                            modifier = Modifier.fillMaxWidth().clickable { 
                                val finalValue = if (isToggle) "toggle:$option" else option
                                onSelected(finalValue) 
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
            }
        )
    }
}

@Composable
fun WorkflowsList(viewModel: ManagementViewModel, onEditActions: (String) -> Unit) {
    var workflowToDelete by remember { mutableStateOf<String?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(viewModel.workflows.toList()) { (wfId, wfData) ->
            var showRenameDialog by remember { mutableStateOf(false) }
            val isMode = wfData["is_mode"]?.jsonPrimitive?.booleanOrNull ?: false
            val actions = wfData["mobile_actions"]?.jsonArray?.size ?: 0
            
            ListItem(
                headlineContent = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(wfId.uppercase(), fontWeight = FontWeight.Black)
                        if (isMode) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    stringResource(R.string.label_mode), 
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                },
                supportingContent = { Text(stringResource(R.string.actions_config_count, actions)) },
                trailingContent = {
                    Row {
                        IconButton(onClick = { showRenameDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                        IconButton(onClick = { workflowToDelete = wfId }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                modifier = Modifier.clickable { onEditActions(wfId) }
            )
            HorizontalDivider()

            if (showRenameDialog) {
                RenameWorkflowDialog(
                    currentId = wfId,
                    onRename = { newId, _ -> viewModel.renameWorkflow(wfId, newId) },
                    onDismiss = { showRenameDialog = false }
                )
            }
        }
    }

    if (workflowToDelete != null) {
        DeleteConfirmationDialog(
            workflowId = workflowToDelete!!,
            onConfirm = { password ->
                if (viewModel.checkDeletePassword(password)) {
                    viewModel.removeWorkflow(workflowToDelete!!)
                    workflowToDelete = null
                }
            },
            onDismiss = { workflowToDelete = null }
        )
    }
}

@Composable
fun DeleteConfirmationDialog(workflowId: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_delete_title)) },
        text = {
            Column {
                Text(stringResource(R.string.dialog_delete_msg, workflowId))
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.dialog_password)) },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text(stringResource(R.string.dialog_delete_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        }
    )
}

@Composable
fun RenameWorkflowDialog(currentId: String, onRename: (String, Boolean) -> Unit, onDismiss: () -> Unit) {
    var newId by remember { mutableStateOf(currentId) }
    var isMode by remember { mutableStateOf(false) }
    val isNew = currentId.isBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) stringResource(R.string.dialog_new_wf_title) else stringResource(R.string.dialog_rename_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = newId,
                    onValueChange = { newId = it.lowercase().replace(" ", "_") },
                    label = { Text(stringResource(R.string.dialog_id_technical)) },
                    singleLine = true
                )
                if (isNew) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = isMode, onCheckedChange = { isMode = it })
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.dialog_is_mode))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onRename(newId, isMode); onDismiss() }) { Text(stringResource(R.string.dialog_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowEditorView(workflowId: String, viewModel: ManagementViewModel, onBack: () -> Unit) {
    val wfData = viewModel.workflows[workflowId] ?: return
    val actions = wfData["mobile_actions"]?.jsonArray ?: JsonArray(emptyList())
    var showActionSelector by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_prefix, workflowId.uppercase())) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.dialog_back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showActionSelector = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_action))
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
            itemsIndexed(actions.toList()) { index, action ->
                ActionItem(
                    index = index,
                    action = action.jsonObject,
                    onRemove = { viewModel.removeActionFromWorkflow(workflowId, index) },
                    onUpdateParam = { key, value -> viewModel.updateActionParam(workflowId, index, key, value) },
                    actionTranslations = viewModel.actionTranslations
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }

    if (showActionSelector) {
        ActionSelectorDialog(
            templates = viewModel.templates,
            onSelected = { 
                viewModel.addActionToWorkflow(workflowId, it)
                showActionSelector = false
            },
            onDismiss = { showActionSelector = false },
            actionTranslations = viewModel.actionTranslations
        )
    }
}

@Composable
fun ActionItem(index: Int, action: JsonObject, onRemove: () -> Unit, onUpdateParam: (String, String) -> Unit, actionTranslations: Map<String, String>) {
    val technicalName = action["action"]?.jsonPrimitive?.content ?: "unknown"
    val friendlyName = actionTranslations[technicalName] ?: technicalName
    val params = action["params"]?.jsonObject ?: JsonObject(emptyMap())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "#${index + 1} ${friendlyName.uppercase()}", fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                }
            }
            params.forEach { (key, value) ->
                ParameterEditor(key = key, value = value.jsonPrimitive.content, onUpdate = { onUpdateParam(key, it) })
            }
        }
    }
}

@Composable
fun ParameterEditor(key: String, value: String, onUpdate: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(text = key.replace("_", " ").uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        when {
            key == "level" || key == "volume" -> {
                var sliderValue by remember(value) { mutableFloatStateOf(value.toFloatOrNull() ?: 0f) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(value = sliderValue, onValueChange = { sliderValue = it; onUpdate(it.toInt().toString()) }, valueRange = 0f..100f, modifier = Modifier.weight(1f))
                    Text(text = "${sliderValue.toInt()}%", modifier = Modifier.width(44.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
            key == "stream" -> {
                val streams = listOf("music", "notification", "ring", "alarm")
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(value.uppercase()); Spacer(Modifier.weight(1f)); Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        streams.forEach { stream -> DropdownMenuItem(text = { Text(stream.uppercase()) }, onClick = { onUpdate(stream); expanded = false }) }
                    }
                }
            }
            key == "alias" -> {
                val apps = listOf("spotify", "youtube", "whatsapp", "instagram", "maps", "gmail", "chrome", "calculadora", "reloj", "ajustes")
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(value.uppercase()); Spacer(Modifier.weight(1f)); Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        apps.forEach { app -> DropdownMenuItem(text = { Text(app.uppercase()) }, onClick = { onUpdate(app); expanded = false }) }
                    }
                }
            }
            key == "enabled" -> {
                val isChecked = value.lowercase() == "true"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isChecked) "ON" else "OFF", modifier = Modifier.weight(1f))
                    Switch(checked = isChecked, onCheckedChange = { onUpdate(it.toString()) })
                }
            }
            else -> {
                var textValue by remember(value) { mutableStateOf(value) }
                val isNumeric = key.contains("duration") || key.contains("seconds")
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it; onUpdate(it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = if (isNumeric) androidx.compose.ui.text.input.KeyboardType.Number else androidx.compose.ui.text.input.KeyboardType.Text)
                )
            }
        }
    }
}

@Composable
fun ActionSelectorDialog(templates: List<ActionTemplate>, onSelected: (ActionTemplate) -> Unit, onDismiss: () -> Unit, actionTranslations: Map<String, String>) {
    val friendlyCancel = stringResource(R.string.dialog_cancel)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_action)) },
        text = {
            LazyColumn {
                items(templates) { template ->
                    val friendlyName = actionTranslations[template.action] ?: template.action
                    ListItem(
                        headlineContent = { Text(friendlyName.uppercase()) },
                        supportingContent = { Text(template.notas ?: "") },
                        modifier = Modifier.clickable { onSelected(template) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(friendlyCancel) }
        }
    )
}

@Composable
fun NfcReadingDialog(onDismiss: () -> Unit) {
    val friendlyCancel = stringResource(R.string.dialog_cancel)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_new_tag_title)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Nfc, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.dialog_new_tag_msg))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(friendlyCancel) }
        }
    )
}
