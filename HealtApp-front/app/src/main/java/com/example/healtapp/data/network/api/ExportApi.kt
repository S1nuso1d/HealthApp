package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.export.ExportReportDto
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming

interface ExportApi {
    @GET("export/report")
    suspend fun getReport(@Query("days") days: Int = 30): ExportReportDto

    @Streaming
    @GET("export/csv")
    suspend fun getCsv(@Query("days") days: Int = 90): ResponseBody
}
