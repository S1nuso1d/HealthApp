package com.example.healtapp.domain.repository

import com.example.healtapp.data.network.dto.gamification.AchievementsResponseDto

interface GamificationRepository {
    suspend fun getMyAchievements(): Result<AchievementsResponseDto>
}
