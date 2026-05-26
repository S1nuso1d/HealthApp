package com.example.healtapp.widget



import android.content.Context

import android.content.Intent

import androidx.compose.runtime.Composable

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import androidx.glance.GlanceId

import androidx.glance.GlanceModifier

import androidx.glance.GlanceTheme

import androidx.glance.action.clickable

import androidx.glance.appwidget.GlanceAppWidget

import androidx.glance.appwidget.GlanceAppWidgetReceiver

import androidx.glance.appwidget.action.actionStartActivity

import androidx.glance.appwidget.provideContent

import androidx.glance.background

import androidx.glance.color.ColorProvider

import androidx.glance.layout.Alignment

import androidx.glance.layout.Column

import androidx.glance.layout.Row

import androidx.glance.layout.Spacer

import androidx.glance.layout.fillMaxSize

import androidx.glance.layout.fillMaxWidth

import androidx.glance.layout.height

import androidx.glance.layout.padding

import androidx.glance.text.FontWeight

import androidx.glance.text.Text

import androidx.glance.text.TextStyle

import com.example.healtapp.MainActivity

import com.example.healtapp.data.preferences.DashboardCache

import com.example.healtapp.data.preferences.WidgetSnapshotStore

import com.example.healtapp.notifications.HealthNotificationHelper



private fun widgetColor(hex: Long) = ColorProvider(day = Color(hex), night = Color(hex))



private val WidgetBg = widgetColor(0xFFF8FCFF)

private val WidgetMuted = widgetColor(0xFF5A6B7D)

private val WidgetText = widgetColor(0xFF16324F)

private val WidgetAccent = widgetColor(0xFF4BA3E3)



class HealthDashboardWidget : GlanceAppWidget() {



    override suspend fun provideGlance(context: Context, id: GlanceId) {

        val snapshot = WidgetSnapshotStore(context).load()

        val home = DashboardCache(context).load()

        val stepsToday = snapshot?.stepsToday ?: 0
        val stepsGoal = snapshot?.stepsGoal ?: 10_000
        val waterMl = snapshot?.waterMl ?: 0
        val waterGoal = snapshot?.waterGoalMl ?: 2500
        val stepsPct = if (stepsGoal > 0) ((stepsToday * 100) / stepsGoal).coerceIn(0, 999) else 0
        val waterPct = if (waterGoal > 0) ((waterMl * 100) / waterGoal).coerceIn(0, 999) else 0

        val briefTitle = home?.dailyBrief?.title ?: "HealthApp"

        val openActivity = actionStartActivity(

            Intent(context, MainActivity::class.java).apply {

                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

                putExtra(HealthNotificationHelper.EXTRA_NAV_ROUTE, "activity")

            },

        )

        val openHydration = actionStartActivity(

            Intent(context, MainActivity::class.java).apply {

                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

                putExtra(HealthNotificationHelper.EXTRA_NAV_ROUTE, "hydration")

            },

        )



        provideContent {

            GlanceTheme {

                WidgetContent(

                    stepsToday = stepsToday,

                    stepsGoal = stepsGoal,
                    stepsPercent = stepsPct,
                    waterMl = waterMl,
                    waterGoalMl = waterGoal,
                    waterPercent = waterPct,
                    briefTitle = briefTitle,

                    openActivity = openActivity,

                    openHydration = openHydration,

                )

            }

        }

    }

}



@Composable

private fun WidgetContent(

    stepsToday: Int,

    stepsGoal: Int,
    stepsPercent: Int,
    waterMl: Int,
    waterGoalMl: Int,
    waterPercent: Int,
    briefTitle: String,

    openActivity: androidx.glance.action.Action,

    openHydration: androidx.glance.action.Action,

) {

    Column(

        modifier = GlanceModifier

            .fillMaxSize()

            .background(WidgetBg)

            .padding(12.dp),

        verticalAlignment = Alignment.Vertical.Top,

    ) {

        Text(

            text = "HealthApp",

            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = WidgetMuted),

        )

        Spacer(GlanceModifier.height(6.dp))

        Row(modifier = GlanceModifier.fillMaxWidth()) {

            MetricBlock(

                label = "Шаги",

                value = "%,d".format(stepsToday).replace(',', ' '),

                sub = "$stepsPercent% · из ${"%,d".format(stepsGoal).replace(',', ' ')}",

                modifier = GlanceModifier.defaultWeight().clickable(openActivity),

            )

            MetricBlock(

                label = "Вода",

                value = "$waterMl мл",

                sub = "$waterPercent% · из $waterGoalMl",

                modifier = GlanceModifier.defaultWeight().clickable(openHydration),

            )

        }

        Spacer(GlanceModifier.height(8.dp))

        Text(

            text = briefTitle,

            style = TextStyle(fontSize = 10.sp, color = WidgetAccent),

            maxLines = 2,

        )

    }

}



@Composable

private fun MetricBlock(

    label: String,

    value: String,

    sub: String,

    modifier: GlanceModifier = GlanceModifier,

) {

    Column(modifier = modifier) {

        Text(text = label, style = TextStyle(fontSize = 10.sp, color = WidgetMuted))

        Text(text = value, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WidgetText))

        Text(text = sub, style = TextStyle(fontSize = 9.sp, color = WidgetMuted))

    }

}



class HealthDashboardWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = HealthDashboardWidget()

}

