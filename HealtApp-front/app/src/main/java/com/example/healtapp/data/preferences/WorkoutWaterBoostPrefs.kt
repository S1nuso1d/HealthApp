package com.example.healtapp.data.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutWaterBoostPrefs @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("workout_water_boost", Context.MODE_PRIVATE)

    fun extraMlToday(): Int {
        if (prefs.getString(KEY_DATE, null) != LocalDate.now().toString()) return 0
        return prefs.getInt(KEY_ML, 0)
    }

    fun setFromDistanceKm(distanceKm: Float) {
        if (distanceKm < 3f) return
        val extra = if (distanceKm >= 8f) 400 else 250
        prefs.edit()
            .putString(KEY_DATE, LocalDate.now().toString())
            .putInt(KEY_ML, extra)
            .apply()
    }

    companion object {
        private const val KEY_DATE = "date"
        private const val KEY_ML = "ml"
    }
}
