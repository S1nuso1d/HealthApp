package com.example.healtapp.data.repository

import com.example.healtapp.data.network.api.HydrationApi
import com.example.healtapp.data.network.dto.hydration.CreateHydrationRequestDto
import com.example.healtapp.data.network.dto.hydration.HydrationDto
import com.example.healtapp.data.network.dto.hydration.HydrationSummaryDto
import com.example.healtapp.domain.repository.HydrationRepository

class HydrationRepositoryImpl(
    private val hydrationApi: HydrationApi
) : HydrationRepository {

    override suspend fun getHydrationHistory(): Result<List<HydrationDto>> {
        return runCatching {
            hydrationApi.getHydrationHistory()
        }
    }

    override suspend fun getTodayHydrationSummary(): Result<HydrationSummaryDto> {
        return runCatching {
            hydrationApi.getTodayHydrationSummary()
        }
    }

    override suspend fun addHydration(amountMl: Int): Result<HydrationDto> {
        return runCatching {
            hydrationApi.addHydration(
                CreateHydrationRequestDto(
                    amount_ml = amountMl
                )
            )
        }
    }
}