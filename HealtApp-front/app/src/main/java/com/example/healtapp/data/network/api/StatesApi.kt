package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.wellness.UserStateCreateDto
import com.example.healtapp.data.network.dto.wellness.UserStateDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface StatesApi {
    @POST("states/")
    suspend fun createState(@Body body: UserStateCreateDto): UserStateDto

    @GET("states/")
    suspend fun listStates(): List<UserStateDto>
}
