package com.example.healtapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.trainingPrefsStore by preferencesDataStore(name = "training_prefs")

@Singleton
class TrainingPrefs @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val favoritesKey = stringSetPreferencesKey("favorites")
    private val usageKey = stringPreferencesKey("usage_v1")

    val favoritesFlow: Flow<Set<String>> = context.trainingPrefsStore.data.map { prefs ->
        prefs[favoritesKey].orEmpty()
    }

    val usageFlow: Flow<Map<String, Int>> = context.trainingPrefsStore.data.map { prefs ->
        decodeUsage(prefs[usageKey].orEmpty())
    }

    suspend fun favoritesSnapshot(): Set<String> = favoritesFlow.first()

    suspend fun usageSnapshot(): Map<String, Int> = usageFlow.first()

    suspend fun toggleFavorite(slug: String) {
        context.trainingPrefsStore.edit { prefs ->
            val current = prefs[favoritesKey].orEmpty().toMutableSet()
            if (!current.add(slug)) current.remove(slug)
            prefs[favoritesKey] = current
        }
    }

    suspend fun recordUsage(slug: String) {
        context.trainingPrefsStore.edit { prefs ->
            val usage = decodeUsage(prefs[usageKey].orEmpty()).toMutableMap()
            usage[slug] = (usage[slug] ?: 0) + 1
            prefs[usageKey] = encodeUsage(usage)
        }
    }

    suspend fun clear() {
        context.trainingPrefsStore.edit { it.clear() }
    }

    private fun encodeUsage(map: Map<String, Int>): String =
        map.entries.joinToString("|") { "${it.key}:${it.value}" }

    private fun decodeUsage(raw: String): Map<String, Int> {
        if (raw.isBlank()) return emptyMap()
        return raw.split("|").mapNotNull { part ->
            val idx = part.lastIndexOf(':')
            if (idx <= 0) return@mapNotNull null
            val slug = part.substring(0, idx)
            val count = part.substring(idx + 1).toIntOrNull() ?: return@mapNotNull null
            slug to count
        }.toMap()
    }
}
