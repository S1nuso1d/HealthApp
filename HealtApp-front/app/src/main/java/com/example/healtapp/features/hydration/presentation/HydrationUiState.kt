package com.example.healtapp.features.hydration.presentation

import com.example.healtapp.data.network.dto.hydration.HydrationDto

data class HydrationUiState(
    val waterToday: Int = 0,
    val target: Int = 2500,
    val todayRecords: List<HydrationDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
