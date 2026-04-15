package com.example.healtapp.features.timeline.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppCard

@Composable
fun TimelineSummaryCard(
    selectedDate: String,
    summaryText: String
) {
    AppCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = selectedDate,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}