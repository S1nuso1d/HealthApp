package com.example.healtapp.domain.usecase.profile

import com.example.healtapp.data.network.dto.profile.ProfileDto
import com.example.healtapp.domain.repository.ProfileRepository
import javax.inject.Inject

class UpdateProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(
        firstName: String? = null,
        lastName: String? = null,
        nickname: String? = null,
        age: Int? = null,
        sex: String? = null,
        heightCm: Float? = null,
        weightKg: Float? = null,
        goal: String? = null,
        activityLevel: String? = null,
        targetSleepHours: Float? = null,
        targetWaterMl: Float? = null,
        targetDailyCalories: Int? = null,
        targetProteinG: Float? = null,
        targetFatG: Float? = null,
        targetCarbsG: Float? = null,
        targetSteps: Int? = null,
        isVegetarian: Boolean? = null,
        hasAllergies: Boolean? = null,
        allergiesText: String? = null,
    ): Result<ProfileDto> {
        return profileRepository.updateMyProfile(
            firstName = firstName,
            lastName = lastName,
            nickname = nickname,
            age = age,
            sex = sex,
            heightCm = heightCm,
            weightKg = weightKg,
            goal = goal,
            activityLevel = activityLevel,
            targetSleepHours = targetSleepHours,
            targetWaterMl = targetWaterMl,
            targetDailyCalories = targetDailyCalories,
            targetProteinG = targetProteinG,
            targetFatG = targetFatG,
            targetCarbsG = targetCarbsG,
            targetSteps = targetSteps,
            isVegetarian = isVegetarian,
            hasAllergies = hasAllergies,
            allergiesText = allergiesText
        )
    }
}
