package com.example.healtapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.healtapp.core.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

class ThemePreferences(
    private val context: Context,
) {
    private val themeKey = stringPreferencesKey("theme_mode")

    fun themeModeFlow(): Flow<ThemeMode> =
        context.themeDataStore.data.map { prefs ->
            ThemeMode.fromStorageKey(prefs[themeKey])
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[themeKey] = mode.storageKey
        }
    }
}
