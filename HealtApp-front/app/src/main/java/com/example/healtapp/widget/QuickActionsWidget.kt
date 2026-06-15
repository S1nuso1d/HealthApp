package com.example.healtapp.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import com.example.healtapp.MainActivity
import com.example.healtapp.features.hydration.widget.AddWaterAction
import com.example.healtapp.features.hydration.widget.WaterAmountKey
import com.example.healtapp.notifications.HealthNotificationHelper

class QuickActionsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                QuickActionsWidgetContent(
                    openDashboard = openRoute(context, "dashboard"),
                    openNutrition = openRoute(context, "nutrition"),
                    openActivity = openRoute(context, "activity"),
                )
            }
        }
    }
}

private fun openRoute(context: Context, route: String): Action =
    androidx.glance.appwidget.action.actionStartActivity(
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(HealthNotificationHelper.EXTRA_NAV_ROUTE, route)
        },
    )

@Composable
private fun QuickActionsWidgetContent(
    openDashboard: Action,
    openNutrition: Action,
    openActivity: Action,
) {
    WidgetSurface {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(text = "Быстрые действия", style = WidgetTextStyles.title)
            Spacer(GlanceModifier.height(8.dp))
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                WidgetActionButton(
                    label = "+200",
                    onClick = waterAction(200),
                    modifier = GlanceModifier.defaultWeight().padding(end = 4.dp),
                )
                WidgetActionButton(
                    label = "+250",
                    onClick = waterAction(250),
                    modifier = GlanceModifier.defaultWeight().padding(horizontal = 4.dp),
                )
                WidgetActionButton(
                    label = "+500",
                    onClick = waterAction(500),
                    modifier = GlanceModifier.defaultWeight().padding(start = 4.dp),
                )
            }
            Spacer(GlanceModifier.height(8.dp))
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                WidgetQuickChip(
                    label = "Еда",
                    onClick = openNutrition,
                    modifier = GlanceModifier.defaultWeight().padding(end = 4.dp),
                )
                WidgetQuickChip(
                    label = "Шаги",
                    onClick = openActivity,
                    modifier = GlanceModifier.defaultWeight().padding(horizontal = 4.dp),
                )
                WidgetQuickChip(
                    label = "Сводка",
                    onClick = openDashboard,
                    modifier = GlanceModifier.defaultWeight().padding(start = 4.dp),
                )
            }
        }
    }
}

private fun waterAction(amount: Int): Action =
    actionRunCallback<AddWaterAction>(
        actionParametersOf(WaterAmountKey to amount),
    )

class QuickActionsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickActionsWidget()
}
