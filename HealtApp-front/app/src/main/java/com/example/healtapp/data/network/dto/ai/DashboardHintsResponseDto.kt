package com.example.healtapp.data.network.dto.ai

import androidx.annotation.Keep

@Keep
data class DashboardHintsResponseDto(
    val hints: List<String>
)