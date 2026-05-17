package com.example.healtapp.data.network.dto.hydration

data class CreateHydrationRequestDto(
    val amount_ml: Int,
    val record_time: String? = null,
    val source: String? = "manual",
)
