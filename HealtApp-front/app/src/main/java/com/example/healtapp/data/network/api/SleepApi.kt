package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.sleep.CreateSleepRequestDto
import com.example.healtapp.data.network.dto.sleep.SleepDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface SleepApi {

    @GET("sleep/history")
    suspend fun getSleepHistory(): List<SleepDto>

    @POST("sleep/")
    suspend fun addSleep(
        @Body request: CreateSleepRequestDto
    ): SleepDto

    @PUT("sleep/{id}")
    suspend fun updateSleep(
        @Path("id") id: Int,
        @Body request: CreateSleepRequestDto
    ): SleepDto

    @DELETE("sleep/{id}")
    suspend fun deleteSleep(@Path("id") id: Int): Map<String, String>
}
