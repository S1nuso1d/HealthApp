package com.example.healtapp.data.network.dto.hydration

data class HydrationSummaryDto(
    val total_ml: Int = 0,
    val records: List<HydrationDto> = emptyList()
)