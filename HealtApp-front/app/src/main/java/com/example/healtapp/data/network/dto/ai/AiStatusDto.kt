package com.example.healtapp.data.network.dto.ai

data class AiStatusDto(
    val llm_enabled: Boolean,
    val llm_provider: String,
    val llm_model: String,
    val llm_available: Boolean,
    val fallback_enabled: Boolean,
    val message: String,
)
