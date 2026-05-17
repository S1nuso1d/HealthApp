package com.example.healtapp.features.dashboard.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WaterDrop
import com.example.healtapp.core.ui.theme.metricIconGradient
import com.example.healtapp.core.ui.theme.themedCardBlue
import com.example.healtapp.core.ui.theme.themedCardLavender
import com.example.healtapp.core.ui.theme.themedCardMint
import java.text.DecimalFormat
import kotlin.math.roundToInt

@Composable
fun DashboardMetricsGrid(
    sleepHours: Float,
    sleepTargetHours: Float,
    sleepQuality: String,
    waterMl: Int,
    waterTargetMl: Int,
    caloriesToday: Int,
    caloriesTarget: Int,
    caffeineToday: Int,
    stepsToday: Int,
    stepsGoal: Int,
    activityMinutesToday: Int,
    onOpenSleep: () -> Unit,
    onOpenHydration: () -> Unit,
    onOpenNutrition: () -> Unit,
    onOpenActivity: () -> Unit,
) {
    val sleepFmt = DecimalFormat("#.#").format(sleepHours).replace('.', ',')
    val sleepTargetFmt = DecimalFormat("#.#").format(sleepTargetHours).replace('.', ',')
    val sleepProgress = if (sleepTargetHours > 0f) sleepHours / sleepTargetHours else 0f
    val waterProgress = if (waterTargetMl > 0) waterMl.toFloat() / waterTargetMl else 0f
    val calProgress = if (caloriesTarget > 0) caloriesToday.toFloat() / caloriesTarget else 0f
    val stepsProgress = if (stepsGoal > 0) stepsToday.toFloat() / stepsGoal else 0f

    val waterStr = "%,d".format(waterMl).replace(',', '\u00A0')
    val waterGoalStr = "%,d".format(waterTargetMl).replace(',', '\u00A0')
    val stepsStr = "%,d".format(stepsToday).replace(',', '\u00A0')

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DashboardMetricTile(
                title = "Сон",
                value = if (sleepHours > 0f) "$sleepFmt ч" else "—",
                progress = sleepProgress,
                progressLabel = if (sleepHours > 0f) {
                    "Качество $sleepQuality/100"
                } else {
                    "Запишите ночь"
                },
                icon = Icons.Filled.Bedtime,
                iconGradient = metricIconGradient(themedCardLavender()),
                onClick = onOpenSleep,
                modifier = Modifier.weight(1f),
            )
            DashboardMetricTile(
                title = "Вода",
                value = "$waterStr мл",
                progress = waterProgress,
                progressLabel = "из $waterGoalStr мл",
                icon = Icons.Filled.WaterDrop,
                iconGradient = metricIconGradient(themedCardBlue()),
                onClick = onOpenHydration,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DashboardMetricTile(
                title = "Питание",
                value = "$caloriesToday",
                progress = calProgress,
                progressLabel = "ккал · цель $caloriesTarget",
                icon = Icons.Filled.Restaurant,
                iconGradient = metricIconGradient(themedCardMint(), mintTint = true),
                onClick = onOpenNutrition,
                modifier = Modifier.weight(1f),
            )
            DashboardMetricTile(
                title = "Шаги",
                value = stepsStr,
                progress = stepsProgress,
                progressLabel = buildString {
                    val pct = if (stepsGoal > 0) ((stepsToday * 100f) / stepsGoal).roundToInt() else 0
                    append("$pct% · $activityMinutesToday мин спорт")
                },
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                iconGradient = metricIconGradient(themedCardBlue()),
                onClick = onOpenActivity,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
