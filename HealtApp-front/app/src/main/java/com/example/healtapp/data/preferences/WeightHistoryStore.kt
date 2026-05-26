package com.example.healtapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate

private val Context.weightHistoryStore by preferencesDataStore(name = "weight_history")

data class WeightEntry(
    val date: String,
    val weightKg: Float,
)

class WeightHistoryStore(
    private val context: Context,
    private val gson: Gson = Gson(),
) {
    private val entriesKey = stringPreferencesKey("entries_json")
    private val lastPromptKey = longPreferencesKey("last_weekly_prompt_epoch")

    suspend fun append(weightKg: Float, date: LocalDate = LocalDate.now()) {
        if (weightKg <= 0f) return
        val list = loadEntries().toMutableList()
        val key = date.toString()
        list.removeAll { it.date == key }
        list.add(WeightEntry(key, weightKg))
        val trimmed = list.sortedBy { it.date }.takeLast(56)
        saveEntries(trimmed)
    }

    suspend fun loadEntries(): List<WeightEntry> {
        val json = context.weightHistoryStore.data.first()[entriesKey] ?: return emptyList()
        val type = object : TypeToken<List<WeightEntry>>() {}.type
        return runCatching { gson.fromJson<List<WeightEntry>>(json, type) }.getOrDefault(emptyList())
            .sortedBy { it.date }
    }

    suspend fun shouldShowWeeklyReminder(): Boolean {
        val last = context.weightHistoryStore.data.first()[lastPromptKey] ?: 0L
        val weekAgo = Instant.now().minusSeconds(7 * 24 * 3600).toEpochMilli()
        return last < weekAgo
    }

    suspend fun markWeeklyPromptShown() {
        context.weightHistoryStore.edit { it[lastPromptKey] = Instant.now().toEpochMilli() }
    }

    private suspend fun saveEntries(entries: List<WeightEntry>) {
        context.weightHistoryStore.edit { prefs ->
            prefs[entriesKey] = gson.toJson(entries)
        }
    }
}
