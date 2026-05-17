package com.example.healtapp.features.auth.presentation

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val repeatPassword: String = "",
    val verificationCode: String = "",
    /** true — ждём ввод кода с почты */
    val awaitingEmailVerification: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val infoMessage: String? = null,
    val isAuthorized: Boolean = false,
)
