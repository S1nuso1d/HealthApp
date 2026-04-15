package com.example.healtapp.features.hydration.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.ProgressRing

@Composable
fun HydrationSummaryCard(
    totalTodayMl: Int,
    targetTodayMl: Int,
    waterOnlyMl: Int
) {
    val progress = (totalTodayMl.toFloat() / targetTodayMl.toFloat()).coerceIn(0f, 1f)

    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProgressRing(
                progress = progress,
                text = "${totalTodayMl / 1000f}л"
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Гидратация",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Цель: ${targetTodayMl} мл",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Чистая вода: ${waterOnlyMl} мл",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}