package com.example.healtapp.features.recommendations.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppButton

@Composable
fun RecommendationFilterBar(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppButton(
            text = "Все",
            onClick = { onFilterSelected("all") },
            enabled = selectedFilter != "all"
        )
        AppButton(
            text = "Сон",
            onClick = { onFilterSelected("sleep") },
            enabled = selectedFilter != "sleep"
        )
        AppButton(
            text = "Вода",
            onClick = { onFilterSelected("hydration") },
            enabled = selectedFilter != "hydration"
        )
        AppButton(
            text = "Питание",
            onClick = { onFilterSelected("nutrition") },
            enabled = selectedFilter != "nutrition"
        )
        AppButton(
            text = "Активность",
            onClick = { onFilterSelected("activity") },
            enabled = selectedFilter != "activity"
        )
    }
}