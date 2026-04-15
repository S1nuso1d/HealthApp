package com.example.healtapp.features.activity.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.healtapp.core.ui.components.AppCard

@Composable
fun ActivityInsightCard(
    text: String
) {
    AppCard {
        Text(
            text = "Инсайт",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}