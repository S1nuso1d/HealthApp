package com.example.healtapp.data.network.interceptor

import com.example.healtapp.data.network.ApiServerConfig
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Подменяет host/scheme/port запроса на актуальный [ApiServerConfig.baseUrl].
 * Retrofit собирается с фиктивным baseUrl — реальный адрес задаётся в настройках.
 */
@Singleton
class DynamicBaseUrlInterceptor @Inject constructor(
    private val serverConfig: ApiServerConfig,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val configured = serverConfig.baseUrl().toHttpUrl()
        val original = request.url

        val newUrl = original.newBuilder()
            .scheme(configured.scheme)
            .host(configured.host)
            .port(configured.port)
            .build()

        return chain.proceed(
            request.newBuilder().url(newUrl).build(),
        )
    }
}
