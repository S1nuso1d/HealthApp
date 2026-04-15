package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.profile.ProfileDto
import com.example.healtapp.data.network.dto.profile.UpdateProfileRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface ProfileApi {

    @GET("profile/me")
    suspend fun getMyProfile(): ProfileDto

    @PUT("profile/me")
    suspend fun updateMyProfile(
        @Body request: UpdateProfileRequestDto
    ): ProfileDto
}