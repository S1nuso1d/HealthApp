package com.example.healtapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.apiServerDataStore by preferencesDataStore(name = "api_server_prefs")

class ApiServerPreferences(
    private val context: Context,
) {
    companion object {
        private val OVERRIDE_URL = stringPreferencesKey("api_base_url_override")
    }

    val overrideFlow: Flow<String?> =
        context.apiServerDataStore.data.map { prefs ->
            prefs[OVERRIDE_URL]?.takeIf { it.isNotBlank() }
        }

    suspend fun getOverride(): String? =
        overrideFlow.first()

    suspend fun setOverride(url: String?) {
        context.apiServerDataStore.edit { prefs ->
            if (url.isNullOrBlank()) {
                prefs.remove(OVERRIDE_URL)
            } else {
                prefs[OVERRIDE_URL] = url
            }
        }
    }
}
