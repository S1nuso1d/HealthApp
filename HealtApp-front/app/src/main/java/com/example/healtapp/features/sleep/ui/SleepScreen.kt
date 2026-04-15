package com.example.healtapp.features.sleep.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.features.sleep.presentation.SleepViewModel
import com.example.healtapp.features.sleep.ui.components.SleepFormCard
import com.example.healtapp.features.sleep.ui.components.SleepInsightCard
import com.example.healtapp.features.sleep.ui.components.SleepRecordItem
import com.example.healtapp.features.sleep.ui.components.SleepSummaryCard

@Composable
fun SleepScreen() {
    val context = LocalContext.current
    val viewModel = remember { SleepViewModel(context) }
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Сон",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Отслеживай продолжительность, качество и режим сна",
            style = MaterialTheme.typography.bodyLarge
        )

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

        uiState.records.forEach { record ->
            SleepRecordItem(record = record)
        }
    }
}