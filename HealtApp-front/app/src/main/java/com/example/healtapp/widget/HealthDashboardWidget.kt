package com.example.healtapp.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import com.example.healtapp.MainActivity
import com.example.healtapp.data.preferences.DashboardCache
import com.example.healtapp.data.preferences.WidgetSnapshotStore
import com.example.healtapp.notifications.HealthNotificationHelper
import kotlin.math.roundToInt

class HealthDashboardWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetSnapshotStore(context).load()
        val home = DashboardCache(context).load()

        val data = snapshot ?: com.example.healtapp.data.preferences.WidgetSnapshot()
        val briefTitle = data.briefTitle.ifBlank {
            home?.dailyBrief?.title.orEmpty()
        }.ifBlank { "Сводка за сегодня" }

        val openDashboard = openRoute(context, "dashboard")
        val openActivity = openRoute(context, "activity")
        val openHydration = openRoute(context, "hydration")
        val openNutrition = openRoute(context, "nutrition")

        provideContent {
            GlanceTheme {
                HealthDashboardWidgetContent(
                    snapshot = data,
                    briefTitle = briefTitle,
                    openDashboard = openDashboard,
                    openActivity = openActivity,
                    openHydration = openHydration,
                    openNutrition = openNutrition,
                )
            }
        }
    }
}

private fun openRoute(context: Context, route: String): Action =
    actionStartActivity(
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(HealthNotificationHelper.EXTRA_NAV_ROUTE, route)
        },
    )

@Composable
private fun HealthDashboardWidgetContent(
    snapshot: com.example.healtapp.data.preferences.WidgetSnapshot,
    briefTitle: String,
    openDashboard: Action,
    openActivity: Action,
    openHydration: Action,
    openNutrition: Action,
) {
    val sleepLabel = if (snapshot.sleepHours > 0f) {
        "%.1f ч".format(snapshot.sleepHours)
    } else {
        "—"
    }
    val sleepPct = (widgetProgressFraction(snapshot.sleepHours, snapshot.sleepTargetHours) * 100).roundToInt()
    val waterPct = (widgetProgressFraction(snapshot.waterMl, snapshot.waterGoalMl) * 100).roundToInt()
    val stepsPct = (widgetProgressFraction(snapshot.stepsToday, snapshot.stepsGoal) * 100).roundToInt()
    val calPct = (widgetProgressFraction(snapshot.caloriesToday, snapshot.caloriesTarget) * 100).roundToInt()

    WidgetSurface(modifier = GlanceModifier.clickable(openDashboard)) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            WidgetHeroHeader(
                healthScore = snapshot.healthScore,
                briefTitle = briefTitle,
            )
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                WidgetMetricRow(
                    label = "Сон",
                    value = sleepLabel,
                    subLabel = "$sleepPct% · цель ${snapshot.sleepTargetHours.roundToInt()} ч",
                    progress = widgetProgressFraction(snapshot.sleepHours, snapshot.sleepTargetHours),
                    onClick = openDashboard,
                )
                WidgetMetricRow(
                    label = "Вода",
                    value = "${snapshot.waterMl} мл",
                    subLabel = "$waterPct% · из ${snapshot.waterGoalMl} мл",
                    progress = widgetProgressFraction(snapshot.waterMl, snapshot.waterGoalMl),
                    onClick = openHydration,
                )
                WidgetMetricRow(
                    label = "Шаги",
                    value = formatSteps(snapshot.stepsToday),
                    subLabel = "$stepsPct% · из ${formatSteps(snapshot.stepsGoal)}",
                    progress = widgetProgressFraction(snapshot.stepsToday, snapshot.stepsGoal),
                    onClick = openActivity,
                )
                WidgetMetricRow(
                    label = "Калории",
                    value = "${snapshot.caloriesToday} ккал",
                    subLabel = "$calPct% · из ${snapshot.caloriesTarget} ккал",
                    progress = widgetProgressFraction(snapshot.caloriesToday, snapshot.caloriesTarget),
                    onClick = openNutrition,
                )
            }
        }
    }
}

class HealthDashboardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HealthDashboardWidget()
}
