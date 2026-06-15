package com.example.healtapp.domain.repository

import com.example.healtapp.data.network.dto.cycle.CycleEntryCreateDto
import com.example.healtapp.data.network.dto.cycle.CycleEntryDto
import com.example.healtapp.data.network.dto.cycle.CycleEntryUpdateDto

interface CycleRepository {
    suspend fun getEntries(): Result<List<CycleEntryDto>>
    suspend fun createEntry(request: CycleEntryCreateDto): Result<CycleEntryDto>
    suspend fun updateEntry(id: Int, request: CycleEntryUpdateDto): Result<CycleEntryDto>
    suspend fun deleteEntry(id: Int): Result<Unit>
}
