package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.ai.AIRecommendationsResponseDto
import com.example.healtapp.data.network.dto.wellness.AIBriefDto
import com.example.healtapp.data.network.dto.wellness.AIChatRequestDto
import com.example.healtapp.data.network.dto.wellness.AIExplainInsightRequestDto
import com.example.healtapp.data.network.dto.wellness.AIResponseDto
import com.example.healtapp.data.network.dto.ai.MealPlanResponseDto
import com.example.healtapp.data.network.dto.ai.WorkoutPlanResponseDto
import com.example.healtapp.data.network.dto.ai.AiRecognizedFoodResponseDto
import com.example.healtapp.data.network.dto.ai.SleepSummaryRequestDto
import com.example.healtapp.data.network.dto.ai.SleepSummaryResponseDto
import com.example.healtapp.data.network.dto.ai.AiRecognizeTextFoodRequestDto
import com.example.healtapp.data.network.dto.ai.ProactiveTipResponseDto
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface AiApi {

    @GET("ai/proactive-tip")
    suspend fun getProactiveTip(): ProactiveTipResponseDto


    @GET("ai/recommendations")
    suspend fun getRecommendations(
        @Query("days") days: Int = 7,
        @Query("use_llm_tips") useLlmTips: Boolean = false,
    ): AIRecommendationsResponseDto

    @GET("ai/meal_plan")
    suspend fun getMealPlan(@Query("days") days: Int = 7): MealPlanResponseDto

    @GET("ai/status")
    suspend fun getAiStatus(): com.example.healtapp.data.network.dto.ai.AiStatusDto

    @GET("ai/workout_plan")
    suspend fun getWorkoutPlan(@Query("days") days: Int = 7): WorkoutPlanResponseDto

    @POST("ai/chat")
    suspend fun chat(@Body body: AIChatRequestDto): AIResponseDto

    @retrofit2.http.Streaming
    @POST("ai/chat/stream")
    suspend fun streamChat(@Body body: AIChatRequestDto): okhttp3.ResponseBody

    @GET("ai/daily-brief")
    suspend fun dailyBrief(@Query("days") days: Int = 3): AIBriefDto

    @GET("ai/weekly-brief")
    suspend fun weeklyBrief(@Query("days") days: Int = 7): AIBriefDto

    @POST("ai/explain-insight")
    suspend fun explainInsight(@Body body: AIExplainInsightRequestDto): AIResponseDto

    @Multipart
    @POST("ai/recognize-food")
    suspend fun recognizeFood(
        @Part image: MultipartBody.Part
    ): AiRecognizedFoodResponseDto

    @POST("ai/recognize-text-food")
    suspend fun recognizeFoodFromText(
        @Body body: AiRecognizeTextFoodRequestDto
    ): AiRecognizedFoodResponseDto

    @POST("ai/sleep-summary")
    suspend fun getSleepSummary(@Body body: SleepSummaryRequestDto): SleepSummaryResponseDto

    @GET("ai/dashboard-hints")
    suspend fun getDashboardHints(): com.example.healtapp.data.network.dto.ai.DashboardHintsResponseDto
}
