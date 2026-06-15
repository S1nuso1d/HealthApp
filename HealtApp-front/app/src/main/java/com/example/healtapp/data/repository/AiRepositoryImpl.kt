package com.example.healtapp.data.repository

import com.example.healtapp.data.network.api.AiApi
import com.example.healtapp.data.network.dto.ai.AIRecommendationsResponseDto
import com.example.healtapp.data.network.dto.ai.AiRecognizedFoodResponseDto
import com.example.healtapp.data.network.dto.ai.MealPlanResponseDto
import com.example.healtapp.data.network.dto.ai.SleepSummaryRequestDto
import com.example.healtapp.data.network.dto.ai.SleepSummaryResponseDto
import com.example.healtapp.data.network.dto.ai.WorkoutPlanResponseDto
import com.example.healtapp.data.network.dto.ai.ProactiveTipResponseDto
import com.example.healtapp.data.network.dto.ai.AiRecognizeTextFoodRequestDto
import com.example.healtapp.data.network.toUserMessage
import com.example.healtapp.domain.repository.AiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import com.example.healtapp.data.network.dto.wellness.AIChatRequestDto
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class AiRepositoryImpl @Inject constructor(
    private val aiApi: AiApi,
) : AiRepository {

    override suspend fun getProactiveTip(): Result<ProactiveTipResponseDto> {
        return try {
            Result.success(aiApi.getProactiveTip())
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserMessage("Не удалось получить совет от AI")))
        }
    }

    override suspend fun getRecommendations(days: Int): Result<AIRecommendationsResponseDto> {
        return try {
            Result.success(aiApi.getRecommendations(days = days, useLlmTips = false))
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserMessage("Не удалось загрузить рекомендации")))
        }
    }

    override suspend fun getMealPlan(days: Int): Result<MealPlanResponseDto> {
        return try {
            Result.success(aiApi.getMealPlan(days))
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserMessage("Не удалось сгенерировать план питания")))
        }
    }

    override suspend fun getAiStatus(): Result<com.example.healtapp.data.network.dto.ai.AiStatusDto> {
        return try {
            Result.success(aiApi.getAiStatus())
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserMessage("Не удалось проверить статус AI")))
        }
    }

    override suspend fun getWorkoutPlan(days: Int): Result<WorkoutPlanResponseDto> {
        return try {
            Result.success(aiApi.getWorkoutPlan(days))
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserMessage("Не удалось сгенерировать план тренировок")))
        }
    }

    override suspend fun recognizeFood(imageFile: File): Result<AiRecognizedFoodResponseDto> {
        return try {
            val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
            Result.success(aiApi.recognizeFood(body))
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserMessage("Не удалось распознать еду")))
        }
    }

    override suspend fun recognizeFoodFromText(text: String): Result<AiRecognizedFoodResponseDto> {
        return try {
            val request = AiRecognizeTextFoodRequestDto(text = text)
            Result.success(aiApi.recognizeFoodFromText(request))
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserMessage("Не удалось распознать еду из текста")))
        }
    }

    override suspend fun streamChat(request: AIChatRequestDto): Result<Flow<String>> {
        return try {
            val responseBody = aiApi.streamChat(request)
            val streamFlow = flow {
                responseBody.byteStream().use { inputStream ->
                    val buffer = ByteArray(1024)
                    while (true) {
                        val bytesRead = inputStream.read(buffer)
                        if (bytesRead == -1) break
                        emit(String(buffer, 0, bytesRead))
                    }
                }
            }.flowOn(Dispatchers.IO)
            Result.success(streamFlow)
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserMessage("Не удалось получить потоковый ответ")))
        }
    }

    override suspend fun getSleepSummary(request: SleepSummaryRequestDto): Result<SleepSummaryResponseDto> {
        return try {
            Result.success(aiApi.getSleepSummary(request))
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserMessage("Не удалось сгенерировать саммари")))
        }
    }

    override suspend fun getDashboardHints(): Result<List<String>> {
        return try {
            val response = aiApi.getDashboardHints()
            Result.success(response.hints)
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserMessage("Не удалось загрузить подсказки")))
        }
    }
}