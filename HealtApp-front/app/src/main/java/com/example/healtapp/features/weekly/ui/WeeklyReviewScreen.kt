package com.example.healtapp.features.weekly.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.EmptyStateCard
import com.example.healtapp.core.ui.components.FeatureInlineNotice
import com.example.healtapp.core.ui.components.FeatureScreenShell
import com.example.healtapp.core.ui.components.FeatureSectionTitle
import com.example.healtapp.core.ui.components.LoadingView
import com.example.healtapp.features.dashboard.ui.components.DashboardWeeklySummaryBlock
import com.example.healtapp.features.weekly.presentation.WeeklyReviewViewModel

@Composable
fun WeeklyReviewScreen(
    onBack: () -> Unit,
    viewModel: WeeklyReviewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FeatureScreenShell(
        title = "Разбор недели",
        subtitle = "Итоги, сравнение с прошлой неделей и фокус вперёд",
        icon = Icons.Filled.AutoStories,
        onBack = onBack,
        scrollStateKey = "weekly_review",
    ) {
        when {
            uiState.isLoading -> LoadingView()
            uiState.error != null && uiState.brief == null -> {
                FeatureInlineNotice(text = uiState.error!!, isError = true)
                AppButton(text = "Повторить", onClick = viewModel::refresh, isSecondary = true)
            }
            else -> {
                DashboardWeeklySummaryBlock(summary = uiState.weeklySummary)
                val brief = uiState.brief
                if (brief == null) {
                    EmptyStateCard(
                        title = "Нет разбора",
                        text = "Нужно больше записей в дневнике, чтобы собрать итоги недели.",
                        icon = Icons.Filled.AutoStories,
                    )
                } else {
                    AppCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(brief.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(brief.summary, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    if (brief.keyPoints.isNotEmpty()) {
                        FeatureSectionTitle(title = "Главное", subtitle = "На что обратить внимание")
                        AppCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                brief.keyPoints.forEach { point ->
                                    Text("• $point", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }

                FeatureSectionTitle(
                    title = "Сравнение с прошлым пересчётом",
                    subtitle = "Как изменились баллы",
                )
                val compare = uiState.compare
                if (compare == null) {
                    FeatureInlineNotice(text = uiState.compareUnavailable ?: "Сравнение пока недоступно")
                } else {
                    val delta = compare.summary?.healthScoreDelta
                    AppCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = when (compare.summary?.overallTrend) {
                                    "improved" -> "Неделя лучше предыдущей"
                                    "declined" -> "Неделя слабее предыдущей"
                                    "mixed" -> "Картина смешанная"
                                    else -> "Баллы почти не изменились"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (delta != null) {
                                val sign = if (delta > 0) "+" else ""
                                Text(
                                    "Health score: $sign$delta",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            compare.scoreDeltas.forEach { item ->
                                val label = when (item.metric) {
                                    "health_score" -> "Общий"
                                    "sleep_score" -> "Сон"
                                    "hydration_score" -> "Вода"
                                    "activity_score" -> "Активность"
                                    "nutrition_score" -> "Питание"
                                    "state_score" -> "Состояние"
                                    else -> item.metric.orEmpty()
                                }
                                val change = item.delta ?: 0
                                val sign = if (change > 0) "+" else ""
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(label, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "$sign$change  (${item.previousValue ?: "—"} → ${item.currentValue ?: "—"})",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                    if (compare.progressInsights.isNotEmpty()) {
                        FeatureSectionTitle(title = "Наблюдения", subtitle = null)
                        compare.progressInsights.forEach { insight ->
                            AppCard {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(insight.title.orEmpty(), fontWeight = FontWeight.SemiBold)
                                    Text(
                                        insight.description.orEmpty(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
