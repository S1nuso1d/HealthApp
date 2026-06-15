package com.example.healtapp.domain.repository

import com.example.healtapp.data.network.dto.auth.RegisterProfileDraftDto

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Unit>
    /** @return сообщение с сервера (куда смотреть, если почта не настроена). */
    suspend fun sendRegistrationCode(email: String, password: String): Result<String>
    suspend fun confirmRegistration(
        email: String,
        password: String,
        code: String,
        profile: RegisterProfileDraftDto?,
    ): Result<Unit>
    suspend fun logout()
    suspend fun deleteAccount(password: String): Result<Unit>

    /** Одинаковое сообщение при любом email (антиenumeration). */
    suspend fun forgotPassword(email: String): Result<String>

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>
}