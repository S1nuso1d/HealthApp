package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.activity.ActivityCreateRequestDto
import com.example.healtapp.data.network.dto.activity.ActivityDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ActivityApi {

    @GET("activity/today")
    suspend fun getTodayActivity(): ActivityDto?

    @GET("activity/history")
    suspend fun getActivityHistory(): List<ActivityDto>

    @POST("activity/")
    suspend fun createActivity(
        @Body request: ActivityCreateRequestDto
    ): ActivityDto

    @PUT("activity/{id}")
    suspend fun updateActivity(
        @Path("id") id: Int,
        @Body request: ActivityCreateRequestDto
    ): ActivityDto

    @DELETE("activity/{id}")
    suspend fun deleteActivity(@Path("id") id: Int): Map<String, String>
}
