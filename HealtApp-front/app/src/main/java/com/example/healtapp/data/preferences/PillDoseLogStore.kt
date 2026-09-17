package com.example.healtapp.data.preferences

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate

enum class PillDoseStatus { Taken, Skipped }

data class PillDoseLog(
    val pillId: Int,
    val dateKey: String,
    val status: PillDoseStatus,
    val loggedAtEpochMs: Long = System.currentTimeMillis(),
)

/**
 * Локальный журнал «принял / пропустил» по таблеткам.
 * Пока без бэка — adherence считается на устройстве за 7 дней.
 */
class PillDoseLogStore(
    context: Context,
    private val gson: Gson = Gson(),
) {
    private val prefs = context.getSharedPreferences("pill_dose_logs", Context.MODE_PRIVATE)
    private val key = "logs_json"
    private val listType = object : TypeToken<List<PillDoseLog>>() {}.type

    fun all(): List<PillDoseLog> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching { gson.fromJson<List<PillDoseLog>>(raw, listType) }.getOrDefault(emptyList())
    }

    fun logToday(pillId: Int, status: PillDoseStatus) {
        val today = LocalDate.now().toString()
        val next = all()
            .filterNot { it.pillId == pillId && it.dateKey == today }
            .plus(PillDoseLog(pillId = pillId, dateKey = today, status = status))
            .takeLast(400)
        prefs.edit().putString(key, gson.toJson(next)).apply()
    }

    fun todayStatus(pillId: Int): PillDoseStatus? {
        val today = LocalDate.now().toString()
        return all().lastOrNull { it.pillId == pillId && it.dateKey == today }?.status
    }

    /** Доля принятых среди зафиксированных доз за последние [days] дней (0–100). */
    fun adherencePercent(days: Int = 7): Int? {
        val from = LocalDate.now().minusDays((days - 1).toLong())
        val window = all().filter {
            runCatching { LocalDate.parse(it.dateKey) }.getOrNull()?.let { d -> !d.isBefore(from) } == true
        }
        if (window.isEmpty()) return null
        val taken = window.count { it.status == PillDoseStatus.Taken }
        return ((taken * 100f) / window.size).toInt().coerceIn(0, 100)
    }
}
