package com.example.healtapp.features.dashboard.ui.components

import androidx.compose.runtime.Composable

@Composable
fun ActivityPreviewCard(steps: Int) {
    HealthSummaryCard(
        title = "Активность",
        value = "$steps шагов",
        subtitle = "Сегодняшняя активность"
    )
}