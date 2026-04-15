package com.example.healtapp.features.timeline.ui.components

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
import com.example.healtapp.features.timeline.presentation.TimelineEventUi

@Composable
fun TimelineEventItem(
    event: TimelineEventUi
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
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = event.time,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Тип: ${event.type}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}