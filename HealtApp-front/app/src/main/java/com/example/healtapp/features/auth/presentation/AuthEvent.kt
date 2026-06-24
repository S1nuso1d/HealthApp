package com.example.healtapp.features.auth.presentation

sealed interface AuthEvent {
    data class EmailChanged(val value: String) : AuthEvent
    data class PasswordChanged(val value: String) : AuthEvent
    data class RepeatPasswordChanged(val value: String) : AuthEvent
    data class VerificationCodeChanged(val value: String) : AuthEvent
    data class FirstNameChanged(val value: String) : AuthEvent
    data class LastNameChanged(val value: String) : AuthEvent
    data class NicknameChanged(val value: String) : AuthEvent
    data class SexChanged(val value: String) : AuthEvent
    data class HeightChanged(val value: String) : AuthEvent
    data class WeightChanged(val value: String) : AuthEvent
    data class GoalChanged(val value: String) : AuthEvent
    data class BirthDateChanged(val value: String) : AuthEvent
    data class IsVegetarianChanged(val value: Boolean) : AuthEvent
    data class HasAllergiesChanged(val value: Boolean) : AuthEvent
    data class AllergiesTextChanged(val value: String) : AuthEvent
    data class DietaryNotesChanged(val value: String) : AuthEvent
    data class DietaryExclusionToggled(val id: String) : AuthEvent
    data object SubmitLogin : AuthEvent
    data object SubmitRegisterNext : AuthEvent
    data object SubmitRegisterBack : AuthEvent
    data object HealthConnectSkip : AuthEvent
    data class HealthConnectPermissionsResult(val granted: Boolean) : AuthEvent
    data object SubmitRegisterSendCode : AuthEvent
    data object SubmitRegisterConfirm : AuthEvent
    data object RegisterEditCredentials : AuthEvent
}
