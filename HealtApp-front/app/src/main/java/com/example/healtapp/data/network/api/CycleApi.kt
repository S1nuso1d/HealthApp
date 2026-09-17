package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.cycle.CycleEntryCreateDto
import com.example.healtapp.data.network.dto.cycle.CycleEntryDto
import com.example.healtapp.data.network.dto.cycle.CycleEntryUpdateDto
import com.example.healtapp.data.network.dto.cycle.CycleInsightsDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface CycleApi {
    @GET("cycle/")
    suspend fun getEntries(): List<CycleEntryDto>

    @POST("cycle/")
    suspend fun createEntry(@Body request: CycleEntryCreateDto): CycleEntryDto

    @PUT("cycle/{id}")
    suspend fun updateEntry(@Path("id") id: Int, @Body request: CycleEntryUpdateDto): CycleEntryDto

    @DELETE("cycle/{id}")
    suspend fun deleteEntry(@Path("id") id: Int)

    @GET("cycle/insights")
    suspend fun getInsights(@Query("months") months: Int = 6): CycleInsightsDto
}
