package com.example.healtapp.features.hydration.presentation

data class HydrationUiState(
    val waterToday: Int = 0,
    val target: Int = 2500,
    val isLoading: Boolean = false,
    val error: String? = null
)