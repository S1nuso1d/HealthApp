package com.example.healtapp.features.dashboard.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.runtime.Composable
import com.example.healtapp.core.ui.theme.cardHeaderGradientMuted
import com.example.healtapp.core.ui.theme.themedCardBlue

@Composable
fun DashboardActivityCard(
    stepsToday: Int,
    stepsGoal: Int,
    activityMinutesToday: Int,
    caloriesBurnedToday: Int,
    onClick: (() -> Unit)? = null,
) {
    val stepsFormatted = "%,d".format(stepsToday).replace(',', '\u00A0')
    val goalFormatted = "%,d".format(stepsGoal).replace(',', '\u00A0')
    val percent = if (stepsGoal > 0) ((stepsToday * 100) / stepsGoal).coerceAtMost(999) else 0

    DashboardSectionCard(
        title = "Активность",
        value = "$stepsFormatted шагов",
        subtitle = buildString {
            append("$percent% цели · $activityMinutesToday мин · $caloriesBurnedToday ккал")
            if (stepsGoal > 0) append(" · цель $goalFormatted")
        },
        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
        iconBackground = cardHeaderGradientMuted(themedCardBlue()),
        onClick = onClick,
    )
}
