package com.example.healtapp.data.repository

import com.example.healtapp.data.network.api.AiApi
import com.example.healtapp.data.network.dto.ai.AIRecommendationsResponseDto
import com.example.healtapp.domain.repository.AiRepository
import javax.inject.Inject

class AiRepositoryImpl @Inject constructor(
    private val aiApi: AiApi,
) : AiRepository {

    override suspend fun getRecommendations(days: Int): Result<AIRecommendationsResponseDto> {
        return runCatching {
            aiApi.getRecommendations(days)
        }
    }
}