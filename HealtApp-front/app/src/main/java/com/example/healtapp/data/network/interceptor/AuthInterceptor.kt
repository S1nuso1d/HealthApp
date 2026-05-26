package com.example.healtapp.data.network.interceptor

import com.example.healtapp.core.common.AppRefreshBus
import com.example.healtapp.data.network.auth.TokenProvider
import com.example.healtapp.data.preferences.TokenStorage
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenProvider: TokenProvider,
    private val tokenStorage: TokenStorage,
) : Interceptor {

    private val sessionExpiredNotified = AtomicBoolean(false)

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider.getToken()

        val request = chain.request().newBuilder().apply {
            if (!token.isNullOrBlank()) {
                addHeader("Authorization", "Bearer $token")
            }
        }.build()

        val response = chain.proceed(request)
        if (response.code == 401 && !token.isNullOrBlank()) {
            notifySessionExpiredOnce()
        } else if (response.isSuccessful) {
            sessionExpiredNotified.set(false)
        }
        return response
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