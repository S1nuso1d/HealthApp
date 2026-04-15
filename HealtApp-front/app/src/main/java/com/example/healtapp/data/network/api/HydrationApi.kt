package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.hydration.CreateHydrationRequestDto
import com.example.healtapp.data.network.dto.hydration.HydrationDto
import com.example.healtapp.data.network.dto.hydration.HydrationSummaryDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface HydrationApi {

    @GET("hydration/history")
    suspend fun getHydrationHistory(): List<HydrationDto>

    @GET("hydration/today")
    suspend fun getTodayHydrationSummary(): HydrationSummaryDto

    @POST("hydration/")
    suspend fun addHydration(
        @Body request: CreateHydrationRequestDto
    ): HydrationDto
}