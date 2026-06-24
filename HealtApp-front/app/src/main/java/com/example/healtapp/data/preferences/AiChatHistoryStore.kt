package com.example.healtapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val Context.aiChatHistoryStore by preferencesDataStore(name = "ai_chat_history")

data class StoredChatMessage(
    val id: Long,
    val isUser: Boolean,
    val text: String,
)

data class StoredChatSession(
    val id: String,
    val savedAt: Long,
    val messages: List<StoredChatMessage>,
)

class AiChatHistoryStore(
    private val context: Context,
    private val gson: Gson = Gson(),
) {
    private val activeKey = stringPreferencesKey("messages_json")
    private val archiveKey = stringPreferencesKey("archived_sessions_json")

    suspend fun save(messages: List<StoredChatMessage>) {
        context.aiChatHistoryStore.edit { prefs ->
            if (messages.isEmpty()) {
                prefs.remove(activeKey)
            } else {
                prefs[activeKey] = gson.toJson(messages)
            }
        }
    }

    suspend fun load(): List<StoredChatMessage>? {
        val json = context.aiChatHistoryStore.data.first()[activeKey] ?: return null
        return runCatching {
            gson.fromJson(json, Array<StoredChatMessage>::class.java)?.toList()
        }.getOrNull()
    }

    suspend fun clear() {
        context.aiChatHistoryStore.edit { it.remove(activeKey) }
    }

    suspend fun archiveSession(messages: List<StoredChatMessage>) {
        val meaningful = messages.dropWhile { !it.isUser }
        if (meaningful.none { it.isUser }) return

        val session = StoredChatSession(
            id = UUID.randomUUID().toString(),
            savedAt = System.currentTimeMillis(),
            messages = meaningful,
        )
        val existing = loadArchivedSessions().toMutableList()
        existing.add(0, session)
        val trimmed = existing.take(MAX_ARCHIVED_SESSIONS)
        context.aiChatHistoryStore.edit { prefs ->
            prefs[archiveKey] = gson.toJson(trimmed)
        }
    }

    suspend fun loadArchivedSessions(): List<StoredChatSession> {
        val json = context.aiChatHistoryStore.data.first()[archiveKey] ?: return emptyList()
        return runCatching {
            gson.fromJson(json, Array<StoredChatSession>::class.java)?.toList().orEmpty()
        }.getOrNull().orEmpty()
    }

    companion object {
        private const val MAX_ARCHIVED_SESSIONS = 30

        fun sessionTitle(savedAt: Long, messages: List<StoredChatMessage>): String {
            val date = SimpleDateFormat("d MMM, HH:mm", Locale("ru")).format(Date(savedAt))
            val preview = messages.firstOrNull { it.isUser }?.text?.lineSequence()?.first()?.take(48)
            return if (preview.isNullOrBlank()) "Диалог · $date" else "$preview · $date"
        }
    }
}
