package com.example.healtapp.features.fasting.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppMessageBanner
import com.example.healtapp.core.ui.components.AppMessageType
import com.example.healtapp.core.ui.components.BrandedFilterChipRowIndexed
import com.example.healtapp.core.ui.components.ProgressRing
import com.example.healtapp.core.ui.theme.heroBlockGradient
import com.example.healtapp.core.ui.theme.heroContentColor
import com.example.healtapp.data.preferences.FastingPlan
import com.example.healtapp.features.fasting.presentation.FastingViewModel
import java.util.concurrent.TimeUnit

@Composable
fun FastingTabContent(
    viewModel: FastingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val fasting = uiState.fasting
    val progress = fasting.progress(uiState.nowMs)
    val remainingMs = fasting.remainingMs(uiState.nowMs)
    val elapsedMs = fasting.elapsedMs(uiState.nowMs)
    val planLabels = FastingPlan.entries.map { it.label }
    val selectedPlanIndex = FastingPlan.entries.indexOfFirst { it.hours == fasting.planHours }
        .coerceAtLeast(0)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppMessageBanner(
            text = "Голодание хранится только на этом устройстве и не синхронизируется с аккаунтом.",
            type = AppMessageType.Info,
        )
        uiState.message?.let {
            AppMessageBanner(text = it, type = AppMessageType.Info)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(heroBlockGradient()))
                .padding(20.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = if (fasting.isActive) "Голодание идёт" else "Таймер",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = heroContentColor(),
                )
                ProgressRing(
                    progress = progress,
                    text = formatDuration(if (fasting.isActive) elapsedMs else 0L),
                    color = heroContentColor(),
                    trackColor = heroContentColor().copy(alpha = 0.28f),
                    textColor = heroContentColor(),
                )
                Text(
                    text = if (fasting.isActive) "из ${fasting.planHours} ч" else "Выберите план и начните",
                    style = MaterialTheme.typography.bodySmall,
                    color = heroContentColor().copy(alpha = 0.86f),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = fasting.phaseLabel(uiState.nowMs),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = heroContentColor(),
                )
                if (fasting.isActive) {
                    Text(
                        text = "До конца: ${formatDuration(remainingMs)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = heroContentColor().copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Окно питания",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (fasting.isActive) {
                        "Есть можно ${24 - fasting.planHours} ч в сутки · сейчас фаза: ${fasting.phaseLabel(uiState.nowMs)}"
                    } else {
                        "План ${fasting.planHours}/${24 - fasting.planHours}: ${fasting.planHours} ч голодания и ${24 - fasting.planHours} ч питания"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BrandedFilterChipRowIndexed(
                    items = planLabels,
                    selectedIndex = selectedPlanIndex,
                    onSelect = { index ->
                        FastingPlan.entries.getOrNull(index)?.let(viewModel::selectPlan)
                    },
                    enabled = !fasting.isActive,
                )
            }
        }

        if (fasting.isActive) {
            AppButton(text = "Завершить голодание", onClick = viewModel::stopFast, isSecondary = true)
        } else {
            AppButton(text = "Начать голодание", onClick = viewModel::startFast)
        }

        AppCard {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Мягкие правила",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Пейте воду и травяной чай во время окна голодания. При головокружении завершите таймер.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

private fun formatDuration(ms: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(ms)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}
