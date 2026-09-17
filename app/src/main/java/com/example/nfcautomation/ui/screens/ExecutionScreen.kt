package com.example.nfcautomation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.nfcautomation.R
import com.example.nfcautomation.ui.viewmodel.MainViewModel

@Composable
fun ExecutionScreen(viewModel: MainViewModel) {
    val result = viewModel.lastExecutionResult ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        // 1. Cabecera Limpia con Badge de Estado
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { viewModel.goBackToMenu() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.dialog_back), tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = stringResource(R.string.execution_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            
            // Badge de Estado ON/OFF
            if (result.is_toggle && result.toggle_type != null) {
                Surface(
                    color = if (result.toggle_type == "ON") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text(
                        text = result.toggle_type,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (result.toggle_type == "ON") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Información del Workflow (Limpio)
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small
        ) {
            val friendlyTarget = viewModel.translatedWorkflows[result.target?.replace("_ON", "")?.replace("_OFF", "")] 
                ?: result.target?.replace("_ON", "")?.replace("_OFF", "")?.replace("_", " ")?.uppercase() 
                ?: stringResource(R.string.unknown_target)
            Text(
                text = friendlyTarget,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Área de Contenido Principal
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            if (result.critical_error != null) {
                ErrorCard(message = result.critical_error)
            }

            // --- SECCIÓN DE ASISTENCIA (SI EXISTE) ---
            result.attendance_info?.let { info ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.class_registered),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = info.subject,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { viewModel.toggleAttendance(info.date, info.subject, info.startTime, "ATTENDED") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        ) {
                            Text(stringResource(R.string.unmark_attendance))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- LISTADO SIMPLIFICADO DE ACCIONES ---
            if (result.summarized_actions.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.actions_performed),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                result.summarized_actions.forEach { action ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Check, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = action, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            // --- DETALLES DE EJECUCIÓN (LOGS) ---
            if (!result.details.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.execution_details),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        result.details.split("\n").forEach { line ->
                            if (line.isNotBlank()) {
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // --- LOGS TÉCNICOS (Solo si hay errores) ---
            if (result.errors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.issues_detected),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                result.errors.forEach { error ->
                    LogItem(text = error, isError = true)
                }
            }
        }

        // 4. Botón Dinámico Contextual
        if (result.is_toggle) {
            Spacer(modifier = Modifier.height(24.dp))
            
            val isCurrentlyOn = result.toggle_type == "ON"
            
            Button(
                onClick = { 
                    if (isCurrentlyOn) viewModel.deactivateCurrentMode()
                    else {
                        val base = result.target?.replace("_OFF", "") ?: ""
                        viewModel.executeManual(base)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCurrentlyOn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    contentColor = if (isCurrentlyOn) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = if (isCurrentlyOn) stringResource(R.string.deactivate_mode) else stringResource(R.string.reactivate_mode),
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = { viewModel.goBackToMenu() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.back_to_start))
        }
    }
}

@Composable
fun ErrorCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Composable
fun LogItem(text: String, isError: Boolean = false) {
    val cleanText = text.removePrefix("•").trim()
    val isAction = text.startsWith("•") || isError
    
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Icon(
            imageVector = when {
                isError -> Icons.Default.ErrorOutline
                isAction -> Icons.Default.CheckCircle
                else -> Icons.Default.ChevronRight
            },
            contentDescription = null,
            tint = when {
                isError -> MaterialTheme.colorScheme.error
                isAction -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = cleanText,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isAction) FontWeight.SemiBold else FontWeight.Normal,
            lineHeight = 20.sp
        )
    }
}
