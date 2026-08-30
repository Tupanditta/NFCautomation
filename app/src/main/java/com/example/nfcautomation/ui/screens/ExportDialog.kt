package com.example.nfcautomation.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nfcautomation.R

@Composable
fun ExportDialog(
    currentSemester: String,
    onExport: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf("PDF") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.export_title), fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column {
                Text(stringResource(R.string.export_desc), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))

                // Selector de Formato
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormatChip(
                        label = "PDF (Gráficos)", 
                        icon = Icons.Default.PictureAsPdf, 
                        selected = selectedFormat == "PDF",
                        onClick = { selectedFormat = "PDF" },
                        modifier = Modifier.weight(1f)
                    )
                    FormatChip(
                        label = "Excel (Datos)", 
                        icon = Icons.Default.TableChart, 
                        selected = selectedFormat == "EXCEL",
                        onClick = { selectedFormat = "EXCEL" },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(16.dp))
                
                ExportOptionItem(stringResource(R.string.export_week)) { onExport("WEEK", selectedFormat) }
                ExportOptionItem(stringResource(R.string.export_month)) { onExport("MONTH", selectedFormat) }
                ExportOptionItem(stringResource(R.string.export_semester, currentSemester.uppercase())) { onExport("SEMESTER", selectedFormat) }
                ExportOptionItem(stringResource(R.string.export_year)) { onExport("YEAR", selectedFormat) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

@Composable
fun FormatChip(
    label: String, 
    icon: ImageVector, 
    selected: Boolean, 
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(18.dp)) },
        modifier = modifier
    )
}

@Composable
fun ExportOptionItem(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label, 
                fontWeight = FontWeight.Bold, 
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.Description, 
                contentDescription = null, 
                modifier = Modifier.size(20.dp), 
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
