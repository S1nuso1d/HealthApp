package com.example.healtapp.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class AppMessageType {
    Error,
    Warning,
    Info,
    Success,
}

@Composable
fun AppMessageBanner(
    text: String,
    modifier: Modifier = Modifier,
    type: AppMessageType = AppMessageType.Info,
    title: String? = null,
) {
    val (container, content, icon) = styleFor(type)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = container,
        border = BorderStroke(1.dp, content.copy(alpha = 0.22f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(22.dp),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                title?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = content,
                    )
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = content,
                )
            }
        }
    }
}

@Composable
fun AppDialogMessage(
    body: String,
    modifier: Modifier = Modifier,
    warning: String? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        warning?.let {
            AppMessageBanner(
                text = it,
                type = AppMessageType.Warning,
                title = "Внимание",
            )
        }
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun styleFor(type: AppMessageType): Triple<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color, ImageVector> {
    val scheme = MaterialTheme.colorScheme
    return when (type) {
        AppMessageType.Error -> Triple(
            scheme.errorContainer,
            scheme.onErrorContainer,
            Icons.Outlined.ErrorOutline,
        )
        AppMessageType.Warning -> Triple(
            scheme.secondaryContainer.copy(alpha = 0.65f),
            Color(0xFF8B5A00),
            Icons.Outlined.WarningAmber,
        )
        AppMessageType.Success -> Triple(
            scheme.primaryContainer,
            scheme.onPrimaryContainer,
            Icons.Filled.CheckCircle,
        )
        AppMessageType.Info -> Triple(
            scheme.surfaceVariant,
            scheme.onSurfaceVariant,
            Icons.Filled.Info,
        )
    }
}
