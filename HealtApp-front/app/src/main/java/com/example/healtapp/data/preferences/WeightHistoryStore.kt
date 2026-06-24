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

    private val tokenStorage: TokenStorage,

    private val gson: Gson = Gson(),

) {

    private fun entriesKey(userId: Int) = stringPreferencesKey("entries_json_$userId")

    private fun lastPromptKey(userId: Int) = longPreferencesKey("last_weekly_prompt_$userId")



    private suspend fun currentUserId(): Int? = tokenStorage.getUserId()



    suspend fun append(weightKg: Float, date: LocalDate = LocalDate.now()) {

        if (weightKg <= 0f) return

        val userId = currentUserId() ?: return

        val list = loadEntries().toMutableList()

        val key = date.toString()

        list.removeAll { it.date == key }

        list.add(WeightEntry(key, weightKg))

        val trimmed = list.sortedBy { it.date }.takeLast(56)

        saveEntries(userId, trimmed)

    }



    suspend fun loadEntries(): List<WeightEntry> {

        val userId = currentUserId() ?: return emptyList()

        val json = context.weightHistoryStore.data.first()[entriesKey(userId)] ?: return emptyList()

        val type = object : TypeToken<List<WeightEntry>>() {}.type

        return runCatching { gson.fromJson<List<WeightEntry>>(json, type) }.getOrDefault(emptyList())

            .sortedBy { it.date }

    }



    suspend fun shouldShowWeeklyReminder(): Boolean {

        val userId = currentUserId() ?: return false

        val last = context.weightHistoryStore.data.first()[lastPromptKey(userId)] ?: 0L

        val weekAgo = Instant.now().minusSeconds(7 * 24 * 3600).toEpochMilli()

        return last < weekAgo

    }



    suspend fun markWeeklyPromptShown() {

        val userId = currentUserId() ?: return

        context.weightHistoryStore.edit { it[lastPromptKey(userId)] = Instant.now().toEpochMilli() }

    }



    private suspend fun saveEntries(userId: Int, entries: List<WeightEntry>) {

        context.weightHistoryStore.edit { prefs ->

            prefs[entriesKey(userId)] = gson.toJson(entries)

        }

    }



    /** Очищает историю только текущего пользователя (при удалении аккаунта и т.п.). */

    suspend fun clearCurrentUser() {

        val userId = currentUserId() ?: return

        context.weightHistoryStore.edit { prefs ->

            prefs.remove(entriesKey(userId))

            prefs.remove(lastPromptKey(userId))

        }

    }

}


