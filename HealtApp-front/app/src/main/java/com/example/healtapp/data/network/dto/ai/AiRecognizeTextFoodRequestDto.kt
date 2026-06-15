package com.example.healtapp.data.network.dto.ai

import com.google.gson.annotations.SerializedName

data class AiRecognizeTextFoodRequestDto(
    @SerializedName("text") val text: String,
)