package com.example.healtapp.features.auth.presentation

sealed interface AuthEvent {
    data class EmailChanged(val value: String) : AuthEvent
    data class PasswordChanged(val value: String) : AuthEvent
    data class RepeatPasswordChanged(val value: String) : AuthEvent
    data class VerificationCodeChanged(val value: String) : AuthEvent
    data object SubmitLogin : AuthEvent
    /** Отправить код на почту (шаг 1 регистрации) */
    data object SubmitRegisterSendCode : AuthEvent
    /** Подтвердить код и создать аккаунт */
    data object SubmitRegisterConfirm : AuthEvent
    /** Вернуться к редактированию email/пароля */
    data object RegisterEditCredentials : AuthEvent
}
