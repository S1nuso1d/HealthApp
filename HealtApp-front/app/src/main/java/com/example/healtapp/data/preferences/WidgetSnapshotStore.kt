package com.example.healtapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.widgetSnapshotStore by preferencesDataStore(name = "widget_snapshot")

data class WidgetSnapshot(
    val stepsToday: Int = 0,
    val stepsGoal: Int = 10_000,
    val waterMl: Int = 0,
    val waterGoalMl: Int = 2500,
    val healthScore: Int = 0,
    val sleepHours: Float = 0f,
    val sleepTargetHours: Float = 8f,
    val caloriesToday: Int = 0,
    val caloriesTarget: Int = 2200,
    val briefTitle: String = "",
)

class WidgetSnapshotStore(private val context: Context) {

    private val stepsKey = intPreferencesKey("steps")
    private val stepsGoalKey = intPreferencesKey("steps_goal")
    private val waterKey = intPreferencesKey("water")
    private val waterGoalKey = intPreferencesKey("water_goal")
    private val healthScoreKey = intPreferencesKey("health_score")
    private val sleepKey = floatPreferencesKey("sleep_hours")
    private val sleepGoalKey = floatPreferencesKey("sleep_goal_hours")
    private val caloriesKey = intPreferencesKey("calories")
    private val caloriesGoalKey = intPreferencesKey("calories_goal")
    private val briefKey = stringPreferencesKey("brief_title")

    suspend fun save(snapshot: WidgetSnapshot) {
        context.widgetSnapshotStore.edit { prefs ->
            prefs[stepsKey] = snapshot.stepsToday
            prefs[stepsGoalKey] = snapshot.stepsGoal
            prefs[waterKey] = snapshot.waterMl
            prefs[waterGoalKey] = snapshot.waterGoalMl
            prefs[healthScoreKey] = snapshot.healthScore
            prefs[sleepKey] = snapshot.sleepHours
            prefs[sleepGoalKey] = snapshot.sleepTargetHours
            prefs[caloriesKey] = snapshot.caloriesToday
            prefs[caloriesGoalKey] = snapshot.caloriesTarget
            prefs[briefKey] = snapshot.briefTitle
        }
    }

    suspend fun load(): WidgetSnapshot? {
        val prefs = context.widgetSnapshotStore.data.first()
        if (!prefs.contains(stepsKey) && !prefs.contains(waterKey)) return null
        return WidgetSnapshot(
            stepsToday = prefs[stepsKey] ?: 0,
            stepsGoal = prefs[stepsGoalKey] ?: 10_000,
            waterMl = prefs[waterKey] ?: 0,
            waterGoalMl = prefs[waterGoalKey] ?: 2500,
            healthScore = prefs[healthScoreKey] ?: 0,
            sleepHours = prefs[sleepKey] ?: 0f,
            sleepTargetHours = prefs[sleepGoalKey] ?: 8f,
            caloriesToday = prefs[caloriesKey] ?: 0,
            caloriesTarget = prefs[caloriesGoalKey] ?: 2200,
            briefTitle = prefs[briefKey].orEmpty(),
        )
    }
}
