package com.example.healtapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.healtapp.core.common.JwtUserId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "auth_prefs")

class TokenStorage(
    private val context: Context
) {
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val GUEST_MODE_KEY = booleanPreferencesKey("guest_mode")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String?) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = accessToken
            if (refreshToken != null) {
                prefs[REFRESH_TOKEN_KEY] = refreshToken
            } else {
                prefs.remove(REFRESH_TOKEN_KEY)
            }
            prefs[GUEST_MODE_KEY] = false
            JwtUserId.fromAccessToken(accessToken)?.let { id ->
                prefs[USER_ID_KEY] = id.toString()
            }
        }
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            prefs[GUEST_MODE_KEY] = false
            JwtUserId.fromAccessToken(token)?.let { id ->
                prefs[USER_ID_KEY] = id.toString()
            }
        }
    }

    /** Локальный просмотр без JWT: главная и рекомендации с демо-данными, без синхронизации. */
    suspend fun setGuestMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            if (enabled) {
                prefs.remove(TOKEN_KEY)
                prefs.remove(REFRESH_TOKEN_KEY)
                prefs[GUEST_MODE_KEY] = true
            } else {
                prefs[GUEST_MODE_KEY] = false
            }
        }
    }

    suspend fun isGuestMode(): Boolean =
        context.dataStore.data.first()[GUEST_MODE_KEY] == true

    fun tokenFlow(): Flow<String?> {
        return context.dataStore.data.map { prefs -> prefs[TOKEN_KEY] }
    }

    fun refreshTokenFlow(): Flow<String?> {
        return context.dataStore.data.map { prefs -> prefs[REFRESH_TOKEN_KEY] }
    }

    suspend fun getToken(): String? {
        return tokenFlow().first()
    }

    suspend fun getRefreshToken(): String? {
        return refreshTokenFlow().first()
    }

    suspend fun getUserId(): Int? =
        context.dataStore.data.first()[USER_ID_KEY]?.toIntOrNull()
            ?: JwtUserId.fromAccessToken(getToken())

    suspend fun setUserId(userId: Int) {
        context.dataStore.edit { prefs ->
            prefs[USER_ID_KEY] = userId.toString()
        }
    }

    suspend fun clearToken() {
        context.dataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
            prefs.remove(REFRESH_TOKEN_KEY)
            prefs.remove(USER_ID_KEY)
            prefs[GUEST_MODE_KEY] = false
        }
    }
}