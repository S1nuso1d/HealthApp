package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.ai.AIRecommendationsResponseDto
import com.example.healtapp.data.network.dto.wellness.AIBriefDto
import com.example.healtapp.data.network.dto.wellness.AIChatRequestDto
import com.example.healtapp.data.network.dto.wellness.AIExplainInsightRequestDto
import com.example.healtapp.data.network.dto.wellness.AIResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AiApi {

    @GET("ai/recommendations")
    suspend fun getRecommendations(
        @Query("days") days: Int = 7,
        @Query("use_llm_tips") useLlmTips: Boolean = false,
    ): AIRecommendationsResponseDto

    @POST("ai/chat")
    suspend fun chat(@Body body: AIChatRequestDto): AIResponseDto

    @GET("ai/daily-brief")
    suspend fun dailyBrief(@Query("days") days: Int = 3): AIBriefDto

    @GET("ai/weekly-brief")
    suspend fun weeklyBrief(@Query("days") days: Int = 7): AIBriefDto

    @POST("ai/explain-insight")
    suspend fun explainInsight(@Body body: AIExplainInsightRequestDto): AIResponseDto
}
