package com.example.healtapp.data.network.dto.auth

data class NicknameCheckResponseDto(
    val available: Boolean,
    val message: String? = null,
)
