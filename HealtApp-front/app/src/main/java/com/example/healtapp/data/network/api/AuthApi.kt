package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.auth.ChangePasswordRequestDto
import com.example.healtapp.data.network.dto.auth.ForgotPasswordRequestDto
import com.example.healtapp.data.network.dto.auth.PasswordConfirmDto
import com.example.healtapp.data.network.dto.auth.RegisterRequestDto
import com.example.healtapp.data.network.dto.auth.RegisterStartResponseDto
import com.example.healtapp.data.network.dto.auth.RegisterVerifyDto
import com.example.healtapp.data.network.dto.auth.TokenResponseDto
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/delete-account")
    suspend fun deleteAccount(@Body body: PasswordConfirmDto): Map<String, String>

    @FormUrlEncoded
    @POST("auth/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): TokenResponseDto

    @POST("auth/register/start")
    suspend fun registerStart(@Body body: RegisterRequestDto): RegisterStartResponseDto

    @POST("auth/register/complete")
    suspend fun registerComplete(@Body body: RegisterVerifyDto): TokenResponseDto

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequestDto): RegisterStartResponseDto

    @POST("auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequestDto): Map<String, String>
}