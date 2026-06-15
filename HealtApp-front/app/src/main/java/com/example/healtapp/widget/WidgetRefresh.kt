package com.example.healtapp.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.example.healtapp.features.hydration.widget.WaterWidget

suspend fun refreshAllWidgets(context: Context) {
    runCatching { HealthDashboardWidget().updateAll(context) }
    runCatching { WaterWidget().updateAll(context) }
    runCatching { QuickActionsWidget().updateAll(context) }
}

/** @deprecated Используйте [refreshAllWidgets]. */
suspend fun refreshHealthWidget(context: Context) = refreshAllWidgets(context)
