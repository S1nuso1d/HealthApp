package com.example.healtapp.data.repository

import com.example.healtapp.data.network.api.ProfileApi
import com.example.healtapp.data.network.dto.profile.ProfileDto
import com.example.healtapp.data.network.dto.profile.UpdateProfileRequestDto
import com.example.healtapp.domain.repository.ProfileRepository
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
        targetWaterMl: Float?
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
                    target_water_ml = targetWaterMl
                )
            )
        }
    }
}