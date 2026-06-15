package com.example.healtapp.data.network.offline

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class OfflineAction(
    val id: String = UUID.randomUUID().toString(),
    val path: String,
    val method: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Singleton
class OfflineActionQueue @Inject constructor(
    @ApplicationContext context: Context,
    private val gson: Gson
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("offline_actions", Context.MODE_PRIVATE)

    fun enqueue(path: String, method: String, body: String) {
        val actions = getActions().toMutableList()
        actions.add(OfflineAction(path = path, method = method, body = body))
        saveActions(actions)
    }

    fun getActions(): List<OfflineAction> {
        val json = prefs.getString("actions", null) ?: return emptyList()
        val type = object : TypeToken<List<OfflineAction>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun removeAction(id: String) {
        val actions = getActions().filter { it.id != id }
        saveActions(actions)
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun saveActions(actions: List<OfflineAction>) {
        prefs.edit().putString("actions", gson.toJson(actions)).apply()
    }
}