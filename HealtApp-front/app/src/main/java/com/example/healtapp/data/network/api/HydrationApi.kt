package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.hydration.CreateHydrationRequestDto
import com.example.healtapp.data.network.dto.hydration.HydrationDto
import com.example.healtapp.data.network.dto.hydration.HydrationSummaryDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface HydrationApi {

    @GET("hydration/history")
    suspend fun getHydrationHistory(): List<HydrationDto>

    @GET("hydration/today")
    suspend fun getTodayHydrationSummary(): HydrationSummaryDto

    @POST("hydration/")
    suspend fun addHydration(
        @Body request: CreateHydrationRequestDto
    ): HydrationDto

    @PUT("hydration/{id}")
    suspend fun updateHydration(
        @Path("id") id: Int,
        @Body request: CreateHydrationRequestDto
    ): HydrationDto

    @DELETE("hydration/{id}")
    suspend fun deleteHydration(@Path("id") id: Int): Map<String, String>
}
