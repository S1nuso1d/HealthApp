package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.gamification.AchievementsResponseDto
import retrofit2.http.GET

interface GamificationApi {
    @GET("achievements/me")
    suspend fun getMyAchievements(): AchievementsResponseDto
}
