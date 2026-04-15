package com.example.healtapp.features.dashboard.ui.components

import androidx.compose.runtime.Composable

@Composable
fun DashboardSleepCard(
    sleepHours: Float,
    sleepTargetHours: Float,
    sleepQuality: String
) {
    DashboardSectionCard(
        title = "Сон",
        value = "$sleepHours / $sleepTargetHours ч",
        subtitle = "Среднее качество: $sleepQuality/100"
    )
}