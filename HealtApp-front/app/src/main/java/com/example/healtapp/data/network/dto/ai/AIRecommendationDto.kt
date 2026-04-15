package com.example.healtapp.data.network.dto.ai

data class AIRecommendationDto(
    val category: String,
    val title: String,
    val description: String,
    val priority: String,
    val confidence: Float,
    val action: String,
    val related_insight_title: String? = null,
    val related_insight_type: String? = null
)