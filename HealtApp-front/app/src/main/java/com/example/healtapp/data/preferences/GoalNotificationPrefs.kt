package com.example.healtapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.time.LocalDate

private val Context.goalNotifyDataStore by preferencesDataStore(name = "goal_notify_prefs")

/** Чтобы не слать одно и то же поздравление несколько раз за день. */
class GoalNotificationPrefs(private val context: Context) {

    companion object {
        private val STEPS_GOAL_DAY = stringPreferencesKey("steps_goal_day")
        private val WATER_GOAL_DAY = stringPreferencesKey("water_goal_day")
    }

    suspend fun wasStepsGoalNotifiedToday(): Boolean =
        context.goalNotifyDataStore.data.first()[STEPS_GOAL_DAY] == LocalDate.now().toString()

    suspend fun markStepsGoalNotifiedToday() {
        context.goalNotifyDataStore.edit {
            it[STEPS_GOAL_DAY] = LocalDate.now().toString()
        }
    }

    suspend fun wasWaterGoalNotifiedToday(): Boolean =
        context.goalNotifyDataStore.data.first()[WATER_GOAL_DAY] == LocalDate.now().toString()

    suspend fun markWaterGoalNotifiedToday() {
        context.goalNotifyDataStore.edit {
            it[WATER_GOAL_DAY] = LocalDate.now().toString()
        }
    }
}
