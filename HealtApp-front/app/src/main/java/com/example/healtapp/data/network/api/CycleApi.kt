package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.cycle.CycleEntryCreateDto
import com.example.healtapp.data.network.dto.cycle.CycleEntryDto
import com.example.healtapp.data.network.dto.cycle.CycleEntryUpdateDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface CycleApi {
    @GET("cycle/")
    suspend fun getEntries(): List<CycleEntryDto>

    @POST("cycle/")
    suspend fun createEntry(@Body request: CycleEntryCreateDto): CycleEntryDto

    @PUT("cycle/{id}")
    suspend fun updateEntry(@Path("id") id: Int, @Body request: CycleEntryUpdateDto): CycleEntryDto

    @DELETE("cycle/{id}")
    suspend fun deleteEntry(@Path("id") id: Int)
}
