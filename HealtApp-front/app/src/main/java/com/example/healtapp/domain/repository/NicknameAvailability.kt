package com.example.healtapp.domain.repository

data class NicknameAvailability(
    val available: Boolean,
    val message: String? = null,
)
