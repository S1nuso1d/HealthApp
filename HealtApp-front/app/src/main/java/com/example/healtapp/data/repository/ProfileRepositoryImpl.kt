package com.example.healtapp.data.repository

import com.example.healtapp.data.network.api.ProfileApi
import com.example.healtapp.data.network.dto.profile.ProfileDto
import com.example.healtapp.data.network.dto.profile.NutritionTargetsUpdateDto
import com.example.healtapp.data.network.dto.profile.UpdateProfileRequestDto
import com.example.healtapp.data.preferences.ProfileCache
import com.example.healtapp.domain.repository.ProfileRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val profileApi: ProfileApi,
    private val profileCache: ProfileCache,
) : ProfileRepository {

    override suspend fun getMyProfile(): Result<ProfileDto> = runCatching {
        profileApi.getMyProfile().also { profileCache.save(it) }
    }.recoverCatching {
        profileCache.load() ?: throw it
    }

    private suspend fun saveProfileResult(result: Result<ProfileDto>): Result<ProfileDto> =
        result.onSuccess { profileCache.save(it) }

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
        isVegetarian: Boolean?,
        hasAllergies: Boolean?,
        allergiesText: String?,
        onboardingCompleted: Boolean?,
    ): Result<ProfileDto> = saveProfileResult(runCatching {
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
                    is_vegetarian = isVegetarian,
                    has_allergies = hasAllergies,
                    allergies_text = allergiesText,
                    onboarding_completed = onboardingCompleted,
                )
            )
        })

    override suspend fun uploadAvatar(imageBytes: ByteArray, mimeType: String): Result<ProfileDto> =
        saveProfileResult(runCatching {
            val media = (mimeType.ifBlank { "image/jpeg" }).toMediaType()
            val body = imageBytes.toRequestBody(media)
            val part = MultipartBody.Part.createFormData("file", "avatar.jpg", body)
            profileApi.uploadAvatar(part)
        })

    override suspend fun deleteAvatar(): Result<ProfileDto> = saveProfileResult(runCatching {
            profileApi.deleteAvatar()
        })

    override suspend fun updateNutritionTargets(
        targetDailyCalories: Int,
        targetProteinG: Float,
        targetFatG: Float,
        targetCarbsG: Float,
    ): Result<ProfileDto> = saveProfileResult(runCatching {
            profileApi.updateNutritionTargets(
                NutritionTargetsUpdateDto(
                    target_daily_calories = targetDailyCalories,
                    target_protein_g = targetProteinG,
                    target_fat_g = targetFatG,
                    target_carbs_g = targetCarbsG,
                ),
            )
        })
}