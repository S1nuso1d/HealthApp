package com.example.healtapp.features.dashboard.ui.components

import androidx.compose.runtime.Composable

@Composable
fun DashboardActivityCard(
    stepsToday: Int,
    activityMinutesToday: Int,
    caloriesBurnedToday: Int
) {
    DashboardSectionCard(
        title = "Активность",
        value = "$stepsToday шагов",
        subtitle = "$activityMinutesToday мин • $caloriesBurnedToday ккал"
    )
}