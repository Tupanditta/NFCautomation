package com.example.nfcautomation.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfcautomation.R
import com.example.nfcautomation.ui.viewmodel.MainViewModel
import com.example.nfcautomation.ui.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(viewModel: MainViewModel, onGoToManagement: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshState()
    }
    
    val selectedText = if (viewModel.activeMode != null) {
        val friendlyName = viewModel.translatedWorkflows[viewModel.activeMode] ?: viewModel.activeMode?.replace("_", " ")?.uppercase() ?: ""
        stringResource(R.string.active_prefix, friendlyName)
    } else {
        stringResource(R.string.select_automation)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                NfcStatusHeader(isEnabled = viewModel.isNfcEnabled)
            }
            Spacer(modifier = Modifier.width(12.dp))
            FilledIconButton(onClick = onGoToManagement) {
                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        ActiveModeCard(viewModel = viewModel, activeMode = viewModel.activeMode)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Nueva Tarjeta de Asistencia
        AttendanceQuickCard(onClick = { viewModel.currentScreen = Screen.ATTENDANCE })
        
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.manual_launch),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(12.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedText,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.workflow_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                    focusedBorderColor = if (viewModel.activeMode != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                    .fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = if (viewModel.activeMode != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (viewModel.activeMode != null) FontWeight.Black else FontWeight.Normal
                )
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                viewModel.availableWorkflows.forEach { workflow ->
                    val isMode = workflow in listOf("work_mode", "class_mode")
                    val isSuggestedOFF = viewModel.activeMode != null && workflow == viewModel.activeMode
                    val isBlocked = isWorkflowBlocked(workflow, viewModel.activeMode)

                    DropdownMenuItem(
                        leadingIcon = {
                            if (isMode) {
                                Icon(Icons.Default.SyncAlt, null, modifier = Modifier.size(18.dp),
                                    tint = if (isSuggestedOFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                        },
                        text = {
                            val friendlyName = viewModel.translatedWorkflows[workflow] ?: workflow.replace("_", " ").uppercase()
                            Text(
                                text = friendlyName,
                                color = if (isSuggestedOFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSuggestedOFF || isMode) FontWeight.Black else FontWeight.Normal
                            )
                        },
                        onClick = {
                            expanded = false
                            viewModel.executeManual(workflow)
                        },
                        enabled = !isBlocked,
                        trailingIcon = {
                            if (isBlocked) {
                                Icon(Icons.Default.Lock, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                            } else if (isSuggestedOFF) {
                                Text("OFF", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        TuPandittaSignature()
    }
}

@Composable
fun NfcStatusHeader(isEnabled: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (isEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isEnabled) Icons.Default.Nfc else Icons.Default.SignalCellularNoSim,
                contentDescription = null,
                tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = if (isEnabled) stringResource(R.string.nfc_ready) else stringResource(R.string.nfc_disabled),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black
                )
                Text(
                    text = if (isEnabled) stringResource(R.string.scanning) else stringResource(R.string.activate_nfc),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun ActiveModeCard(viewModel: MainViewModel, activeMode: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (activeMode != null) MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (activeMode != null) MaterialTheme.colorScheme.secondary else Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(text = stringResource(R.string.active_mode_label), style = MaterialTheme.typography.labelLarge, color = if (activeMode != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = viewModel.translatedWorkflows[activeMode] ?: activeMode?.replace("_", " ")?.uppercase() ?: stringResource(R.string.idle),
                style = MaterialTheme.typography.headlineLarge,
                color = if (activeMode != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun AttendanceQuickCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.BarChart, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(20.dp))
            Text(
                text = stringResource(R.string.attendance_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        }
    }
}

private fun isWorkflowBlocked(workflow: String, activeMode: String?): Boolean {
    if (activeMode == null) return false
    val modes = listOf("work_mode", "class_mode")
    if (workflow in modes) {
        if (workflow != activeMode) return true
    }
    return false
}

@Composable
fun TuPandittaSignature() {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    // El panda siempre debe tener rasgos oscuros
    val pandaDarkColor = if (isDark) Color(0xFF1A1A1A) else Color.Black
    
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 8.dp)) {
        Box(contentAlignment = Alignment.Center) {
            // Fondo de resplandor sutil SOLO en modo oscuro para que se vean las orejas negras sobre fondo negro
            if (isDark) {
                Canvas(modifier = Modifier.size(52.dp)) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.15f),
                        radius = 26.dp.toPx()
                    )
                }
            }
            
            Canvas(modifier = Modifier.size(44.dp)) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                
                // Orejas (Siempre oscuras)
                drawCircle(pandaDarkColor, radius = 7.dp.toPx(), center = androidx.compose.ui.geometry.Offset(centerX - 13.dp.toPx(), centerY - 13.dp.toPx()))
                drawCircle(pandaDarkColor, radius = 7.dp.toPx(), center = androidx.compose.ui.geometry.Offset(centerX + 13.dp.toPx(), centerY - 13.dp.toPx()))
                
                // Cabeza (Blanca)
                drawCircle(Color.White, radius = 20.dp.toPx(), center = center)
                drawCircle(borderColor, radius = 20.dp.toPx(), center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()))
                
                // Ojos (Siempre oscuros)
                drawCircle(pandaDarkColor, radius = 6.dp.toPx(), center = androidx.compose.ui.geometry.Offset(centerX - 8.dp.toPx(), centerY - 2.dp.toPx()))
                drawCircle(pandaDarkColor, radius = 6.dp.toPx(), center = androidx.compose.ui.geometry.Offset(centerX + 8.dp.toPx(), centerY - 2.dp.toPx()))
                
                // Brillo ojos
                drawCircle(Color.White, radius = 1.5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(centerX - 8.dp.toPx(), centerY - 3.dp.toPx()))
                drawCircle(Color.White, radius = 1.5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(centerX + 8.dp.toPx(), centerY - 3.dp.toPx()))
                
                // Nariz
                drawCircle(pandaDarkColor, radius = 2.5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(centerX, centerY + 6.dp.toPx()))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.designed_by, "TUPANDITTA"), 
            style = MaterialTheme.typography.labelSmall, 
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), 
            fontWeight = FontWeight.Bold, 
            letterSpacing = 1.sp
        )
    }
}
