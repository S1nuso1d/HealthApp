package com.example.healtapp.data.network.dto.ai

data class AIRecommendationsResponseDto(
    val generated_at: String,
    val period_days: Int,
    val health_score: Int,
    val recommendations: List<AIRecommendationDto>
)