package com.example.healtapp.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.theme.accentColor
import com.example.healtapp.core.ui.theme.contentPrimaryColor

@Composable
fun ProgressRing(progress: Float, text: String) {
    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(72.dp),
            color = accentColor(),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = contentPrimaryColor(),
        )
    }
}
