package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.health.HealthSampleDto
import retrofit2.http.GET
import retrofit2.http.Query

interface HealthApi {

    @GET("health/samples")
    suspend fun listSamples(
        @Query("days") days: Int = 30,
        @Query("metrics") metrics: String? = null,
    ): List<HealthSampleDto>
}
