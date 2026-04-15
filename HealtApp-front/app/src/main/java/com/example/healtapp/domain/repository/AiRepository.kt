package com.example.healtapp.domain.repository

import com.example.healtapp.data.network.dto.ai.AIRecommendationsResponseDto

interface AiRepository {
    suspend fun getRecommendations(days: Int = 7): Result<AIRecommendationsResponseDto>
}