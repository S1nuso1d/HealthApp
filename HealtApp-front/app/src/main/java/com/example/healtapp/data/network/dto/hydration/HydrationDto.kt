package com.example.healtapp.data.network.dto.hydration

data class HydrationDto(
    val id: Int,
    val user_id: Int,
    val amount_ml: Int,
    val record_time: String,
    val source: String? = null
)