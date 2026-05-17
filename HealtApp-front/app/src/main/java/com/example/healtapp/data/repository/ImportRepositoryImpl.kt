package com.example.healtapp.data.repository

import com.example.healtapp.data.network.api.ImportApi
import com.example.healtapp.data.network.dto.dataimport.CsvImportRequestDto
import com.example.healtapp.data.network.dto.dataimport.ImportBatchRequestDto
import com.example.healtapp.data.network.dto.dataimport.ImportBatchResponseDto
import com.example.healtapp.domain.repository.ImportRepository
import javax.inject.Inject

class ImportRepositoryImpl @Inject constructor(
    private val api: ImportApi,
) : ImportRepository {

    override suspend fun importBatch(request: ImportBatchRequestDto): Result<ImportBatchResponseDto> {
        return runCatching { api.importBatch(request) }
    }

    override suspend fun importCsv(text: String): Result<ImportBatchResponseDto> {
        return runCatching { api.importCsv(CsvImportRequestDto(text = text)) }
    }
}
