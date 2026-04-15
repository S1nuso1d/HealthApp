package com.example.healtapp.data.repository

import com.example.healtapp.data.network.api.AuthApi
import com.example.healtapp.data.network.dto.auth.RegisterRequestDto
import com.example.healtapp.data.preferences.TokenStorage
import com.example.healtapp.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val authApi: AuthApi,
    private val tokenStorage: TokenStorage
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<Unit> {
        return runCatching {
            val response = authApi.login(
                username = email,
                password = password
            )
            tokenStorage.saveToken(response.access_token)
        }
    }

    override suspend fun register(email: String, password: String): Result<Unit> {
        return runCatching {
            val response = authApi.register(
                RegisterRequestDto(
                    email = email,
                    password = password
                )
            )
            tokenStorage.saveToken(response.access_token)
        }
    }

    override suspend fun logout() {
        tokenStorage.clearToken()
    }
}