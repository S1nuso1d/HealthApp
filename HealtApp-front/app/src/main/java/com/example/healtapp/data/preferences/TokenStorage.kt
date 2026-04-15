package com.example.healtapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "auth_prefs")

class TokenStorage(
    private val context: Context
) {
    private val tokenKey = stringPreferencesKey("auth_token")

    suspend fun saveToken(token: String) {
        context.dataStore.edit { prefs: MutablePreferences ->
            prefs[tokenKey] = token
        }
    }

    suspend fun getToken(): String? {
        val prefs: Preferences = context.dataStore.data.first()
        return prefs[tokenKey]
    }

    suspend fun clearToken() {
        context.dataStore.edit { prefs: MutablePreferences ->
            prefs.remove(tokenKey)
        }
    }
}

typealias MutablePreferences = androidx.datastore.preferences.core.MutablePreferences