package com.example.healtapp.features.dashboard.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import java.text.DecimalFormat

private val oneDecimalFormat = DecimalFormat("#.#")

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
    caloriesBurnedToday: Int,
    caloriesBurnGoal: Int,
    onOpenSleep: () -> Unit,
    onOpenHydration: () -> Unit,
    onOpenNutrition: () -> Unit,
    onOpenActivity: () -> Unit,
    compact: Boolean = false,
    isEvening: Boolean = false,
) {
    val sleepFmt = oneDecimalFormat.format(sleepHours).replace('.', ',')
    val sleepProgress = if (sleepTargetHours > 0f) sleepHours / sleepTargetHours else 0f
    val waterProgress = if (waterTargetMl > 0) waterMl.toFloat() / waterTargetMl else 0f
    val calProgress = if (caloriesTarget > 0) caloriesToday.toFloat() / caloriesTarget else 0f
    val stepsProgress = if (stepsGoal > 0) stepsToday.toFloat() / stepsGoal else 0f

    val sleepMet = sleepHours > 0f && sleepHours >= sleepTargetHours - 0.05f
    val waterMet = waterTargetMl > 0 && waterMl >= waterTargetMl
    val foodMet = caloriesTarget > 0 && caloriesToday >= (caloriesTarget * 0.98f).toInt()
    val stepsMet = stepsGoal > 0 && stepsToday >= stepsGoal

    Column(
        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DashboardMetricTile(
                title = "Сон",
                value = if (sleepHours > 0f) "$sleepFmt ч" else "—",
                progress = sleepProgress,
                progressLabel = sleepRemainderLabel(sleepHours, sleepTargetHours, sleepQuality),
                icon = Icons.Filled.Bedtime,
                iconGradient = metricIconGradient(themedCardBlue()),
                onClick = onOpenSleep,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                goalMet = sleepMet,
                compact = compact,
            )
            DashboardMetricTile(
                title = "Вода",
                value = formatInt(waterMl) + " мл",
                progress = waterProgress,
                progressLabel = waterRemainderLabel(waterMl, waterTargetMl, isEvening),
                icon = Icons.Filled.WaterDrop,
                iconGradient = metricIconGradient(themedCardBlue()),
                onClick = onOpenHydration,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                goalMet = waterMet,
                compact = compact,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DashboardMetricTile(
                title = "Питание",
                value = if (caloriesToday > 0) "$caloriesToday" else "—",
                progress = calProgress,
                progressLabel = foodRemainderLabel(caloriesToday, caloriesTarget, caffeineToday),
                icon = Icons.Filled.Restaurant,
                iconGradient = metricIconGradient(themedCardBlue()),
                onClick = onOpenNutrition,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                goalMet = foodMet,
                compact = compact,
            )
            DashboardMetricTile(
                title = "Шаги",
                value = formatInt(stepsToday),
                progress = stepsProgress,
                progressLabel = stepsRemainderLabel(
                    stepsToday,
                    stepsGoal,
                    activityMinutesToday,
                    caloriesBurnedToday,
                    caloriesBurnGoal,
                ),
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                iconGradient = metricIconGradient(themedCardBlue()),
                onClick = onOpenActivity,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                goalMet = stepsMet,
                compact = compact,
            )
        }
    }
}

private fun formatInt(value: Int): String = "%,d".format(value).replace(',', '\u00A0')

private fun sleepRemainderLabel(hours: Float, target: Float, quality: String): String {
    if (hours <= 0f) return "Ночь не записана"
    if (target > 0f && hours >= target - 0.05f) return "Сон закрыт"
    if (target > 0f && hours < target) {
        val left = oneDecimalFormat.format(target - hours).replace('.', ',')
        return "ещё $left ч"
    }
    return "Качество $quality/100"
}

private fun waterRemainderLabel(current: Int, target: Int, isEvening: Boolean): String {
    if (target <= 0) return "из ${formatInt(current)} мл"
    if (current >= target) return "вода закрыта"
    val left = target - current
    return if (isEvening) "ещё ${formatInt(left)} мл · без спешки" else "ещё ${formatInt(left)} мл"
}

private fun intRemainderLabel(current: Int, target: Int, unit: String, closed: String): String {
    if (target <= 0) return "из ${formatInt(current)} $unit"
    if (current >= target) return closed
    return "ещё ${formatInt(target - current)} $unit"
}

private fun foodRemainderLabel(calories: Int, target: Int, caffeine: Int): String {
    if (calories <= 0) {
        val hour = java.time.LocalTime.now().hour
        return if (hour < 12) "Завтрак до полудня" else "Еда ещё не записана"
    }
    val base = intRemainderLabel(calories, target, "ккал", "еда закрыта")
    return if (caffeine > 0) "$base · кофеин $caffeine" else base
}

private fun stepsRemainderLabel(
    steps: Int,
    goal: Int,
    minutes: Int,
    burned: Int,
    burnGoal: Int,
): String {
    val base = if (steps <= 400 && java.time.LocalTime.now().hour < 11) {
        "Утренние шаги"
    } else if (goal <= 0) {
        formatInt(steps)
    } else if (steps >= goal) {
        "шаги закрыты"
    } else {
        "ещё ${formatInt(goal - steps)}"
    }
    val burnHint = burnGoal.takeIf { it > 0 }
    val extras = buildList {
        if (minutes > 0) add("$minutes мин")
        if (burned > 0) add("$burned ккал")
        if (burnHint != null && burned <= 0) add("цель $burnHint")
    }
    return if (extras.isEmpty()) base else "$base · ${extras.joinToString(" · ")}"
}
