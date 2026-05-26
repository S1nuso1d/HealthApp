package com.example.healtapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.healtapp.data.network.dto.profile.ProfileDto
import com.google.gson.Gson
import kotlinx.coroutines.flow.first

private val Context.profileCacheStore by preferencesDataStore(name = "profile_cache")

class ProfileCache(
    private val context: Context,
    private val gson: Gson = Gson(),
) {
    private val key = stringPreferencesKey("profile_json")

    suspend fun save(profile: ProfileDto) {
        context.profileCacheStore.edit { prefs ->
            prefs[key] = gson.toJson(profile)
        }
    }

    suspend fun load(): ProfileDto? {
        val json = context.profileCacheStore.data.first()[key] ?: return null
        return runCatching { gson.fromJson(json, ProfileDto::class.java) }.getOrNull()
    }

    suspend fun clear() {
        context.profileCacheStore.edit { it.remove(key) }
    }

    fun needsOnboarding(profile: ProfileDto?): Boolean =
        profile == null ||
            profile.age == null ||
            profile.height_cm == null ||
            profile.weight_kg == null
}
