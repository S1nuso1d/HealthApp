package com.example.healtapp.data.network.offline

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class PendingAiQuestion(
    val id: String = UUID.randomUUID().toString(),
    val question: String,
    val userDisplayText: String,
    val timestamp: Long = System.currentTimeMillis(),
)

@Singleton
class PendingAiChatStore @Inject constructor(
    @ApplicationContext context: Context,
    private val gson: Gson,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pending_ai_chat", Context.MODE_PRIVATE)

    fun enqueue(question: String, userDisplayText: String): PendingAiQuestion {
        val item = PendingAiQuestion(question = question, userDisplayText = userDisplayText)
        val next = load().toMutableList()
        next.add(item)
        save(next)
        com.example.healtapp.core.common.AppPendingSyncBus.notifyQueueChanged()
        return item
    }

    fun load(): List<PendingAiQuestion> {
        val json = prefs.getString("items", null) ?: return emptyList()
        val type = object : TypeToken<List<PendingAiQuestion>>() {}.type
        return runCatching { gson.fromJson<List<PendingAiQuestion>>(json, type) }.getOrNull().orEmpty()
    }

    fun remove(id: String) {
        save(load().filterNot { it.id == id })
    }

    private fun save(items: List<PendingAiQuestion>) {
        prefs.edit().putString("items", gson.toJson(items)).apply()
    }
}
