package com.example.healtapp.domain.repository

import com.example.healtapp.data.network.dto.hydration.HydrationDto
import com.example.healtapp.data.network.dto.hydration.HydrationSummaryDto

interface HydrationRepository {

    suspend fun getHydrationHistory(): Result<List<HydrationDto>>

    suspend fun getTodayHydrationSummary(): Result<HydrationSummaryDto>

    suspend fun addHydration(amountMl: Int): Result<HydrationDto>

    suspend fun updateHydration(id: Int, amountMl: Int, recordTimeIso: String?): Result<HydrationDto>

    suspend fun deleteHydration(id: Int): Result<Unit>
}
