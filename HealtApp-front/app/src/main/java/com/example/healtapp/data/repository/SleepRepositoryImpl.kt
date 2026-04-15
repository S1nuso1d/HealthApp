package com.example.healtapp.data.repository

import com.example.healtapp.data.network.api.SleepApi
import com.example.healtapp.data.network.dto.sleep.CreateSleepRequestDto
import com.example.healtapp.data.network.dto.sleep.SleepDto
import com.example.healtapp.domain.repository.SleepRepository

class SleepRepositoryImpl(
    private val sleepApi: SleepApi
) : SleepRepository {

    override suspend fun getSleepHistory(): Result<List<SleepDto>> {
        return runCatching {
            sleepApi.getSleepHistory()
        }
    }

    override suspend fun addSleep(request: CreateSleepRequestDto): Result<SleepDto> {
        return runCatching {
            sleepApi.addSleep(request)
        }
    }
}