package com.example.healtapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.fastingStore by preferencesDataStore(name = "fasting")

enum class FastingPlan(val hours: Int, val label: String) {
    PLAN_16_8(16, "16/8"),
    PLAN_18_6(18, "18/6"),
    PLAN_20_4(20, "20/4"),
}

data class FastingState(
    val planHours: Int = FastingPlan.PLAN_16_8.hours,
    val fastStartEpochMs: Long? = null,
    val isActive: Boolean = false,
) {
    val plan: FastingPlan
        get() = FastingPlan.entries.firstOrNull { it.hours == planHours } ?: FastingPlan.PLAN_16_8

    fun elapsedMs(nowMs: Long = System.currentTimeMillis()): Long {
        val start = fastStartEpochMs ?: return 0L
        return (nowMs - start).coerceAtLeast(0L)
    }

    fun targetMs(): Long = planHours * 60L * 60L * 1000L

    fun progress(nowMs: Long = System.currentTimeMillis()): Float {
        val target = targetMs().toFloat()
        if (target <= 0f) return 0f
        return (elapsedMs(nowMs) / target).coerceIn(0f, 1f)
    }

    fun remainingMs(nowMs: Long = System.currentTimeMillis()): Long {
        return (targetMs() - elapsedMs(nowMs)).coerceAtLeast(0L)
    }

    fun phaseLabel(nowMs: Long = System.currentTimeMillis()): String {
        if (!isActive) return "Готовы начать"
        val elapsedHours = elapsedMs(nowMs) / (60f * 60f * 1000f)
        return when {
            elapsedHours < 4f -> "Раннее голодание"
            elapsedHours < 12f -> "Сжигание гликогена"
            elapsedHours < planHours * 0.85f -> "Активное голодание"
            else -> "Окно приёма пищи скоро"
        }
    }
}

class FastingPrefs(private val context: Context) {
    private val planKey = intPreferencesKey("plan_hours")
    private val startKey = longPreferencesKey("fast_start")
    private val activeKey = intPreferencesKey("is_active")

    val state: Flow<FastingState> = context.fastingStore.data.map { prefs ->
        FastingState(
            planHours = prefs[planKey] ?: FastingPlan.PLAN_16_8.hours,
            fastStartEpochMs = prefs[startKey],
            isActive = (prefs[activeKey] ?: 0) == 1,
        )
    }

    suspend fun setPlan(plan: FastingPlan) {
        context.fastingStore.edit { prefs ->
            prefs[planKey] = plan.hours
        }
    }

    suspend fun startFast(nowMs: Long = System.currentTimeMillis()) {
        context.fastingStore.edit { prefs ->
            prefs[startKey] = nowMs
            prefs[activeKey] = 1
        }
    }

    suspend fun stopFast() {
        context.fastingStore.edit { prefs ->
            prefs.remove(startKey)
            prefs[activeKey] = 0
        }
    }
}
