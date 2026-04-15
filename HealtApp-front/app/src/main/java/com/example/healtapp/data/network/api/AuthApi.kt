package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.auth.RegisterRequestDto
import com.example.healtapp.data.network.dto.auth.TokenResponseDto
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface AuthApi {

    @FormUrlEncoded
    @POST("auth/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): TokenResponseDto

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequestDto
    ): TokenResponseDto
}