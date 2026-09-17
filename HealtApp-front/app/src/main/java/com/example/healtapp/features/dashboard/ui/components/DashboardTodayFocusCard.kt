package com.example.healtapp.features.dashboard.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.animation.AppMotion
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.core.ui.theme.Dimens
import com.example.healtapp.core.ui.theme.brandingGradient
import com.example.healtapp.core.ui.theme.contentPrimaryColor
import com.example.healtapp.core.ui.theme.heroBlockGradient
import com.example.healtapp.core.ui.theme.heroContentColor
import com.example.healtapp.features.dashboard.presentation.DashboardUiState
import com.example.healtapp.features.dashboard.presentation.TodayFocusBuilder
import com.example.healtapp.features.dashboard.presentation.TodayFocusItemUi

@Composable
fun DashboardTodayFocusCard(
    uiState: DashboardUiState,
    isEvening: Boolean,
    modifier: Modifier = Modifier,
) {
    val items = remember(
        uiState.actionPlanItems,
        uiState.recommendations,
        uiState.circadian,
        uiState.waterMl,
        uiState.stepsToday,
        uiState.sleepHours,
        uiState.caloriesToday,
        uiState.moodCheckIn.savedToday,
        uiState.recoveryMode,
        isEvening,
    ) { TodayFocusBuilder.build(uiState, isEvening, uiState.recoveryMode) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
        SectionHeader(title = if (isEvening) "Вечерний план" else "Фокус утра")
        if (items.isEmpty()) {
            Text(
                "Вода, шаги, сон и самочувствие в пределах целей.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            items.forEach { item ->
                FocusInfoCard(item = item)
            }
        }
    }
}

@Composable
private fun FocusInfoCard(item: TodayFocusItemUi) {
    val fraction by animateFloatAsState(
        targetValue = item.progressFraction?.coerceIn(0f, 1f) ?: 0f,
        animationSpec = AppMotion.tweenMedium(),
        label = "focus_progress",
    )
    AppCard(highlight = item.primary) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(if (item.primary) 52.dp else 44.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(heroBlockGradient())),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        focusIcon(item.category),
                        contentDescription = null,
                        tint = heroContentColor(),
                        modifier = Modifier.size(if (item.primary) 26.dp else 22.dp),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        item.title,
                        style = if (item.primary) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.titleSmall
                        },
                        fontWeight = FontWeight.SemiBold,
                        color = contentPrimaryColor(),
                    )
                    Text(
                        item.detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (item.progressFraction != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction.coerceAtLeast(0.04f))
                            .height(8.dp)
                            .background(
                                Brush.horizontalGradient(brandingGradient()),
                                RoundedCornerShape(4.dp),
                            ),
                    )
                }
            }
            item.progress?.let { progress ->
                Text(
                    progress,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private fun focusIcon(category: String): ImageVector = when (category.lowercase()) {
    "hydration" -> Icons.Filled.LocalDrink
    "activity" -> Icons.AutoMirrored.Filled.DirectionsWalk
    "sleep" -> Icons.Filled.WbTwilight
    "meals", "nutrition" -> Icons.Filled.Restaurant
    else -> Icons.Filled.AutoAwesome
}
