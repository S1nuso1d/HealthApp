package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.profile.ProfileDto
import com.example.healtapp.data.network.dto.profile.UpdateProfileRequestDto
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part

interface ProfileApi {

    @GET("profile/me")
    suspend fun getMyProfile(): ProfileDto

    @PUT("profile/me")
    suspend fun updateMyProfile(
        @Body request: UpdateProfileRequestDto
    ): ProfileDto

    @Multipart
    @POST("profile/me/avatar")
    suspend fun uploadAvatar(
        @Part file: MultipartBody.Part,
    ): ProfileDto

    @DELETE("profile/me/avatar")
    suspend fun deleteAvatar(): ProfileDto
}