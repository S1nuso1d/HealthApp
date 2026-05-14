package com.example.healtapp.features.recommendations.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.features.recommendations.presentation.RecommendationUiItem

@Composable
fun RecommendationCard(
    item: RecommendationUiItem
) {
    AppCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Категория: ${item.category}",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Приоритет: ${item.priority}",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Статус: ${item.status}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            item.confidence?.let { confidence ->
                Text(
                    text = "Уверенность: ${(confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            item.progressLabel?.let { progress ->
                Text(
                    text = progress,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium
            )

            item.action?.let { actionText ->
                Text(
                    text = "Что сделать: $actionText",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            item.personalizedTip?.let { tip ->
                Text(
                    text = "AI-совет: $tip",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}