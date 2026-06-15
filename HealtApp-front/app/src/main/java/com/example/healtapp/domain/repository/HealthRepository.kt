package com.example.healtapp.domain.repository

import com.example.healtapp.data.network.dto.health.PillDto

interface HealthRepository {
    suspend fun getPillReminders(): Result<List<PillDto>>
    suspend fun createPillReminder(pill: PillDto): Result<PillDto>
    suspend fun updatePillReminder(id: Int, pill: PillDto): Result<PillDto>
    suspend fun deletePillReminder(id: Int): Result<Unit>
}
