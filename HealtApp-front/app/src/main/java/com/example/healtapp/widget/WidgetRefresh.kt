package com.example.healtapp.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

suspend fun refreshHealthWidget(context: Context) {
    runCatching { HealthDashboardWidget().updateAll(context) }
}
