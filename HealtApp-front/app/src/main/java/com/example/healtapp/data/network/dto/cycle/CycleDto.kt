package com.example.healtapp.data.network.dto.cycle

import java.time.LocalDate
import java.time.LocalDateTime

data class CycleEntryDto(
    val id: Int,
    val user_id: Int,
    val start_date: String,
    val end_date: String?,
    val symptoms: String?,
    val notes: String?,
    val created_at: String,
    val updated_at: String?
)

data class CycleEntryCreateDto(
    val start_date: String,
    val end_date: String? = null,
    val symptoms: String? = null,
    val notes: String? = null
)

data class CycleEntryUpdateDto(
    val start_date: String? = null,
    val end_date: String? = null,
    val symptoms: String? = null,
    val notes: String? = null
)
