package com.example.healtapp.data.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExperimentCheckinStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("experiment_checkins", Context.MODE_PRIVATE)

    fun markToday(experimentId: Int, kept: Boolean) {
        prefs.edit().putString(todayKey(experimentId), if (kept) "1" else "0").apply()
    }

    fun todayValue(experimentId: Int): Boolean? {
        val raw = prefs.getString(todayKey(experimentId), null) ?: return null
        return raw == "1"
    }

    fun keptCount(experimentId: Int): Int {
        val prefix = "$experimentId|"
        return prefs.all.count { (key, value) ->
            key.startsWith(prefix) && value == "1"
        }
    }

    private fun todayKey(experimentId: Int): String = "$experimentId|${LocalDate.now()}"
}
