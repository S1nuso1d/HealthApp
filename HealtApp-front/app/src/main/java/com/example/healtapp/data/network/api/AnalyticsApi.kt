package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.analytics.HabitExperimentDto
import com.example.healtapp.data.network.dto.analytics.HabitExperimentsResponseDto
import com.example.healtapp.data.network.dto.analytics.TonightRiskDto
import com.example.healtapp.data.network.dto.wellness.AnalysisRunDto
import com.example.healtapp.data.network.dto.wellness.AnalyticsResponseDto
import com.example.healtapp.data.network.dto.wellness.InsightItemDto
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AnalyticsApi {
    @GET("analytics/overview")
    suspend fun getOverview(@Query("days") days: Int = 7): AnalyticsResponseDto

    @GET("analytics/insights")
    suspend fun getInsights(): List<InsightItemDto>

    @GET("analytics/runs")
    suspend fun getRuns(@Query("limit") limit: Int = 20): List<AnalysisRunDto>

    @GET("analytics/influence-factors")
    suspend fun getInfluenceFactors(@Query("days") days: Int = 14): com.example.healtapp.data.network.dto.analytics.InfluenceFactorsResponseDto

    @GET("analytics/compare")
    suspend fun compareRuns(): com.example.healtapp.data.network.dto.analytics.AnalyticsCompareDto

    @GET("analytics/tonight-risk")
    suspend fun getTonightRisk(): TonightRiskDto

    @GET("analytics/circadian")
    suspend fun getCircadian(): com.example.healtapp.data.network.dto.analytics.CircadianProfileDto

    @GET("analytics/experiments")
    suspend fun getHabitExperiments(): HabitExperimentsResponseDto

    @POST("analytics/experiments")
    suspend fun startHabitExperiment(
        @Query("factor_id") factorId: String,
        @Query("title") title: String? = null,
        @Query("action") action: String? = null,
    ): HabitExperimentDto

    @POST("analytics/experiments/{id}/cancel")
    suspend fun cancelHabitExperiment(@Path("id") id: Int): HabitExperimentDto
}
