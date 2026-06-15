package com.example.healtapp.domain.repository

import com.example.healtapp.data.network.dto.ai.AIRecommendationsResponseDto
import com.example.healtapp.data.network.dto.ai.AiRecognizedFoodResponseDto
import com.example.healtapp.data.network.dto.ai.MealPlanResponseDto
import com.example.healtapp.data.network.dto.ai.SleepSummaryRequestDto
import com.example.healtapp.data.network.dto.ai.SleepSummaryResponseDto
import com.example.healtapp.data.network.dto.ai.WorkoutPlanResponseDto
import com.example.healtapp.data.network.dto.ai.ProactiveTipResponseDto
import com.example.healtapp.data.network.dto.wellness.AIChatRequestDto
import kotlinx.coroutines.flow.Flow
import java.io.File

interface AiRepository {
    suspend fun getProactiveTip(): Result<ProactiveTipResponseDto>
    suspend fun getRecommendations(days: Int = 7): Result<AIRecommendationsResponseDto>
    suspend fun getMealPlan(days: Int = 7): Result<MealPlanResponseDto>
    suspend fun getAiStatus(): Result<com.example.healtapp.data.network.dto.ai.AiStatusDto>
    suspend fun getWorkoutPlan(days: Int = 7): Result<WorkoutPlanResponseDto>
    suspend fun recognizeFood(imageFile: File): Result<AiRecognizedFoodResponseDto>
    suspend fun recognizeFoodFromText(text: String): Result<AiRecognizedFoodResponseDto>
    suspend fun streamChat(request: AIChatRequestDto): Result<Flow<String>>
    suspend fun getSleepSummary(request: SleepSummaryRequestDto): Result<SleepSummaryResponseDto>
    suspend fun getDashboardHints(): Result<List<String>>
}