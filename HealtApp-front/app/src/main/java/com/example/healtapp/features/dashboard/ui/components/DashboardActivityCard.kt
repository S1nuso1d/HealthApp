package com.example.healtapp.features.dashboard.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.runtime.Composable
import com.example.healtapp.core.ui.theme.CardBlue

@Composable
fun DashboardActivityCard(
    stepsToday: Int,
    activityMinutesToday: Int,
    caloriesBurnedToday: Int
) {
    DashboardSectionCard(
        title = "Активность",
        value = "$stepsToday шагов",
        subtitle = "$activityMinutesToday мин • $caloriesBurnedToday ккал",
        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
        iconBackground = listOf(
            CardBlue.copy(alpha = 0.95f),
            CardBlue.copy(alpha = 0.5f),
        ),
    )
}