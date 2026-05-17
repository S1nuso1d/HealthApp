package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.dataimport.CsvImportRequestDto
import com.example.healtapp.data.network.dto.dataimport.ImportBatchRequestDto
import com.example.healtapp.data.network.dto.dataimport.ImportBatchResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface ImportApi {

    @POST("import/batch")
    suspend fun importBatch(@Body body: ImportBatchRequestDto): ImportBatchResponseDto

    @POST("import/csv")
    suspend fun importCsv(@Body body: CsvImportRequestDto): ImportBatchResponseDto
}
