package com.example.healtapp.features.sleep.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.ProgressRing

@Composable
fun SleepSummaryCard(
    averageSleepHours: Float,
    targetSleepHours: Float,
    sleepQualityAverage: Int
) {
    val progress = (averageSleepHours / targetSleepHours).coerceIn(0f, 1f)

    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProgressRing(
                progress = progress,
                text = "${averageSleepHours}ч"
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Сон за неделю",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Цель: ${targetSleepHours} ч",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Среднее качество: $sleepQualityAverage/100",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}