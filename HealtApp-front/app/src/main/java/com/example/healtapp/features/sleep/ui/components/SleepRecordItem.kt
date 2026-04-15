package com.example.healtapp.features.sleep.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.features.sleep.presentation.SleepRecordUi

@Composable
fun SleepRecordItem(
    record: SleepRecordUi
) {
    AppCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = record.date,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${record.qualityScore}/100",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = "${record.startTime} → ${record.endTime}",
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Длительность: ${record.durationHours} ч",
                style = MaterialTheme.typography.bodyMedium
            )

            if (record.note.isNotBlank()) {
                Text(
                    text = record.note,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}