package com.example.healtapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.healtapp.data.network.dto.wellness.DashboardHomeDto
import com.google.gson.Gson
import kotlinx.coroutines.flow.first

private val Context.dashboardCacheStore by preferencesDataStore(name = "dashboard_cache")

class DashboardCache(
    private val context: Context,
    private val gson: Gson = Gson(),
) {
    private val key = stringPreferencesKey("dashboard_home_json")

    suspend fun save(home: DashboardHomeDto) {
        context.dashboardCacheStore.edit { prefs ->
            prefs[key] = gson.toJson(home)
        }
    }

    suspend fun load(): DashboardHomeDto? {
        val json = context.dashboardCacheStore.data.first()[key] ?: return null
        return runCatching { gson.fromJson(json, DashboardHomeDto::class.java) }.getOrNull()
    }

    suspend fun clear() {
        context.dashboardCacheStore.edit { it.remove(key) }
    }
}
