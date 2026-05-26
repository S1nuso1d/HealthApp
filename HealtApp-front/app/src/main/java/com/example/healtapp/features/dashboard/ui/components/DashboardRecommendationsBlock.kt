package com.example.healtapp.features.dashboard.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.features.recommendations.presentation.RecommendationUiItem

@Composable
fun DashboardRecommendationsBlock(
    isLoading: Boolean,
    error: String?,
    recommendations: List<RecommendationUiItem>,
    onRetry: () -> Unit,
    onOpenAll: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeader(
            title = "Советы для вас",
            subtitle = "На основе вашей аналитики · обновляются при достижении целей",
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!isLoading) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onRetry)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "Обновить",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Обновить",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        when {
            isLoading -> {
                AppCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = "Анализируем ваши данные…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            error != null -> {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        AppButton(
                            text = "Повторить загрузку",
                            onClick = onRetry,
                        )
                    }
                }
            }

            recommendations.isEmpty() -> {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Пока нет активных советов",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Заполните сон, воду, питание или шаги — персональные рекомендации появятся здесь. Выполненные цели скрывают советы автоматически.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        AppButton(
                            text = "Обновить",
                            onClick = onRetry,
                            isSecondary = true,
                        )
                    }
                }
            }

            else -> {
                recommendations.take(3).forEachIndexed { index, item ->
                    DashboardRecommendationCard(
                        item = item,
                        onClick = onOpenAll,
                        emphasized = index == 0,
                    )
                }
                AppButton(
                    text = "Все рекомендации (${recommendations.size})",
                    onClick = onOpenAll,
                    modifier = Modifier.fillMaxWidth(),
                    isSecondary = true,
                )
            }
        }
    }
}
