package com.example.healtapp.features.dashboard.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.ProgressRing

@Composable
fun DashboardWaterCard(
    waterMl: Int,
    waterTargetMl: Int
) {
    val progress = (waterMl.toFloat() / waterTargetMl.toFloat()).coerceIn(0f, 1f)

    AppCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Вода",
                style = MaterialTheme.typography.titleMedium
            )
            ProgressRing(
                progress = progress,
                text = "$waterMl"
            )
            Text(
                text = "Цель: $waterTargetMl мл",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}