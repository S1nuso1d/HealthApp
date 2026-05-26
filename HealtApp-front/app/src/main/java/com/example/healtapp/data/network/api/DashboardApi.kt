package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.dashboard.GoalsCalendarDto
import com.example.healtapp.data.network.dto.wellness.DashboardHomeDto
import retrofit2.http.GET
import retrofit2.http.Query

interface DashboardApi {
    @GET("dashboard/home")
    suspend fun getHome(@Query("days") days: Int = 7): DashboardHomeDto

    @GET("dashboard/goals-calendar")
    suspend fun getGoalsCalendar(
        @Query("year") year: Int? = null,
        @Query("month") month: Int? = null,
        @Query("days") days: Int? = null,
    ): GoalsCalendarDto
}
