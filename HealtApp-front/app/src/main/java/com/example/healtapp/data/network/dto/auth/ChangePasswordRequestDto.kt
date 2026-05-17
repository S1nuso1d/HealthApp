package com.example.healtapp.data.network.dto.auth

import com.google.gson.annotations.SerializedName

data class ChangePasswordRequestDto(
    @SerializedName("current_password")
    val currentPassword: String,
    @SerializedName("new_password")
    val newPassword: String,
)
