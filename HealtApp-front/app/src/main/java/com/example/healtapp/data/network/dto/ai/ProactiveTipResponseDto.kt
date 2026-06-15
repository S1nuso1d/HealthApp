package com.example.healtapp.data.network.dto.ai

import com.google.gson.annotations.SerializedName

data class ProactiveTipResponseDto(
    @SerializedName("tip") val tip: String,
    @SerializedName("generated_at") val generatedAt: String,
)
