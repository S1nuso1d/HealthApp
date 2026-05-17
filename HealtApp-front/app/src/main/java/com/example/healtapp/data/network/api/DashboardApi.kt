package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.wellness.DashboardHomeDto
import retrofit2.http.GET
import retrofit2.http.Query

interface DashboardApi {
    @GET("dashboard/home")
    suspend fun getHome(@Query("days") days: Int = 7): DashboardHomeDto
}
