package com.example.healtapp.features.dashboard.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.healtapp.core.ui.components.AppCard

@Composable
fun HealthSummaryCard(title: String, value: String, subtitle: String? = null) {
    AppCard {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall
        )
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}