package com.example.nfcautomation.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import com.example.nfcautomation.ui.viewmodel.MainViewModel
import com.example.nfcautomation.ui.viewmodel.Screen
import kotlinx.serialization.json.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(viewModel: MainViewModel) {
    var showExportDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateToEdit by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(viewModel.selectedTimeFilter, viewModel.currentSemester, viewModel.attendanceStateFilter, viewModel.anchorDate) {
        viewModel.fetchAttendanceData()
    }

    val summary = viewModel.attendanceSummary
    val records = summary?.get("records")?.jsonArray?.toList() ?: emptyList()
    val stats = summary?.get("stats")?.jsonObject

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.attendance_title), fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.goBackToMenu() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.FileDownload, contentDescription = stringResource(R.string.export_title))
                    }
                    IconButton(onClick = { viewModel.currentScreen = Screen.SCHEDULE_EDITOR }) {
                        Icon(Icons.Default.EditCalendar, contentDescription = stringResource(R.string.edit_schedule))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(horizontal = 20.dp)) {
            // Fila de Filtros Superiores
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedCard(onClick = { viewModel.timeMenuExpanded = true }) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = when(viewModel.selectedTimeFilter) {
                                    "DAY" -> stringResource(R.string.filter_day)
                                    "WEEK" -> stringResource(R.string.filter_week)
                                    else -> stringResource(R.string.filter_month)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    DropdownMenu(expanded = viewModel.timeMenuExpanded, onDismissRequest = { viewModel.timeMenuExpanded = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.filter_day)) }, onClick = { viewModel.selectedTimeFilter = "DAY"; viewModel.timeMenuExpanded = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.filter_week)) }, onClick = { viewModel.selectedTimeFilter = "WEEK"; viewModel.timeMenuExpanded = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.filter_month)) }, onClick = { viewModel.selectedTimeFilter = "MONTH"; viewModel.timeMenuExpanded = false })
                    }
                }
                
                Spacer(Modifier.width(12.dp))
                
                FilterChip(selected = viewModel.currentSemester == "q1", onClick = { viewModel.jumpToSemester("q1") }, label = { Text("Q1") })
                Spacer(Modifier.width(4.dp))
                FilterChip(selected = viewModel.currentSemester == "q2", onClick = { viewModel.jumpToSemester("q2") }, label = { Text("Q2") })
            }

            // --- NAVEGADOR TEMPORAL (Flechas) ---
            TimeNavigationHeader(viewModel, onLabelClick = { showDatePicker = true })

            // Filtro de Estados
            LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val stateFilters = listOf("ALL" to R.string.all_label, "ATTENDED" to R.string.attended_label, "MISSED" to R.string.missed_label, "UPCOMING" to R.string.upcoming_label)
                items(stateFilters) { (filter, labelRes) ->
                    FilterChip(selected = viewModel.attendanceStateFilter == filter, onClick = { viewModel.attendanceStateFilter = filter }, label = { Text(stringResource(labelRes)) })
                }
            }

            if (stats != null) {
                AttendanceStatsHeader(stats)
            }
            
            // Botón Modificar Día
            Button(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                Icon(Icons.Default.EventAvailable, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.modify_day), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (records.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_records), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(records) { record ->
                        AttendanceRecordItem(record.jsonObject, viewModel)
                    }
                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }

    if (showDatePicker) {
        AcademicDatePicker(
            viewModel = viewModel,
            onDateSelected = { 
                selectedDateToEdit = it
                showDatePicker = false
                viewModel.fetchDaySchedule(it)
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (selectedDateToEdit != null) {
        DailyExceptionDialog(
            date = selectedDateToEdit!!,
            viewModel = viewModel,
            onDismiss = { selectedDateToEdit = null }
        )
    }

    if (showExportDialog) {
        ExportDialog(currentSemester = viewModel.currentSemester, onExport = { period, format -> viewModel.exportAttendance(period, format); showExportDialog = false }, onDismiss = { showExportDialog = false })
    }

    if (viewModel.isExporting) {
        AlertDialog(onDismissRequest = { }, title = { Text(stringResource(R.string.exporting_msg)) }, text = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }, confirmButton = { })
    }
}

@Composable
fun TimeNavigationHeader(viewModel: MainViewModel, onLabelClick: () -> Unit) {
    val formatter = remember { DateTimeFormatter.ofPattern("dd MMMM yyyy") }
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy") }
    
    val displayText = when (viewModel.selectedTimeFilter) {
        "DAY" -> viewModel.anchorDate.format(formatter)
        "WEEK" -> {
            val start = viewModel.anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val end = start.plusDays(6)
            "${start.dayOfMonth} - ${end.dayOfMonth} ${end.format(DateTimeFormatter.ofPattern("MMM"))}"
        }
        "MONTH" -> viewModel.anchorDate.format(monthFormatter).uppercase()
        else -> ""
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.navigateTime(false) }) {
                Icon(Icons.Default.ChevronLeft, null, tint = MaterialTheme.colorScheme.primary)
            }
            
            Text(
                text = displayText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable { onLabelClick() }
            )
            
            IconButton(onClick = { viewModel.navigateTime(true) }) {
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicDatePicker(viewModel: MainViewModel, onDateSelected: (String) -> Unit, onDismiss: () -> Unit) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = viewModel.anchorDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneId.of("UTC")).toLocalDate()
                return viewModel.isAcademicDay(date.toString())
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let {
                    val date = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()
                    onDateSelected(date.toString())
                    viewModel.anchorDate = date // Actualizar anclaje al seleccionar
                }
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
fun DailyExceptionDialog(date: String, viewModel: MainViewModel, onDismiss: () -> Unit) {
    var tempSchedule by remember(viewModel.editingDaySchedule) { 
        mutableStateOf(viewModel.editingDaySchedule) 
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_day_title, date), fontWeight = FontWeight.Black) },
        text = {
            Column(modifier = Modifier.heightIn(max = 450.dp)) {
                if (viewModel.isEditingDayException) {
                    Text(stringResource(R.string.exception_notice), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(8.dp))
                }
                
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f, fill = false)) {
                    itemsIndexed(tempSchedule) { index, item ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // SELECTOR DE ASIGNATURA
                                    var expanded by remember { mutableStateOf(false) }
                                    val currentSubj = item["subject"]?.jsonPrimitive?.content ?: ""
                                    
                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedCard(
                                            onClick = { expanded = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = currentSubj.ifBlank { stringResource(R.string.class_name_hint) },
                                                    modifier = Modifier.weight(1f),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (currentSubj.isNotBlank()) FontWeight.Bold else FontWeight.Normal
                                                )
                                                Icon(Icons.Default.ArrowDropDown, null)
                                            }
                                        }
                                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                            viewModel.semesterSubjects.forEach { subj ->
                                                DropdownMenuItem(
                                                    text = { Text(subj) },
                                                    onClick = {
                                                        val newList = tempSchedule.toMutableList()
                                                        val updated = item.toMutableMap().apply { put("subject", JsonPrimitive(subj)) }
                                                        newList[index] = JsonObject(updated)
                                                        tempSchedule = newList
                                                        expanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    IconButton(onClick = {
                                        val newList = tempSchedule.toMutableList()
                                        newList.removeAt(index)
                                        tempSchedule = newList
                                    }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                                }
                                
                                // MARCADOR TEORÍA / PRÁCTICA
                                Row(
                                    modifier = Modifier.padding(top = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val isPractice = item["type"]?.jsonPrimitive?.content == "PRACTICE"
                                    FilterChip(
                                        selected = !isPractice,
                                        onClick = {
                                            val newList = tempSchedule.toMutableList()
                                            val updated = item.toMutableMap().apply { put("type", JsonPrimitive("THEORY")) }
                                            newList[index] = JsonObject(updated)
                                            tempSchedule = newList
                                        },
                                        label = { 
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.AutoMirrored.Filled.MenuBook, null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("T")
                                            }
                                        }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    FilterChip(
                                        selected = isPractice,
                                        onClick = {
                                            val newList = tempSchedule.toMutableList()
                                            val updated = item.toMutableMap().apply { put("type", JsonPrimitive("PRACTICE")) }
                                            newList[index] = JsonObject(updated)
                                            tempSchedule = newList
                                        },
                                        label = { 
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Computer, null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("P")
                                            }
                                        }
                                    )
                                }

                                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = item["start"]?.jsonPrimitive?.content ?: "",
                                        onValueChange = { newVal ->
                                            val newList = tempSchedule.toMutableList()
                                            val updated = item.toMutableMap().apply { put("start", JsonPrimitive(newVal)) }
                                            newList[index] = JsonObject(updated)
                                            tempSchedule = newList
                                        },
                                        label = { Text(stringResource(R.string.start_time_label)) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = item["end"]?.jsonPrimitive?.content ?: "",
                                        onValueChange = { newVal ->
                                            val newList = tempSchedule.toMutableList()
                                            val updated = item.toMutableMap().apply { put("end", JsonPrimitive(newVal)) }
                                            newList[index] = JsonObject(updated)
                                            tempSchedule = newList
                                        },
                                        label = { Text(stringResource(R.string.end_time_label)) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                
                                // --- Campos de Localización (Aula, Piso, Edificio) ---
                                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = item["room"]?.jsonPrimitive?.content ?: "",
                                        onValueChange = { newVal ->
                                            val newList = tempSchedule.toMutableList()
                                            val updated = item.toMutableMap().apply { put("room", JsonPrimitive(newVal)) }
                                            newList[index] = JsonObject(updated)
                                            tempSchedule = newList
                                        },
                                        label = { Text(stringResource(R.string.room_label)) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = item["floor"]?.jsonPrimitive?.content ?: "",
                                        onValueChange = { newVal ->
                                            val newList = tempSchedule.toMutableList()
                                            val updated = item.toMutableMap().apply { put("floor", JsonPrimitive(newVal)) }
                                            newList[index] = JsonObject(updated)
                                            tempSchedule = newList
                                        },
                                        label = { Text(stringResource(R.string.floor_label)) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                OutlinedTextField(
                                    value = item["building"]?.jsonPrimitive?.content ?: "",
                                    onValueChange = { newVal ->
                                        val newList = tempSchedule.toMutableList()
                                        val updated = item.toMutableMap().apply { put("building", JsonPrimitive(newVal)) }
                                        newList[index] = JsonObject(updated)
                                        tempSchedule = newList
                                    },
                                    label = { Text(stringResource(R.string.building_label)) },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
                
                Button(
                    onClick = { 
                        val newClass = buildJsonObject { 
                            put("subject", "")
                            put("start", "08:00")
                            put("end", "10:00")
                            put("type", "THEORY")
                            put("room", "-")
                            put("floor", "-")
                            put("building", "-")
                        }
                        tempSchedule = tempSchedule + newClass
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.add_class_short))
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                val jsonArr = JsonArray(tempSchedule)
                val jsonStr = Json.encodeToString(JsonArray.serializer(), jsonArr)
                viewModel.saveDailyException(date, jsonStr)
                onDismiss()
            }) { Text(stringResource(R.string.dialog_save)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { 
                    viewModel.saveDailyException(date, null)
                    onDismiss()
                }) { Text(stringResource(R.string.restore_base), color = MaterialTheme.colorScheme.error) }
                
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
            }
        }
    )
}

@Composable
fun AttendanceStatsHeader(stats: JsonObject) {
    val attended = stats["total_attended"]?.jsonPrimitive?.intOrNull ?: 0
    val missed = stats["total_missed"]?.jsonPrimitive?.intOrNull ?: 0
    val upcoming = stats["total_upcoming"]?.jsonPrimitive?.intOrNull ?: 0
    val percentage = stats["percentage"]?.jsonPrimitive?.doubleOrNull ?: 0.0

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard(label = stringResource(R.string.attended_label), value = attended.toString(), color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        StatCard(label = stringResource(R.string.missed_label), value = missed.toString(), color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
        StatCard(label = stringResource(R.string.upcoming_label), value = upcoming.toString(), color = Color.Gray, modifier = Modifier.weight(1f))
        StatCard(label = "%", value = "$percentage%", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
    }
}

@Composable
fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
fun AttendanceRecordItem(record: JsonObject, viewModel: MainViewModel) {
    val subject = record["subject"]?.jsonPrimitive?.content ?: "Unknown"
    val date = record["date"]?.jsonPrimitive?.content ?: ""
    val start = record["start"]?.jsonPrimitive?.content ?: ""
    val status = record["status"]?.jsonPrimitive?.content ?: "UPCOMING"
    val isException = record["is_exception"]?.jsonPrimitive?.booleanOrNull ?: false
    val sessionType = record["type"]?.jsonPrimitive?.content ?: "THEORY"
    
    // Metadatos de localización
    val room = record["room"]?.jsonPrimitive?.content ?: "-"
    val floor = record["floor"]?.jsonPrimitive?.content ?: "-"
    val building = record["building"]?.jsonPrimitive?.content ?: "-"

    val cardColor = when(status) {
        "ATTENDED" -> MaterialTheme.colorScheme.surface
        "MISSED" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        "HOLIDAY" -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    }
    
    val borderColor = when(status) {
        "ATTENDED" -> MaterialTheme.colorScheme.outlineVariant
        "MISSED" -> MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
        "HOLIDAY" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (status == "HOLIDAY") subject else subject.uppercase(), 
                        fontWeight = FontWeight.Black, 
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (status == "HOLIDAY") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                    )
                    if (status != "HOLIDAY") {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = if (sessionType == "PRACTICE") Icons.Default.Computer else Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    if (isException) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.PushPin, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                    }
                }
                Text(
                    text = if (status == "HOLIDAY") date else "$date | $start", 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                
                // --- Información de Localización ---
                if (status != "HOLIDAY") {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${stringResource(R.string.room_label)}: $room • ${stringResource(R.string.floor_label)}: $floor",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "${stringResource(R.string.building_label)}: $building",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (status == "ATTENDED" || status == "MISSED") {
                    val isAttended = status == "ATTENDED"
                    
                    FilledTonalIconButton(
                        onClick = { viewModel.toggleAttendance(date, subject, status) },
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (isAttended) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) 
                                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            contentColor = if (isAttended) MaterialTheme.colorScheme.error 
                                          else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (isAttended) Icons.Default.Block else Icons.Default.Check,
                            contentDescription = if (isAttended) "Marcar como falta" else "Marcar como asistida",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                }

                when(status) {
                    "ATTENDED" -> Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                    "MISSED" -> Icon(Icons.Default.Cancel, null, tint = MaterialTheme.colorScheme.error)
                    "UPCOMING" -> Icon(Icons.Default.Schedule, null, tint = Color.Gray)
                    "HOLIDAY" -> Icon(Icons.Default.BeachAccess, null, tint = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}
