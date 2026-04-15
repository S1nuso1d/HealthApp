package com.example.healtapp.features.timeline.ui

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.features.timeline.presentation.TimelineViewModel
import com.example.healtapp.features.timeline.ui.components.TimelineDayCard
import com.example.healtapp.features.timeline.ui.components.TimelineEventItem
import com.example.healtapp.features.timeline.ui.components.TimelineInsightCard
import com.example.healtapp.features.timeline.ui.components.TimelineSummaryCard

@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Health Timeline",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Единая лента дня: сон, вода, питание, активность и рекомендации",
            style = MaterialTheme.typography.bodyLarge
        )

        TimelineSummaryCard(
            selectedDate = uiState.selectedDate,
            summaryText = uiState.summaryText
        )

        TimelineInsightCard(
            text = uiState.insightText
        )

        SectionHeader("День")
        TimelineDayCard(
            date = uiState.selectedDate
        )

        SectionHeader("События")

        uiState.events.forEach { event ->
            TimelineEventItem(event = event)
        }
    }
}