package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.ai.AIRecommendationsResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface AiApi {

    @GET("ai/recommendations")
    suspend fun getRecommendations(
        @Query("days") days: Int = 7
    ): AIRecommendationsResponseDto
}