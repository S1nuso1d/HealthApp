package com.example.healtapp.features.timeline.presentation

data class TimelineEventUi(
    val id: String,
    val timeLabel: String,
    val title: String,
    val subtitle: String,
    val category: String,
)

data class TimelineUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val events: List<TimelineEventUi> = emptyList(),
    val summaryText: String = "",
    val insightHighlight: String = "",
)
