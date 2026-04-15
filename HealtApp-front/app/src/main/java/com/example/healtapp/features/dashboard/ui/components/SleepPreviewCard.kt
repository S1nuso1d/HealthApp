package com.example.healtapp.features.dashboard.ui.components

import androidx.compose.runtime.Composable

@Composable
fun SleepPreviewCard(hours: Float) {
    HealthSummaryCard(
        title = "Сон",
        value = "$hours ч",
        subtitle = "Средняя длительность сна"
    )
}