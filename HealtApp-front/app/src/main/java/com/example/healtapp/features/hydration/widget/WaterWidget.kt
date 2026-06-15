package com.example.healtapp.features.hydration.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
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
import com.example.healtapp.data.preferences.WidgetSnapshotStore
import com.example.healtapp.notifications.HealthNotificationHelper
import com.example.healtapp.widget.WidgetHydrationActions
import com.example.healtapp.widget.WidgetProgressBar
import com.example.healtapp.widget.WidgetQuickChip
import com.example.healtapp.widget.WidgetSurface
import com.example.healtapp.widget.WidgetTextStyles
import com.example.healtapp.widget.widgetProgressFraction
import kotlin.math.roundToInt

val WaterAmountKey = ActionParameters.Key<Int>("water_amount_ml")

class WaterWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetSnapshotStore(context).load()
        val waterMl = snapshot?.waterMl ?: 0
        val waterGoalMl = snapshot?.waterGoalMl ?: 2500

        val openHydration = actionStartActivity(context)

        provideContent {
            GlanceTheme {
                WaterWidgetContent(
                    waterMl = waterMl,
                    waterGoalMl = waterGoalMl,
                    openHydration = openHydration,
                )
            }
        }
    }
}

private fun actionStartActivity(context: Context): Action =
    androidx.glance.appwidget.action.actionStartActivity(
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(HealthNotificationHelper.EXTRA_NAV_ROUTE, "hydration")
        },
    )

@Composable
fun WaterWidgetContent(
    waterMl: Int,
    waterGoalMl: Int,
    openHydration: Action,
) {
    val waterPercent = (widgetProgressFraction(waterMl, waterGoalMl) * 100).roundToInt()

    WidgetSurface {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .clickable(openHydration)
                .padding(14.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "Вода сегодня", style = WidgetTextStyles.sectionLabel)
            Spacer(GlanceModifier.height(6.dp))
            Text(text = "$waterMl мл", style = WidgetTextStyles.waterHero)
            Text(
                text = "$waterPercent% · цель $waterGoalMl мл",
                style = WidgetTextStyles.metricSub,
            )
            Spacer(GlanceModifier.height(10.dp))
            WidgetProgressBar(
                progress = widgetProgressFraction(waterMl, waterGoalMl),
                modifier = GlanceModifier.fillMaxWidth(),
            )
            Spacer(GlanceModifier.height(12.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            ) {
                listOf(200, 250, 500).forEach { amount ->
                    WidgetQuickChip(
                        label = "+$amount",
                        onClick = actionRunCallback<AddWaterAction>(
                            actionParametersOf(WaterAmountKey to amount),
                        ),
                        modifier = GlanceModifier.defaultWeight().padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }
}

class AddWaterAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val amount = parameters[WaterAmountKey] ?: 250
        WidgetHydrationActions.addWater(context, amount)
    }
}
