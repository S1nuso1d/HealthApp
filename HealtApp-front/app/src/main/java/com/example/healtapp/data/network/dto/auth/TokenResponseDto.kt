package com.example.healtapp.data.network.dto.auth

data class TokenResponseDto(
    val access_token: String,
    val token_type: String
)