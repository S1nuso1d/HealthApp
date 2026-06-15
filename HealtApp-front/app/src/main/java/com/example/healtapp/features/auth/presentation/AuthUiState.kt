package com.example.healtapp.features.auth.presentation

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val repeatPassword: String = "",
    val verificationCode: String = "",
    val registerStep: RegisterStep = RegisterStep.Credentials,
    val firstName: String = "",
    val lastName: String = "",
    val nickname: String = "",
    val birthDate: String = "",
    val isVegetarian: Boolean = false,
    val hasAllergies: Boolean = false,
    val allergiesText: String = "",
    val dietaryExclusions: Set<String> = emptySet(),
    val dietaryNotes: String = "",
    val healthConnectGranted: Boolean = false,
    val healthConnectSkipped: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val infoMessage: String? = null,
    val isAuthorized: Boolean = false,
) {
    val awaitingEmailVerification: Boolean
        get() = registerStep == RegisterStep.Verify
}
