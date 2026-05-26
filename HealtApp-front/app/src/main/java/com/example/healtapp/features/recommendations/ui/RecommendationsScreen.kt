package com.example.healtapp.features.recommendations.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppMessageBanner
import com.example.healtapp.core.ui.components.AppMessageType
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.components.CollapsibleAppCard
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.features.dashboard.ui.components.DashboardHealthScoreBanner
import com.example.healtapp.features.dashboard.ui.components.DashboardSkeleton
import com.example.healtapp.features.recommendations.presentation.RecommendationsViewModel
import com.example.healtapp.features.recommendations.ui.components.RecommendationCard

@Composable
fun RecommendationsScreen() {
    val viewModel: RecommendationsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AppScreen(
        title = "Рекомендации",
        subtitle = "Персональные предупреждения по вашему дневнику",
        headerIcon = Icons.Filled.Lightbulb,
        scrollable = true,
    ) {
        when {
            uiState.isLoading -> {
                DashboardSkeleton()
            }

            uiState.error != null -> {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AppMessageBanner(
                            text = uiState.error ?: "Ошибка загрузки",
                            type = AppMessageType.Error,
                            title = "Не удалось загрузить",
                        )
                        AppButton(
                            text = "Повторить",
                            onClick = { viewModel.refresh() },
                            isSecondary = true,
                        )
                    }
                }
            }

            uiState.recommendations.isEmpty() -> {
                if (uiState.healthScore > 0) {
                    DashboardHealthScoreBanner(
                        healthScore = uiState.healthScore,
                        periodDays = uiState.periodDays,
                        recommendationsCount = 0,
                    )
                }
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Нет активных рекомендаций",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "Заполните данные за последние дни или обновите список позже.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        AppButton(
                            text = "Обновить",
                            onClick = { viewModel.refresh() },
                            isSecondary = true,
                        )
                    }
                }
            }

            else -> {
                if (uiState.healthScore > 0) {
                    DashboardHealthScoreBanner(
                        healthScore = uiState.healthScore,
                        periodDays = uiState.periodDays,
                        recommendationsCount = uiState.recommendations.size,
                    )
                }

                CollapsibleAppCard(
                    title = "Список советов",
                    subtitle = "Период: ${uiState.periodDays} дн. · ${uiState.recommendations.size} советов",
                    initiallyExpanded = true,
                ) {
                    uiState.recommendations.forEach { recommendation ->
                        RecommendationCard(item = recommendation)
                    }
                    AppButton(
                        text = "Обновить рекомендации",
                        onClick = { viewModel.refresh() },
                        modifier = Modifier.fillMaxWidth(),
                        isSecondary = true,
                    )
                }
            }
        }

        Spacer(Modifier.height(72.dp))
    }
}
