package com.example.healtapp.data.network.interceptor

import com.example.healtapp.core.common.AppRefreshBus
import com.example.healtapp.data.network.api.AuthApi
import com.example.healtapp.data.network.auth.TokenProvider
import com.example.healtapp.data.network.dto.auth.RefreshTokenRequestDto
import com.example.healtapp.data.preferences.TokenStorage
import dagger.Lazy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class TokenAuthenticator @Inject constructor(
    private val tokenProvider: TokenProvider,
    private val tokenStorage: TokenStorage,
    private val authApiProvider: Lazy<AuthApi>
) : Authenticator {

    private val sessionExpiredNotified = AtomicBoolean(false)

    override fun authenticate(route: Route?, response: Response): Request? {
        val currentToken = tokenProvider.getToken()
        
        // If there's no token, we can't refresh
        if (currentToken.isNullOrBlank()) {
            return null
        }

        // Check if the request already failed with the *same* token we currently have.
        // If it failed with a different token, another thread might have already refreshed it.
        val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")
        
        synchronized(this) {
            val newToken = tokenProvider.getToken()
            if (requestToken != newToken && !newToken.isNullOrBlank()) {
                // Token was already refreshed by another thread
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
            }

            // Need to actually perform refresh
            val refreshToken = tokenProvider.getRefreshToken()
            if (refreshToken.isNullOrBlank()) {
                notifySessionExpiredOnce()
                return null
            }

            return try {
                val authApi = authApiProvider.get()
                val tokenResponse = runBlocking(Dispatchers.IO) {
                    authApi.refreshToken(RefreshTokenRequestDto(refresh_token = refreshToken))
                }

                runBlocking(Dispatchers.IO) {
                    tokenStorage.saveTokens(tokenResponse.access_token, tokenResponse.refresh_token)
                }
                
                // Clear cached token so it will be fetched from storage or updated
                // The DataStoreTokenProvider automatically updates its var token when storage changes,
                // but just to be sure
                
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${tokenResponse.access_token}")
                    .build()
            } catch (e: Exception) {
                notifySessionExpiredOnce()
                null
            }
        }
    }

    private fun notifySessionExpiredOnce() {
        if (!sessionExpiredNotified.compareAndSet(false, true)) return
        tokenProvider.clearCachedToken()
        runBlocking(Dispatchers.IO) {
            tokenStorage.clearToken()
        }
        AppRefreshBus.notifySessionExpired()
    }
}
