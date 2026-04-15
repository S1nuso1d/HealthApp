package com.example.healtapp.domain.repository

import com.example.healtapp.data.network.dto.hydration.HydrationDto
import com.example.healtapp.data.network.dto.hydration.HydrationSummaryDto

interface HydrationRepository {

    suspend fun getHydrationHistory(): Result<List<HydrationDto>>

    suspend fun getTodayHydrationSummary(): Result<HydrationSummaryDto>

    suspend fun addHydration(amountMl: Int): Result<HydrationDto>
}