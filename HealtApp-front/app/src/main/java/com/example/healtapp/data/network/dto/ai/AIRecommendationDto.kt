package com.example.healtapp.data.network.dto.ai

data class AIRecommendationDto(
    val category: String,
    val title: String,
    val description: String,
    val priority: String,
    val status: String = "active",
    val confidence: Float? = null,
    val action: String? = null,
    val personalized_tip: String? = null,
    val progress_label: String? = null,
    val related_insight_title: String? = null,
    val related_insight_type: String? = null
)