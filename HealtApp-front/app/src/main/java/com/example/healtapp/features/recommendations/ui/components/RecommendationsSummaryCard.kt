package com.example.healtapp.features.recommendations.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppCard

@Composable
fun RecommendationsSummaryCard(
    summaryText: String
) {
    AppCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Сводка рекомендаций",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}