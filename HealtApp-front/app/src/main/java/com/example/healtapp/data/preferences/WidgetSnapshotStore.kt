package com.example.healtapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.widgetSnapshotStore by preferencesDataStore(name = "widget_snapshot")

data class WidgetSnapshot(
    val stepsToday: Int = 0,
    val stepsGoal: Int = 10_000,
    val waterMl: Int = 0,
    val waterGoalMl: Int = 2500,
)

class WidgetSnapshotStore(private val context: Context) {

    private val stepsKey = intPreferencesKey("steps")
    private val stepsGoalKey = intPreferencesKey("steps_goal")
    private val waterKey = intPreferencesKey("water")
    private val waterGoalKey = intPreferencesKey("water_goal")

    suspend fun save(snapshot: WidgetSnapshot) {
        context.widgetSnapshotStore.edit { prefs ->
            prefs[stepsKey] = snapshot.stepsToday
            prefs[stepsGoalKey] = snapshot.stepsGoal
            prefs[waterKey] = snapshot.waterMl
            prefs[waterGoalKey] = snapshot.waterGoalMl
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
        )
    }
}
