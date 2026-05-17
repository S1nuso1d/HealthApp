package com.example.healtapp.domain.repository

import com.example.healtapp.data.network.dto.sleep.CreateSleepRequestDto
import com.example.healtapp.data.network.dto.sleep.SleepDto

interface SleepRepository {
    suspend fun getSleepHistory(): Result<List<SleepDto>>
    suspend fun addSleep(request: CreateSleepRequestDto): Result<SleepDto>
    suspend fun updateSleep(id: Int, request: CreateSleepRequestDto): Result<SleepDto>
    suspend fun deleteSleep(id: Int): Result<Unit>
}
