package com.example.healtapp.features.timeline.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun TimelineDayCard(
    date: String
) {
    Text(
        text = date,
        style = MaterialTheme.typography.titleLarge
    )
}