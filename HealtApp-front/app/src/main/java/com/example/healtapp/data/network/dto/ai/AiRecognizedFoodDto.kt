package com.example.healtapp.data.network.dto.ai

import com.google.gson.annotations.SerializedName

data class AiRecognizedFoodResponseDto(
    @SerializedName("items") val items: List<AiRecognizedFoodItemDto>,
)

data class AiRecognizedFoodItemDto(
    @SerializedName("name") val name: String,
    @SerializedName("grams") val grams: Int,
    @SerializedName("calories") val calories: Int,
    @SerializedName("protein") val protein: Double,
    @SerializedName("fat") val fat: Double,
    @SerializedName("carbs") val carbs: Double,
)
