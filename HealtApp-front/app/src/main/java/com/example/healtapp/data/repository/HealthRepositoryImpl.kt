package com.example.healtapp.data.repository

import com.example.healtapp.data.network.api.HealthApi
import com.example.healtapp.data.network.dto.health.PillDto
import com.example.healtapp.domain.repository.HealthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRepositoryImpl @Inject constructor(
    private val api: HealthApi
) : HealthRepository {

    override suspend fun getPillReminders(): Result<List<PillDto>> = runCatching {
        api.getPillReminders()
    }

    override suspend fun createPillReminder(pill: PillDto): Result<PillDto> = runCatching {
        api.createPillReminder(pill)
    }

    override suspend fun updatePillReminder(id: Int, pill: PillDto): Result<PillDto> = runCatching {
        api.updatePillReminder(id, pill)
    }

    override suspend fun deletePillReminder(id: Int): Result<Unit> = runCatching {
        api.deletePillReminder(id)
    }
}
