package com.example.healtapp.features.sleep.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.components.EmptyStateView
import com.example.healtapp.core.ui.components.ErrorStateView
import com.example.healtapp.core.ui.components.ShimmerBox
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.features.sleep.presentation.SleepViewModel
import com.example.healtapp.features.sleep.ui.components.SleepFormCard
import com.example.healtapp.features.sleep.ui.components.SleepInsightCard
import com.example.healtapp.features.sleep.ui.components.SleepRecordItem
import com.example.healtapp.features.sleep.ui.components.SleepSummaryCard

@Composable
fun SleepScreen() {
    val viewModel: SleepViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    AppScreen(
        title = "Сон",
        subtitle = "Длительность и качество",
        headerIcon = Icons.Filled.Bedtime,
        scrollable = true,
    ) {
        Text(
            text = "Следи за продолжительностью сна, качеством и общим режимом отдыха.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (uiState.isLoading) {
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(110.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(84.dp))
            return@AppScreen
        }

        if (uiState.error != null) {
            ErrorStateView(
                message = uiState.error ?: "Не удалось загрузить сон",
                onRetry = { viewModel.load() }
            )
            return@AppScreen
        }

        SleepSummaryCard(
            averageSleepHours = uiState.averageSleepHours,
            targetSleepHours = uiState.targetSleepHours,
            sleepQualityAverage = uiState.sleepQualityAverage
        )

        SleepInsightCard(
            text = uiState.insightText
        )

        SleepFormCard(
            sleepStart = uiState.sleepStartInput,
            sleepEnd = uiState.sleepEndInput,
            quality = uiState.qualityInput,
            note = uiState.noteInput,
            onSleepStartChange = viewModel::updateSleepStart,
            onSleepEndChange = viewModel::updateSleepEnd,
            onQualityChange = viewModel::updateQuality,
            onNoteChange = viewModel::updateNote,
            onSaveClick = viewModel::saveSleepRecord
        )

        SectionHeader("История сна")

        if (uiState.records.isEmpty()) {
            EmptyStateView("Пока нет записей. Добавь первую запись о сне, чтобы увидеть историю.")
        } else {
            uiState.records.forEach { record ->
                SleepRecordItem(record = record)
            }
        }
    }
}