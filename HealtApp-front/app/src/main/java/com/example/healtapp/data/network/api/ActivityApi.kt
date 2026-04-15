package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.activity.ActivityCreateRequestDto
import com.example.healtapp.data.network.dto.activity.ActivityDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ActivityApi {

    @GET("activity/today")
    suspend fun getTodayActivity(): ActivityDto?

    @GET("activity/history")
    suspend fun getActivityHistory(): List<ActivityDto>

    @POST("activity/")
    suspend fun createActivity(
        @Body request: ActivityCreateRequestDto
    ): ActivityDto
}