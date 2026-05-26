package com.example.healtapp.data.repository

import com.example.healtapp.data.network.api.GamificationApi
import com.example.healtapp.domain.repository.GamificationRepository
import javax.inject.Inject

class GamificationRepositoryImpl @Inject constructor(
    private val api: GamificationApi,
) : GamificationRepository {
    override suspend fun getMyAchievements() = runCatching { api.getMyAchievements() }
}
