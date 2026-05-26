package com.example.healtapp.features.dashboard.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.animation.AppMotion
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.theme.brandingGradient
import com.example.healtapp.core.ui.theme.contentPrimaryColor
import com.example.healtapp.core.ui.theme.contentSecondaryColor
import com.example.healtapp.core.ui.theme.iconTintColor

@Composable
fun DashboardMetricTile(
    title: String,
    value: String,
    progress: Float,
    progressLabel: String,
    icon: ImageVector,
    iconGradient: List<Color>,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = AppMotion.tweenMedium(),
        label = "metric_progress",
    )

    AppCard(modifier = modifier, onClick = onClick) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(iconGradient)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTintColor(),
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = contentSecondaryColor(),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = contentPrimaryColor(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress.coerceAtLeast(0.04f))
                        .height(8.dp)
                        .background(
                            Brush.horizontalGradient(brandingGradient()),
                            RoundedCornerShape(4.dp),
                        ),
                )
            }
            Text(
                text = progressLabel,
                style = MaterialTheme.typography.labelMedium,
                color = contentSecondaryColor(),
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
