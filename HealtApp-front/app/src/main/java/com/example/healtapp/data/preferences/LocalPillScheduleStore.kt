package com.example.healtapp.data.preferences

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class LocalPillSchedule(
    val id: Int,
    val name: String,
    val dosage: String,
    val timeOfDay: String,
    val active: Boolean = true,
)

/** Локальный кэш расписания таблеток — чтобы будильники восстанавливались без сети. */
object LocalPillScheduleStore {
    private const val PREFS = "local_pill_schedules"
    private const val KEY = "schedules_json"

    fun load(context: Context): List<LocalPillSchedule> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        LocalPillSchedule(
                            id = obj.getInt("id"),
                            name = obj.optString("name"),
                            dosage = obj.optString("dosage"),
                            timeOfDay = obj.optString("timeOfDay"),
                            active = obj.optBoolean("active", true),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun upsert(context: Context, schedule: LocalPillSchedule) {
        val next = load(context).filterNot { it.id == schedule.id } + schedule
        save(context, next)
    }

    fun remove(context: Context, pillId: Int) {
        save(context, load(context).filterNot { it.id == pillId })
    }

    fun replaceAll(context: Context, schedules: List<LocalPillSchedule>) {
        save(context, schedules)
    }

    private fun save(context: Context, schedules: List<LocalPillSchedule>) {
        val array = JSONArray()
        schedules.forEach { schedule ->
            array.put(
                JSONObject()
                    .put("id", schedule.id)
                    .put("name", schedule.name)
                    .put("dosage", schedule.dosage)
                    .put("timeOfDay", schedule.timeOfDay)
                    .put("active", schedule.active),
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, array.toString())
            .apply()
    }
}
