package com.example.nfcautomation.ui.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.*
import androidx.glance.text.*
import com.example.nfcautomation.R
import com.example.nfcautomation.utils.ClassSession
import com.example.nfcautomation.utils.ScheduleRepository
import java.time.LocalDate

class ClassDetailWidget : GlanceAppWidget() {
    companion object {
        val KEY_INDEX = intPreferencesKey("class_index")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            // Usamos el tema dinámico de Material3 para widgets (GlanceTheme)
            GlanceTheme {
                val repository = ScheduleRepository(context)
                val today = LocalDate.now()
                val schedule = repository.getEffectiveSchedule(today)
                
                val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
                val currentIndex = prefs[KEY_INDEX] ?: 0
                
                WidgetContent(context, schedule, currentIndex)
            }
        }
    }

    @Composable
    private fun WidgetContent(context: Context, schedule: List<ClassSession>, index: Int) {
        // Encerramos todo en un Box con fondo surfaceVariant para reducir el contraste con el wallpaper
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.surfaceVariant)
                .cornerRadius(22.dp) // Mayor redondeo
        ) {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                Text(
                    text = context.getString(R.string.widget_class_detail_title).uppercase(),
                    style = TextStyle(
                        fontWeight = FontWeight.Bold, 
                        fontSize = 9.sp, 
                        color = GlanceTheme.colors.primary
                    )
                )

                Spacer(modifier = GlanceModifier.height(8.dp))

                if (schedule.isEmpty()) {
                    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = context.getString(R.string.no_classes_today),
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
                        )
                    }
                } else {
                    val safeIndex = index.coerceIn(0, schedule.size - 1)
                    val session = schedule[safeIndex]
                    
                    // Box contenedor de la tarjeta para permitir el refresco visual correcto
                    Box(modifier = GlanceModifier.fillMaxSize()) {
                        DetailCard(context, session, safeIndex, schedule.size)
                    }
                }
            }
        }
    }

    @Composable
    private fun DetailCard(context: Context, session: ClassSession, index: Int, total: Int) {
        var cardModifier = GlanceModifier.fillMaxSize()
        
        // El fondo destacado de excepción también se adapta al tema del wallpaper
        if (session.isException) {
            cardModifier = cardModifier.background(GlanceTheme.colors.primaryContainer)
        }

        Column(
            modifier = cardModifier
                .cornerRadius(12.dp)
                .padding(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = if (session.type == "PRACTICE") "💻" else "📖"
                Text(
                    text = "$icon ", 
                    style = TextStyle(fontSize = 18.sp, color = if (session.isException) GlanceTheme.colors.onPrimaryContainer else GlanceTheme.colors.onSurface)
                )
                Text(
                    text = session.subject.uppercase(),
                    style = TextStyle(
                        fontWeight = FontWeight.Bold, 
                        fontSize = 15.sp, 
                        color = if (session.isException) GlanceTheme.colors.onPrimaryContainer else GlanceTheme.colors.onSurface,
                        textDecoration = if (session.isDeleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                if (session.isDeleted) {
                    Text(
                        text = " CANCELADA",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = GlanceTheme.colors.error
                        )
                    )
                }
                if (session.isException && !session.isDeleted) {
                    Text(
                        text = "📌", 
                        style = TextStyle(fontSize = 12.sp, color = if (session.isException) GlanceTheme.colors.onPrimaryContainer else GlanceTheme.colors.primary)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            Text(
                text = "${context.getString(R.string.room_label)}: ${session.room} • ${context.getString(R.string.floor_label)}: ${session.floor}",
                style = TextStyle(
                    fontSize = 12.sp, 
                    color = if (session.isException) GlanceTheme.colors.onPrimaryContainer else GlanceTheme.colors.primary
                )
            )
            
            Text(
                text = "${context.getString(R.string.building_label)}: ${session.building}",
                style = TextStyle(
                    fontSize = 11.sp, 
                    color = if (session.isException) GlanceTheme.colors.onPrimaryContainer else GlanceTheme.colors.onSurfaceVariant
                )
            )

            Spacer(modifier = GlanceModifier.defaultWeight())

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val prevAction = actionRunCallback<NavigateAction>(
                    actionParametersOf(NavigateAction.PARAM_FORWARD to false)
                )
                val nextAction = actionRunCallback<NavigateAction>(
                    actionParametersOf(NavigateAction.PARAM_FORWARD to true)
                )

                Text(
                    text = session.start,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold, 
                        fontSize = 14.sp, 
                        color = if (session.isException) GlanceTheme.colors.onPrimaryContainer else GlanceTheme.colors.primary
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_chevron_left),
                        contentDescription = "Prev",
                        modifier = GlanceModifier.size(24.dp).clickable(prevAction),
                        colorFilter = ColorFilter.tint(
                            if (session.isException) GlanceTheme.colors.onPrimaryContainer else GlanceTheme.colors.primary
                        )
                    )
                    Text(
                        text = "${index + 1}/$total",
                        style = TextStyle(
                            fontSize = 10.sp, 
                            color = if (session.isException) GlanceTheme.colors.onPrimaryContainer else GlanceTheme.colors.onSurfaceVariant
                        ),
                        modifier = GlanceModifier.padding(horizontal = 4.dp)
                    )
                    Image(
                        provider = ImageProvider(R.drawable.ic_chevron_right),
                        contentDescription = "Next",
                        modifier = GlanceModifier.size(24.dp).clickable(nextAction),
                        colorFilter = ColorFilter.tint(
                            if (session.isException) GlanceTheme.colors.onPrimaryContainer else GlanceTheme.colors.primary
                        )
                    )
                }
            }
        }
    }
}

class NavigateAction : ActionCallback {
    companion object {
        val PARAM_FORWARD = ActionParameters.Key<Boolean>("forward")
    }

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val forward = parameters[PARAM_FORWARD] ?: true
        val repository = ScheduleRepository(context)
        val scheduleSize = repository.getEffectiveSchedule(LocalDate.now()).size
        
        if (scheduleSize == 0) return

        updateAppWidgetState(context, glanceId) { prefs ->
            val currentIndex = prefs[ClassDetailWidget.KEY_INDEX] ?: 0
            var nextIndex = if (forward) currentIndex + 1 else currentIndex - 1
            
            if (nextIndex < 0) nextIndex = scheduleSize - 1
            if (nextIndex >= scheduleSize) nextIndex = 0
            
            prefs[ClassDetailWidget.KEY_INDEX] = nextIndex
        }
        ClassDetailWidget().update(context, glanceId)
    }
}

class ClassDetailWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClassDetailWidget()
}
