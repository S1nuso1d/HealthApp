package com.example.healtapp.data.repository

import com.example.healtapp.data.network.api.ProfileApi
import com.example.healtapp.data.network.dto.profile.ProfileDto
import com.example.healtapp.data.network.dto.profile.UpdateProfileRequestDto
import com.example.healtapp.domain.repository.ProfileRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val profileApi: ProfileApi,
) : ProfileRepository {

    override suspend fun getMyProfile(): Result<ProfileDto> {
        return runCatching {
            profileApi.getMyProfile()
        }
    }

    override suspend fun updateMyProfile(
        age: Int?,
        sex: String?,
        heightCm: Float?,
        weightKg: Float?,
        goal: String?,
        activityLevel: String?,
        targetSleepHours: Float?,
        targetWaterMl: Float?,
        targetDailyCalories: Int?,
        targetProteinG: Float?,
        targetFatG: Float?,
        targetCarbsG: Float?,
        targetSteps: Int?,
    ): Result<ProfileDto> {
        return runCatching {
            profileApi.updateMyProfile(
                UpdateProfileRequestDto(
                    age = age,
                    sex = sex,
                    height_cm = heightCm,
                    weight_kg = weightKg,
                    goal = goal,
                    activity_level = activityLevel,
                    target_sleep_hours = targetSleepHours,
                    target_water_ml = targetWaterMl,
                    target_daily_calories = targetDailyCalories,
                    target_protein_g = targetProteinG,
                    target_fat_g = targetFatG,
                    target_carbs_g = targetCarbsG,
                    target_steps = targetSteps,
                )
            )
        }
    }

    override suspend fun uploadAvatar(imageBytes: ByteArray, mimeType: String): Result<ProfileDto> {
        return runCatching {
            val media = (mimeType.ifBlank { "image/jpeg" }).toMediaType()
            val body = imageBytes.toRequestBody(media)
            val part = MultipartBody.Part.createFormData("file", "avatar.jpg", body)
            profileApi.uploadAvatar(part)
        }
    }

    override suspend fun deleteAvatar(): Result<ProfileDto> {
        return runCatching {
            profileApi.deleteAvatar()
        }
    }
}