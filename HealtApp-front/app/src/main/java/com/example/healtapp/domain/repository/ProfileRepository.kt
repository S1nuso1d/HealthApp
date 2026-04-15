package com.example.healtapp.domain.repository

import com.example.healtapp.data.network.dto.profile.ProfileDto

interface ProfileRepository {
    suspend fun getMyProfile(): Result<ProfileDto>

    suspend fun updateMyProfile(
        age: Int?,
        sex: String?,
        heightCm: Float?,
        weightKg: Float?,
        goal: String?,
        activityLevel: String?,
        targetSleepHours: Float?,
        targetWaterMl: Float?
    ): Result<ProfileDto>
}