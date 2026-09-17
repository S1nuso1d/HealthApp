package com.example.healtapp.features.dashboard.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import com.example.healtapp.core.ui.theme.heroContentColor
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
    goalMet: Boolean = false,
    compact: Boolean = false,
    showWaterGlass: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = AppMotion.tweenMedium(),
        label = "metric_progress",
    )

    AppCard(
        modifier = modifier,
        onClick = onClick,
        quiet = goalMet,
        highlight = !goalMet && progress in 0.01f..0.85f && !compact,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp),
        ) {
            if (showWaterGlass) {
                WaterGlassMeter(progress = animatedProgress)
            } else {
                Box(
                    modifier = Modifier
                        .size(if (compact) 36.dp else 44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (goalMet) {
                                Brush.linearGradient(iconGradient.map { it.copy(alpha = 0.45f) })
                            } else {
                                Brush.linearGradient(iconGradient)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTintColor().copy(alpha = if (goalMet) 0.7f else 1f),
                        modifier = Modifier.size(if (compact) 20.dp else 24.dp),
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentSecondaryColor(),
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (goalMet) {
                    GoalMetBadge()
                }
            }
            Text(
                text = value,
                style = if (compact) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.titleLarge
                },
                fontWeight = FontWeight.Bold,
                color = contentPrimaryColor().copy(alpha = if (goalMet) 0.78f else 1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!showWaterGlass) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (compact) 5.dp else 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress.coerceAtLeast(0.04f))
                            .height(if (compact) 5.dp else 8.dp)
                            .background(
                                Brush.horizontalGradient(
                                    if (goalMet) {
                                        brandingGradient().map { it.copy(alpha = 0.45f) }
                                    } else {
                                        brandingGradient()
                                    },
                                ),
                                RoundedCornerShape(4.dp),
                            ),
                    )
                }
            }
            if (!goalMet) {
                Text(
                    text = progressLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = contentSecondaryColor(),
                    fontWeight = FontWeight.Medium,
                )
            }
            if (actionLabel != null && onAction != null) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onAction() },
                )
            }
        }
    }
}

@Composable
private fun GoalMetBadge() {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.linearGradient(brandingGradient())),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = "Цель достигнута",
            tint = heroContentColor(),
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
fun WaterGlassMeter(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val fill = progress.coerceIn(0f, 1f)
    val shape = RoundedCornerShape(10.dp)
    val outline = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    Box(
        modifier = modifier
            .width(32.dp)
            .height(44.dp)
            .clip(shape)
            .border(1.dp, outline, shape)
            .padding(2.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(fill.coerceAtLeast(0.06f))
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.verticalGradient(brandingGradient())),
        )
    }
}
