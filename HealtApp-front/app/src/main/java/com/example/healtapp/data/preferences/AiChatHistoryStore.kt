package com.example.healtapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.first

private val Context.aiChatHistoryStore by preferencesDataStore(name = "ai_chat_history")

data class StoredChatMessage(
    val id: Long,
    val isUser: Boolean,
    val text: String,
)

class AiChatHistoryStore(
    private val context: Context,
    private val gson: Gson = Gson(),
) {
    private val key = stringPreferencesKey("messages_json")

    suspend fun save(messages: List<StoredChatMessage>) {
        context.aiChatHistoryStore.edit { prefs ->
            if (messages.isEmpty()) {
                prefs.remove(key)
            } else {
                prefs[key] = gson.toJson(messages)
            }
        }
    }

    suspend fun load(): List<StoredChatMessage>? {
        val json = context.aiChatHistoryStore.data.first()[key] ?: return null
        return runCatching {
            gson.fromJson(json, Array<StoredChatMessage>::class.java)?.toList()
        }.getOrNull()
    }

    suspend fun clear() {
        context.aiChatHistoryStore.edit { it.remove(key) }
    }
}
