package com.example.healtapp.features.dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.theme.Dimens
import com.example.healtapp.core.ui.theme.contentPrimaryColor
import com.example.healtapp.core.ui.theme.heroBlockGradient
import com.example.healtapp.core.ui.theme.heroContentColor
import com.example.healtapp.data.network.dto.analytics.HabitExperimentDto
import com.example.healtapp.data.network.dto.analytics.TonightRiskDto

@Composable
fun DashboardQuickCaptureRow(
    waterEnabled: Boolean,
    onAddWater: () -> Unit,
    onOpenFoodPhoto: () -> Unit,
    onRefreshSteps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
    ) {
        QuickCaptureChip(
            title = "+250 мл",
            icon = Icons.Filled.WaterDrop,
            onClick = onAddWater,
            enabled = waterEnabled,
            modifier = Modifier.weight(1f),
        )
        QuickCaptureChip(
            title = "Фото еды",
            icon = Icons.Filled.PhotoCamera,
            onClick = onOpenFoodPhoto,
            modifier = Modifier.weight(1f),
        )
        QuickCaptureChip(
            title = "Шаги",
            icon = Icons.AutoMirrored.Filled.DirectionsWalk,
            onClick = onRefreshSteps,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QuickCaptureChip(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    AppCard(
        modifier = modifier,
        onClick = if (enabled) onClick else null,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.IconBadge)
                    .clip(RoundedCornerShape(Dimens.RadiusS))
                    .background(Brush.linearGradient(heroBlockGradient())),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = heroContentColor(),
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentPrimaryColor(),
                maxLines = 1,
            )
        }
    }
}

@Composable
fun DashboardTonightRiskCard(
    risk: TonightRiskDto,
    onStartExperiment: () -> Unit,
    experimentActive: Boolean,
) {
    val showCard = risk.isEvening || risk.levers.isNotEmpty()
    if (!showCard) return
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimens.IconBadge)
                        .clip(RoundedCornerShape(Dimens.RadiusS))
                        .background(Brush.linearGradient(heroBlockGradient())),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.NightsStay,
                        contentDescription = null,
                        tint = heroContentColor(),
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Сон сегодня вечером",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = risk.headline.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentPrimaryColor(),
                    )
                }
            }
            LinearProgressIndicator(
                progress = { (risk.riskScore / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            risk.primaryAction?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            risk.levers.firstOrNull()?.why?.let { why ->
                Text(
                    why,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!experimentActive && risk.suggestedExperiment != null) {
                AppButton(
                    text = "Попробовать 7 дней",
                    onClick = onStartExperiment,
                )
            }
        }
    }
}

@Composable
fun DashboardHabitExperimentCard(
    experiment: HabitExperimentDto,
    checkedToday: Boolean? = null,
    keptCount: Int = 0,
    onCheckin: (Boolean) -> Unit = {},
    onOpenFriends: () -> Unit = {},
) {
    val isDone = experiment.status == "completed"
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimens.IconBadge)
                        .clip(RoundedCornerShape(Dimens.RadiusS))
                        .background(Brush.linearGradient(heroBlockGradient())),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Science,
                        contentDescription = null,
                        tint = heroContentColor(),
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isDone) "Неделя прошла" else "Эксперимент 7 дней",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = experiment.title.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentPrimaryColor(),
                    )
                }
            }
            experiment.action?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            if (!isDone) {
                val progress = if (experiment.daysTotal > 0) {
                    experiment.daysElapsed / experiment.daysTotal.toFloat()
                } else {
                    0f
                }
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "День ${experiment.daysElapsed.coerceAtLeast(1)} из ${experiment.daysTotal}" +
                        if (keptCount > 0) " · держали $keptCount дн." else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppButton(
                        text = if (checkedToday == true) "Сегодня да" else "Да",
                        onClick = { onCheckin(true) },
                        modifier = Modifier.weight(1f),
                    )
                    AppButton(
                        text = if (checkedToday == false) "Сегодня нет" else "Нет",
                        onClick = { onCheckin(false) },
                        isSecondary = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                AppButton(
                    text = "С друзьями",
                    onClick = onOpenFriends,
                    isSecondary = true,
                )
            } else {
                Text(
                    text = experiment.resultSummary.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

