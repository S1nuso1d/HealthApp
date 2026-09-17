package com.example.healtapp.data.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecoveryModePrefs @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("recovery_mode", Context.MODE_PRIVATE)

    fun isActiveToday(): Boolean {
        val stored = prefs.getString(KEY_DATE, null) ?: return false
        return stored == LocalDate.now().toString() && prefs.getBoolean(KEY_ON, false)
    }

    fun setActiveToday(on: Boolean) {
        prefs.edit()
            .putBoolean(KEY_ON, on)
            .putString(KEY_DATE, LocalDate.now().toString())
            .apply()
    }

    fun waterTarget(base: Int): Int =
        if (isActiveToday() && base > 0) (base * 0.75f).toInt().coerceAtLeast(800) else base

    fun stepsGoal(base: Int): Int =
        if (isActiveToday() && base > 0) (base * 0.6f).toInt().coerceAtLeast(3000) else base

    companion object {
        private const val KEY_ON = "on"
        private const val KEY_DATE = "date"
    }
}
