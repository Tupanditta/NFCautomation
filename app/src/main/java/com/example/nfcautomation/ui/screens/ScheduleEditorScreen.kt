package com.example.nfcautomation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nfcautomation.R
import com.example.nfcautomation.ui.viewmodel.MainViewModel
import kotlinx.serialization.json.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditorScreen(viewModel: MainViewModel) {
    var isAuthenticated by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    val days = listOf("lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo")
    val daysDisplay = listOf("L", "M", "X", "J", "V", "S", "D")

    LaunchedEffect(viewModel.currentSemester) {
        viewModel.fetchScheduleData()
    }

    if (!isAuthenticated) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Lock, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(24.dp))
            Text(stringResource(R.string.enter_password), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.dialog_password)) },
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { if (password == "TuPanditta") isAuthenticated = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.dialog_save))
            }
            TextButton(onClick = { viewModel.goBackToMenu() }) {
                Text(stringResource(R.string.dialog_back))
            }
        }
    } else {
        val schedule = viewModel.scheduleData?.toMutableMap() ?: mutableMapOf()
        val currentDay = days[selectedTabIndex]
        val rawClasses = schedule[currentDay]?.jsonArray?.toList() ?: emptyList()
        // Ordenar por hora de inicio para visualización
        val classes = rawClasses.sortedBy { it.jsonObject["start"]?.jsonPrimitive?.content ?: "00:00" }.toMutableList()

        var showAddDialog by remember { mutableStateOf(false) }
        var editingIndex by remember { mutableStateOf<Int?>(null) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Column {
                            Text(stringResource(R.string.schedule_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                            Text(
                                if (viewModel.currentSemester == "q1") stringResource(R.string.semester_q1) else stringResource(R.string.semester_q2),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.goBackToMenu() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },
                    actions = {
                        IconButton(onClick = { 
                            val finalJson = Json.encodeToString(JsonObject.serializer(), JsonObject(schedule))
                            viewModel.saveSchedule(finalJson)
                            viewModel.goBackToMenu()
                        }) {
                            Icon(Icons.Default.Save, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, null)
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                // Selector de Cuatrimestre
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = viewModel.currentSemester == "q1",
                        onClick = { 
                            if (viewModel.currentSemester != "q1") {
                                viewModel.currentSemester = "q1" 
                            }
                        },
                        label = { Text("Q1") }
                    )
                    FilterChip(
                        selected = viewModel.currentSemester == "q2",
                        onClick = { 
                            if (viewModel.currentSemester != "q2") {
                                viewModel.currentSemester = "q2" 
                            }
                        },
                        label = { Text("Q2") }
                    )
                }

                ScrollableTabRow(selectedTabIndex = selectedTabIndex, edgePadding = 16.dp) {
                    daysDisplay.forEachIndexed { index, day ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(day, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(classes) { index, classItem ->
                        val itemObj = classItem.jsonObject
                        val sessionType = itemObj["type"]?.jsonPrimitive?.content ?: "THEORY"
                        
                        ListItem(
                            headlineContent = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(itemObj["subject"]?.jsonPrimitive?.content ?: "", fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(8.dp))
                                    Icon(
                                        imageVector = if (sessionType == "PRACTICE") Icons.Default.Computer else Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    )
                                }
                            },
                            supportingContent = { 
                                Column {
                                    Text("${itemObj["start"]?.jsonPrimitive?.content} - ${itemObj["end"]?.jsonPrimitive?.content}")
                                    Text(
                                        "${itemObj["room"]?.jsonPrimitive?.content} • ${itemObj["building"]?.jsonPrimitive?.content}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { editingIndex = index; showAddDialog = true }) {
                                        Icon(Icons.Default.Edit, null)
                                    }
                                    IconButton(onClick = { 
                                        classes.removeAt(index)
                                        schedule[currentDay] = JsonArray(classes)
                                        viewModel.updateWorkingSchedule(JsonObject(schedule))
                                    }) {
                                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            val classToEdit = if (editingIndex != null) classes[editingIndex!!].jsonObject else null
            ClassEditDialog(
                initialSubject = classToEdit?.get("subject")?.jsonPrimitive?.content ?: "",
                initialStart = classToEdit?.get("start")?.jsonPrimitive?.content ?: "08:00",
                initialEnd = classToEdit?.get("end")?.jsonPrimitive?.content ?: "10:00",
                initialRoom = classToEdit?.get("room")?.jsonPrimitive?.content ?: "",
                initialFloor = classToEdit?.get("floor")?.jsonPrimitive?.content ?: "",
                initialBuilding = classToEdit?.get("building")?.jsonPrimitive?.content ?: "",
                initialType = classToEdit?.get("type")?.jsonPrimitive?.content ?: "THEORY",
                onSave = { subject, start, end, room, floor, building, sessionType ->
                    val newClass = buildJsonObject {
                        put("subject", subject)
                        put("start", start)
                        put("end", end)
                        put("room", room)
                        put("floor", floor)
                        put("building", building)
                        put("type", sessionType)
                    }
                    
                    if (editingIndex != null) {
                        classes[editingIndex!!] = newClass
                    } else {
                        classes.add(newClass)
                    }
                    
                    schedule[currentDay] = JsonArray(classes)
                    viewModel.updateWorkingSchedule(JsonObject(schedule))
                    showAddDialog = false
                    editingIndex = null
                },
                onDismiss = { showAddDialog = false; editingIndex = null }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassEditDialog(
    initialSubject: String,
    initialStart: String,
    initialEnd: String,
    initialRoom: String,
    initialFloor: String,
    initialBuilding: String,
    initialType: String = "THEORY",
    onSave: (String, String, String, String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var subject by remember { mutableStateOf(initialSubject) }
    var start by remember { mutableStateOf(initialStart) }
    var end by remember { mutableStateOf(initialEnd) }
    var room by remember { mutableStateOf(initialRoom) }
    var floor by remember { mutableStateOf(initialFloor) }
    var building by remember { mutableStateOf(initialBuilding) }
    var sessionType by remember { mutableStateOf(initialType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_class)) },
        text = {
            Column {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text(stringResource(R.string.subject_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // ... (session type chips)
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isPractice = sessionType == "PRACTICE"
                    FilterChip(
                        selected = !isPractice,
                        onClick = { sessionType = "THEORY" },
                        label = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.MenuBook, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.session_theory))
                            }
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = isPractice,
                        onClick = { sessionType = "PRACTICE" },
                        label = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Computer, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.session_practice))
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = start,
                        onValueChange = { start = it },
                        label = { Text(stringResource(R.string.start_time_label)) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = end,
                        onValueChange = { end = it },
                        label = { Text(stringResource(R.string.end_time_label)) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        label = { Text(stringResource(R.string.room_label)) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = floor,
                        onValueChange = { floor = it },
                        label = { Text(stringResource(R.string.floor_label)) },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                OutlinedTextField(
                    value = building,
                    onValueChange = { building = it },
                    label = { Text(stringResource(R.string.building_label)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(subject, start, end, room, floor, building, sessionType) }) {
                Text(stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}
