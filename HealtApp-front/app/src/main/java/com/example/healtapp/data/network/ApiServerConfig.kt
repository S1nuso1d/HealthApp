package com.example.healtapp.data.network

import com.example.healtapp.BuildConfig
import com.example.healtapp.data.network.auth.ApplicationScope
import com.example.healtapp.data.preferences.ApiServerPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiServerConfig @Inject constructor(
    private val preferences: ApiServerPreferences,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    val buildTimeDefault: String = normalizeBaseUrl(BuildConfig.BASE_URL)

    private val activeUrl = AtomicReference(buildTimeDefault)

    init {
        applicationScope.launch {
            if (SERVER_OVERRIDE_ENABLED) {
                preferences.getOverride()?.let { activeUrl.set(normalizeBaseUrl(it)) }
            } else {
                preferences.setOverride(null)
                activeUrl.set(buildTimeDefault)
            }
        }
        applicationScope.launch {
            preferences.overrideFlow
                .distinctUntilChanged()
                .collect { override ->
                    activeUrl.set(
                        if (SERVER_OVERRIDE_ENABLED) {
                            normalizeBaseUrl(override ?: buildTimeDefault)
                        } else {
                            buildTimeDefault
                        },
                    )
                }
        }
    }

    fun baseUrl(): String = activeUrl.get()

    fun webSocketBase(): String = baseUrl()
        .trimEnd('/')
        .replaceFirst("https://", "wss://")
        .replaceFirst("http://", "ws://")

    fun avatarBase(): String = baseUrl().trimEnd('/')

    fun isServerOverrideEnabled(): Boolean = SERVER_OVERRIDE_ENABLED

    suspend fun applyOverride(raw: String?) {
        if (!SERVER_OVERRIDE_ENABLED) {
            preferences.setOverride(null)
            activeUrl.set(buildTimeDefault)
            return
        }
        val normalized = raw?.trim()?.takeIf { it.isNotEmpty() }?.let { normalizeBaseUrl(it) }
        if (normalized == null) {
            preferences.setOverride(null)
            activeUrl.set(buildTimeDefault)
        } else {
            preferences.setOverride(normalized)
            activeUrl.set(normalized)
        }
    }

    suspend fun clearOverride() = applyOverride(null)

    companion object {
        val SERVER_OVERRIDE_ENABLED: Boolean = BuildConfig.SERVER_OVERRIDE_ENABLED

        fun normalizeBaseUrl(raw: String): String {
            var s = raw.trim()
            if (s.isEmpty()) return BuildConfig.BASE_URL
            if (!s.startsWith("http://", ignoreCase = true) &&
                !s.startsWith("https://", ignoreCase = true)
            ) {
                // В release без явной схемы — только HTTPS.
                s = if (BuildConfig.ALLOW_CLEARTEXT) "http://$s" else "https://$s"
            }
            if (!BuildConfig.ALLOW_CLEARTEXT && s.startsWith("http://", ignoreCase = true)) {
                throw IllegalArgumentException("В release разрешён только HTTPS")
            }
            if (!s.endsWith('/')) s += '/'
            require(s.toHttpUrlOrNull() != null) { "Некорректный URL сервера" }
            return s
        }
    }
}
