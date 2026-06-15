package com.example.healtapp.data.repository

import com.example.healtapp.data.network.api.CycleApi
import com.example.healtapp.data.network.dto.cycle.CycleEntryCreateDto
import com.example.healtapp.data.network.dto.cycle.CycleEntryDto
import com.example.healtapp.data.network.dto.cycle.CycleEntryUpdateDto
import com.example.healtapp.domain.repository.CycleRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CycleRepositoryImpl @Inject constructor(
    private val api: CycleApi
) : CycleRepository {
    override suspend fun getEntries(): Result<List<CycleEntryDto>> {
        return try {
            Result.success(api.getEntries())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createEntry(request: CycleEntryCreateDto): Result<CycleEntryDto> {
        return try {
            Result.success(api.createEntry(request))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateEntry(id: Int, request: CycleEntryUpdateDto): Result<CycleEntryDto> {
        return try {
            Result.success(api.updateEntry(id, request))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteEntry(id: Int): Result<Unit> {
        return try {
            api.deleteEntry(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
