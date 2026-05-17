package com.example.healtapp.data.network.dto.auth

data class RegisterVerifyDto(
    val email: String,
    val password: String,
    val code: String,
)
