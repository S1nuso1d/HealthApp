package com.example.healtapp.features.hydration.presentation

import com.example.healtapp.data.network.dto.hydration.HydrationDto
import com.example.healtapp.data.preferences.HydrationPrefs

data class HydrationUiState(
    val waterToday: Int = 0,
    val target: Int = 2500,
    val todayRecords: List<HydrationDto> = emptyList(),
    val defaultQuickAmounts: List<Int> = HydrationPrefs.DEFAULT_QUICK_AMOUNTS,
    val customQuickAmounts: List<Int> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val progressCelebrateToken: Int = 0,
    val pendingSyncCount: Int = 0,
)
