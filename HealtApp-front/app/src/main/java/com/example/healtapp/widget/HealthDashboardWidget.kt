package com.example.healtapp.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
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
import com.example.healtapp.data.preferences.DashboardCache

private fun widgetColor(hex: Long) = ColorProvider(day = Color(hex), night = Color(hex))

private val WidgetBg = widgetColor(0xFF0A0A0A)
private val WidgetMuted = widgetColor(0xFF9CA3AF)
private val WidgetText = widgetColor(0xFFF5F5F5)
private val WidgetSubtle = widgetColor(0xFF737373)
private val WidgetAccent = widgetColor(0xFFB8B8B8)

class HealthDashboardWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val home = DashboardCache(context).load()
        val summary = home?.analytics?.summary
        val healthScore = summary?.healthScore ?: 0
        val hydration = summary?.hydrationScore ?: 0
        val activity = summary?.activityScore ?: 0
        val briefTitle = home?.dailyBrief?.title ?: "HealthApp"

        provideContent {
            GlanceTheme {
                WidgetContent(
                    healthScore = healthScore,
                    hydrationScore = hydration,
                    activityScore = activity,
                    briefTitle = briefTitle,
                )
            }
        }
    }
}

@Composable
private fun WidgetContent(
    healthScore: Int,
    hydrationScore: Int,
    activityScore: Int,
    briefTitle: String,
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
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = WidgetMuted),
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = "$healthScore",
            style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, color = WidgetText),
        )
        Text(
            text = "Health score",
            style = TextStyle(fontSize = 11.sp, color = WidgetSubtle),
        )
        Spacer(GlanceModifier.height(8.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            MetricChip("Вода", hydrationScore)
            MetricChip("Актив.", activityScore)
        }
        Spacer(GlanceModifier.height(6.dp))
        Text(
            text = briefTitle,
            style = TextStyle(fontSize = 10.sp, color = WidgetAccent),
            maxLines = 2,
        )
    }
}

@Composable
private fun MetricChip(label: String, value: Int) {
    Column {
        Text(text = label, style = TextStyle(fontSize = 10.sp, color = WidgetSubtle))
        Text(text = "$value", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = WidgetText))
    }
}

class HealthDashboardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HealthDashboardWidget()
}
