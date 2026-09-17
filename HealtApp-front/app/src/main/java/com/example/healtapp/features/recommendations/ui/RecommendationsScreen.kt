package com.example.healtapp.features.recommendations.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.EmptyStateCard
import com.example.healtapp.core.ui.components.FeatureHeroChip
import com.example.healtapp.core.ui.components.FeatureHighlightPanel
import com.example.healtapp.core.ui.components.FeatureInlineNotice
import com.example.healtapp.core.ui.components.LoadingView
import com.example.healtapp.core.ui.components.FeatureScreenShell
import com.example.healtapp.core.ui.components.FeatureSectionTitle
import com.example.healtapp.core.ui.theme.contentPrimaryColor
import com.example.healtapp.core.ui.theme.contentSecondaryColor
import com.example.healtapp.features.recommendations.presentation.RecommendationsViewModel
import com.example.healtapp.features.recommendations.ui.components.RecommendationCard

@Composable
fun RecommendationsScreen(
    onBack: () -> Unit = {},
) {
    val viewModel: RecommendationsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FeatureScreenShell(
        title = "Рекомендации",
        subtitle = "Персональные советы по вашему дневнику",
        icon = Icons.Filled.Lightbulb,
        onBack = onBack,
        scrollStateKey = "recommendations",
        heroFooter = if (!uiState.isLoading) {
            {
                Row(
                    modifier = Modifier.padding(start = 12.dp, top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (uiState.healthScore > 0) {
                        FeatureHeroChip(label = "Индекс ${uiState.healthScore}")
                    }
                    FeatureHeroChip(
                        label = if (uiState.recommendations.isEmpty()) {
                            "Период ${uiState.periodDays} дн."
                        } else {
                            "${uiState.recommendations.size} советов · ${uiState.periodDays} дн."
                        },
                    )
                }
            }
        } else {
            null
        },
    ) {
        if (uiState.isLoading) {
            LoadingView()
            return@FeatureScreenShell
        }

        uiState.error?.let { err ->
            FeatureInlineNotice(text = err, isError = true)
            AppButton(
                text = "Повторить",
                onClick = { viewModel.refresh() },
                isSecondary = true,
            )
            return@FeatureScreenShell
        }

        if (uiState.healthScore > 0) {
            FeatureHighlightPanel {
                Column {
                    Text(
                        text = "Индекс здоровья",
                        style = MaterialTheme.typography.labelMedium,
                        color = contentSecondaryColor(),
                    )
                    Text(
                        text = "${uiState.healthScore}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentPrimaryColor(),
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Активных советов",
                        style = MaterialTheme.typography.labelMedium,
                        color = contentSecondaryColor(),
                    )
                    Text(
                        text = "${uiState.recommendations.size}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = contentPrimaryColor(),
                    )
                }
            }
        }

        FeatureSectionTitle(
            title = "Советы для вас",
            subtitle = "На основе аналитики за ${uiState.periodDays} дней",
        )

        if (uiState.recommendations.isEmpty()) {
            EmptyStateCard(
                title = "Нет активных рекомендаций",
                text = "Заполните сон, воду, питание или шаги — персональные советы появятся здесь.",
                icon = Icons.Filled.Lightbulb,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                uiState.recommendations.forEach { recommendation ->
                    RecommendationCard(item = recommendation)
                }
            }
        }

        AppButton(
            text = "Обновить рекомендации",
            onClick = { viewModel.refresh() },
            modifier = Modifier.fillMaxWidth(),
            isSecondary = true,
        )
    }
}
