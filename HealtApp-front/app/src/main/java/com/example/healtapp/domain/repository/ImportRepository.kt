package com.example.healtapp.domain.repository

import com.example.healtapp.data.network.dto.dataimport.CsvImportRequestDto
import com.example.healtapp.data.network.dto.dataimport.ImportBatchRequestDto
import com.example.healtapp.data.network.dto.dataimport.ImportBatchResponseDto

interface ImportRepository {
    suspend fun importBatch(request: ImportBatchRequestDto): Result<ImportBatchResponseDto>
    suspend fun importCsv(text: String): Result<ImportBatchResponseDto>
}
