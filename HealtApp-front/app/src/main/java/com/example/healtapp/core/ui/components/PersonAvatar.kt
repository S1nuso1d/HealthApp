package com.example.healtapp.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.theme.brandingGradient

@Composable
fun PersonAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    useGradient: Boolean = false,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (useGradient) {
                    Modifier.background(Brush.linearGradient(brandingGradient().map { it.copy(alpha = 0.55f) }))
                } else {
                    Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.take(1).uppercase(),
            fontWeight = FontWeight.Bold,
            color = if (useGradient) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
            style = if (size >= 48.dp) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
        )
    }
}
