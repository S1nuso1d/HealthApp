package com.example.healtapp.data.network.dto.health

import com.google.gson.annotations.SerializedName

data class PillDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("name") val name: String,
    @SerializedName("dosage") val dosage: String,
    @SerializedName("time_of_day") val timeOfDay: String, // format "HH:mm:ss"
    @SerializedName("is_active") val isActive: Boolean = true
)
