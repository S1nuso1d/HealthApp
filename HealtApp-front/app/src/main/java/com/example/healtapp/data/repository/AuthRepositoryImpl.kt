package com.example.healtapp.data.repository

import com.example.healtapp.data.network.api.AuthApi
import com.example.healtapp.data.network.dto.auth.ChangePasswordRequestDto
import com.example.healtapp.data.network.dto.auth.ForgotPasswordRequestDto
import com.example.healtapp.data.network.dto.auth.PasswordConfirmDto
import com.example.healtapp.data.network.dto.auth.RegisterRequestDto
import com.example.healtapp.data.network.dto.auth.RegisterVerifyDto
import com.example.healtapp.data.network.realtime.RealtimeUpdatesClient
import com.example.healtapp.data.preferences.DashboardCache
import com.example.healtapp.data.preferences.ProfileCache
import com.example.healtapp.data.preferences.TokenStorage
import com.example.healtapp.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStorage: TokenStorage,
    private val profileCache: ProfileCache,
    private val dashboardCache: DashboardCache,
    private val realtimeUpdatesClient: RealtimeUpdatesClient,
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<Unit> {
        return runCatching {
            val response = authApi.login(
                username = email,
                password = password
            )
            tokenStorage.saveToken(response.access_token)
            realtimeUpdatesClient.start()
        }
    }

    override suspend fun sendRegistrationCode(email: String, password: String): Result<String> {
        return runCatching {
            authApi.registerStart(
                RegisterRequestDto(
                    email = email,
                    password = password,
                ),
            ).message
        }
    }

    override suspend fun confirmRegistration(email: String, password: String, code: String): Result<Unit> {
        return runCatching {
            val response = authApi.registerComplete(
                RegisterVerifyDto(
                    email = email,
                    password = password,
                    code = code.trim(),
                ),
            )
            tokenStorage.saveToken(response.access_token)
            realtimeUpdatesClient.start()
        }
    }

    override suspend fun logout() {
        realtimeUpdatesClient.stop()
        tokenStorage.clearToken()
        profileCache.clear()
        dashboardCache.clear()
    }

    override suspend fun deleteAccount(password: String): Result<Unit> {
        return runCatching {
            authApi.deleteAccount(PasswordConfirmDto(password = password))
            realtimeUpdatesClient.stop()
            tokenStorage.clearToken()
            profileCache.clear()
            dashboardCache.clear()
        }
    }

    override suspend fun forgotPassword(email: String): Result<String> {
        return runCatching {
            authApi.forgotPassword(ForgotPasswordRequestDto(email = email.trim())).message
        }
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return runCatching {
            authApi.changePassword(
                ChangePasswordRequestDto(
                    currentPassword = currentPassword,
                    newPassword = newPassword,
                ),
            )
            Unit
        }
    }
}