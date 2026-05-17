package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.wellness.AnalysisRunDto
import com.example.healtapp.data.network.dto.wellness.AnalyticsResponseDto
import com.example.healtapp.data.network.dto.wellness.InsightItemDto
import retrofit2.http.GET
import retrofit2.http.Query

interface AnalyticsApi {
    @GET("analytics/overview")
    suspend fun getOverview(@Query("days") days: Int = 7): AnalyticsResponseDto

    @GET("analytics/insights")
    suspend fun getInsights(): List<InsightItemDto>

    @GET("analytics/runs")
    suspend fun getRuns(@Query("limit") limit: Int = 20): List<AnalysisRunDto>
}
