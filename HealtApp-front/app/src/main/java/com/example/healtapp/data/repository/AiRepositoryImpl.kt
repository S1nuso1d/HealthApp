package com.example.healtapp.data.repository

import com.example.healtapp.data.network.api.AiApi
import com.example.healtapp.data.network.dto.ai.AIRecommendationsResponseDto
import com.example.healtapp.domain.repository.AiRepository

class AiRepositoryImpl(
    private val aiApi: AiApi
) : AiRepository {

    override suspend fun getRecommendations(days: Int): Result<AIRecommendationsResponseDto> {
        return runCatching {
            aiApi.getRecommendations(days)
        }
    }
}