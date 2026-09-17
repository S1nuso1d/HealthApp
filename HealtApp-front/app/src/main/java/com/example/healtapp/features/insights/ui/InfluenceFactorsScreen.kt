package com.example.healtapp.features.insights.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppMessageBanner
import com.example.healtapp.core.ui.components.AppMessageType
import com.example.healtapp.core.ui.components.EmptyStateCard
import com.example.healtapp.core.ui.components.FeatureInlineNotice
import com.example.healtapp.core.ui.components.FeatureScreenShell
import com.example.healtapp.core.ui.components.LoadingView
import com.example.healtapp.core.ui.theme.Dimens
import com.example.healtapp.core.ui.theme.contentPrimaryColor
import com.example.healtapp.data.network.dto.analytics.HabitExperimentDto
import com.example.healtapp.data.network.dto.analytics.HoursBeforeSleepBucketDto
import com.example.healtapp.data.network.dto.analytics.InfluenceFactorDto
import com.example.healtapp.features.insights.presentation.InfluenceFactorsViewModel
import kotlinx.coroutines.delay

@Composable
fun InfluenceFactorsScreen(
    onBack: () -> Unit,
    viewModel: InfluenceFactorsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FeatureScreenShell(
        title = "Что на тебя влияет",
        subtitle = "Закономерности из дневника за две недели",
        icon = Icons.Filled.Insights,
        onBack = onBack,
        scrollStateKey = "influence_factors",
    ) {
        val notice = uiState.notice
        LaunchedEffect(notice) {
            if (notice.isNullOrBlank()) return@LaunchedEffect
            delay(3200)
            viewModel.consumeNotice()
        }
        notice?.let {
            AppMessageBanner(text = it, type = AppMessageType.Success)
        }
        uiState.activeExperiment?.takeIf { it.status == "active" }?.let { experiment ->
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
                    Text(
                        "Сейчас идёт эксперимент",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        experiment.title.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentPrimaryColor(),
                    )
                    Text(
                        "День ${experiment.daysElapsed.coerceAtLeast(1)} из ${experiment.daysTotal}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        HoursBeforeSleepCard(buckets = uiState.circadian?.hoursBeforeSleep.orEmpty())
        RecentExperimentsCard(items = uiState.recentExperiments)
        when {
            uiState.isLoading -> LoadingView()
            uiState.error != null -> {
                FeatureInlineNotice(text = uiState.error!!, isError = true)
                AppButton(text = "Повторить", onClick = viewModel::refresh, isSecondary = true)
            }
            uiState.data?.hasEnoughData != true -> EmptyStateCard(
                title = "Пока рано",
                text = uiState.data?.message
                    ?: "Нужно хотя бы две недели записей сна, еды и воды.",
                icon = Icons.Filled.Insights,
            )
            else -> {
                uiState.data?.factors.orEmpty().forEach { factor ->
                    InfluenceFactorCard(
                        factor = factor,
                        isStarting = uiState.startingFactorId == factor.id,
                        experimentActive = uiState.activeExperiment?.status == "active" &&
                            uiState.activeExperiment?.factorId == factor.id,
                        onStartExperiment = {
                            viewModel.startExperiment(factor.id.orEmpty(), factor.title)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun InfluenceFactorCard(
    factor: InfluenceFactorDto,
    isStarting: Boolean,
    experimentActive: Boolean,
    onStartExperiment: () -> Unit,
) {
    val comparison = factor.comparison
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
            Text(
                text = factor.title.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentPrimaryColor(),
            )
            factor.description?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }
            comparison?.proofLine?.let { proof ->
                Text(
                    text = proof,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = contentPrimaryColor(),
                )
            }
            Text(
                text = "Сила связи ${factor.strength}% · ${factor.affectsMetricTitle ?: "самочувствие"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { factor.strength / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            )
            if (comparison != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
                ) {
                    ComparisonCell(
                        title = comparison.withDays?.let { "С фактором · $it дн." } ?: "С фактором",
                        value = comparison.withFactorValue,
                        note = comparison.withFactorNote,
                        unit = comparison.unitLabel,
                        modifier = Modifier.weight(1f),
                    )
                    ComparisonCell(
                        title = comparison.withoutDays?.let { "Без фактора · $it дн." } ?: "Без фактора",
                        value = comparison.withoutFactorValue,
                        note = comparison.withoutFactorNote,
                        unit = comparison.unitLabel,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (factor.impact != "positive") {
                AppButton(
                    text = when {
                        experimentActive -> "Эксперимент уже идёт"
                        isStarting -> "Запускаем…"
                        else -> "Попробовать 7 дней"
                    },
                    onClick = onStartExperiment,
                    enabled = !isStarting && !experimentActive,
                )
            }
        }
    }
}

@Composable
private fun ComparisonCell(
    title: String,
    value: Double?,
    note: String?,
    unit: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val formatted = value?.let { v ->
            val number = if (v % 1.0 == 0.0) v.toInt().toString() else "%.1f".format(v)
            if (unit.isNullOrBlank()) number else "$number $unit"
        } ?: "—"
        Text(formatted, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        note?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HoursBeforeSleepCard(buckets: List<HoursBeforeSleepBucketDto>) {
    if (buckets.isEmpty()) return
    val maxHours = buckets.maxOf { it.sleepHours ?: 0.0 }.coerceAtLeast(1.0)
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
            Text(
                "Ужин и длительность сна",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentPrimaryColor(),
            )
            Text(
                "Средний сон в зависимости от того, за сколько часов до отбоя был последний приём пищи.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            buckets.forEach { bucket ->
                val hours = bucket.sleepHours ?: 0.0
                val shortest = buckets.minByOrNull { it.sleepHours ?: Double.MAX_VALUE }?.bucket
                val tallest = buckets.maxByOrNull { it.sleepHours ?: 0.0 }?.bucket
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            (bucket.label ?: bucket.bucket.orEmpty()) + when (bucket.bucket) {
                                shortest -> " · короче"
                                tallest -> " · длиннее"
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (bucket.bucket == shortest || bucket.bucket == tallest) {
                                FontWeight.SemiBold
                            } else {
                                FontWeight.Normal
                            },
                        )
                        Text(
                            "${"%.1f".format(hours)} ч · ${bucket.nights} ноч.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (hours / maxHours).toFloat().coerceIn(0.08f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (bucket.bucket == shortest) 14.dp else 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentExperimentsCard(items: List<HabitExperimentDto>) {
    if (items.isEmpty()) return
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
            Text(
                "Прошлые эксперименты",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentPrimaryColor(),
            )
            items.forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        item.title.orEmpty(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val summary = item.resultSummary?.takeIf { it.isNotBlank() }
                        ?: listOfNotNull(item.startedOn, item.endsOn).joinToString(" — ")
                    if (summary.isNotBlank()) {
                        Text(
                            summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
