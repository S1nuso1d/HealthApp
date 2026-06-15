package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.health.HealthSampleDto
import com.example.healtapp.data.network.dto.health.PillDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface HealthApi {

    @GET("health/samples")
    suspend fun listSamples(
        @Query("days") days: Int = 30,
        @Query("metrics") metrics: String? = null,
    ): List<HealthSampleDto>

    @GET("pills/")
    suspend fun getPillReminders(): List<PillDto>

    @POST("pills/")
    suspend fun createPillReminder(@Body request: PillDto): PillDto

    @PUT("pills/{id}")
    suspend fun updatePillReminder(@Path("id") id: Int, @Body request: PillDto): PillDto

    @DELETE("pills/{id}")
    suspend fun deletePillReminder(@Path("id") id: Int)
}
