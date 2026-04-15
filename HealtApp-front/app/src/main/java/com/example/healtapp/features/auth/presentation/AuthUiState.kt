package com.example.healtapp.features.auth.presentation

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val repeatPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthorized: Boolean = false
)