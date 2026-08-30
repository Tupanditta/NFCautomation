package com.example.nfcautomation.utils

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.example.nfcautomation.ui.widgets.ClassDetailWidget
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

object WidgetUtils {
    fun refreshWidgets(context: Context) {
        MainScope().launch {
            ClassDetailWidget().updateAll(context)
        }
    }
}
