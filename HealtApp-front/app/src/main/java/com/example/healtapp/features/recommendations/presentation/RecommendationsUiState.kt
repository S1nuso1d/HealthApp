package com.example.healtapp.features.recommendations.presentation

data class RecommendationUiItem(
    val category: String,
    val title: String,
    val description: String,
    val priority: String,
    val status: String = "active",
    val confidence: Float? = null,
    val action: String? = null,
    val personalizedTip: String? = null,
    val progressLabel: String? = null
)

data class RecommendationsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val healthScore: Int = 0,
    val periodDays: Int = 7,
    val recommendations: List<RecommendationUiItem> = emptyList()
)