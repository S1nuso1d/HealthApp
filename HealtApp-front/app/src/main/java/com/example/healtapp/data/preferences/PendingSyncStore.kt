package com.example.healtapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.healtapp.core.common.AppPendingSyncBus
import com.google.gson.Gson
import kotlinx.coroutines.flow.first

private val Context.pendingSyncStore by preferencesDataStore(name = "pending_sync")

data class PendingHydrationOp(val amountMl: Int)

data class PendingMealOp(
    val mealType: String,
    val name: String,
    val calories: Float?,
    val proteinG: Float?,
    val fatG: Float?,
    val carbsG: Float?,
)

data class PendingSyncQueue(
    val hydration: List<PendingHydrationOp> = emptyList(),
    val meals: List<PendingMealOp> = emptyList(),
)

class PendingSyncStore(
    private val context: Context,
    private val gson: Gson = Gson(),
) {
    private val key = stringPreferencesKey("queue_json")

    suspend fun load(): PendingSyncQueue {
        val json = context.pendingSyncStore.data.first()[key] ?: return PendingSyncQueue()
        return runCatching { gson.fromJson(json, PendingSyncQueue::class.java) }.getOrDefault(PendingSyncQueue())
    }

    suspend fun save(queue: PendingSyncQueue) {
        context.pendingSyncStore.edit { prefs ->
            if (queue.hydration.isEmpty() && queue.meals.isEmpty()) {
                prefs.remove(key)
            } else {
                prefs[key] = gson.toJson(queue)
            }
        }
        AppPendingSyncBus.notifyQueueChanged()
    }

    suspend fun enqueueHydration(amountMl: Int) {
        val q = load()
        save(q.copy(hydration = q.hydration + PendingHydrationOp(amountMl)))
    }

    suspend fun enqueueMeal(op: PendingMealOp) {
        val q = load()
        save(q.copy(meals = q.meals + op))
    }

    suspend fun pendingCount(): Int {
        val q = load()
        return q.hydration.size + q.meals.size
    }
}
