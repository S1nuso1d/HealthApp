package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.sleep.CreateSleepRequestDto
import com.example.healtapp.data.network.dto.sleep.SleepDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface SleepApi {

    @GET("sleep/history")
    suspend fun getSleepHistory(): List<SleepDto>

    @POST("sleep/")
    suspend fun addSleep(
        @Body request: CreateSleepRequestDto
    ): SleepDto
}