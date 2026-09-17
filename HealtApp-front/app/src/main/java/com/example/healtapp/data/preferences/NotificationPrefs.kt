package com.example.healtapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.notificationDataStore by preferencesDataStore(name = "notification_prefs")

class NotificationPrefs(private val context: Context) {

    companion object {
        private val HYDRATION_ENABLED = booleanPreferencesKey("hydration_reminders")
        private val MEAL_REMINDERS_ENABLED = booleanPreferencesKey("meal_reminders")
        private val RECOMMENDATIONS_ENABLED = booleanPreferencesKey("recommendation_reminders")
        private val MISSED_MEAL_CHECKS = booleanPreferencesKey("missed_meal_checks")
        private val GOAL_ACHIEVEMENTS = booleanPreferencesKey("goal_achievement_notifications")
        private val RECOMMENDATION_HOUR = intPreferencesKey("recommendation_hour")
        private val RECOMMENDATION_MINUTE = intPreferencesKey("recommendation_minute")
        private val USUAL_BEDTIME_HOUR = intPreferencesKey("usual_bedtime_hour")
        private val USUAL_BEDTIME_MINUTE = intPreferencesKey("usual_bedtime_minute")

        const val DEFAULT_RECOMMENDATION_HOUR = 10
        const val DEFAULT_RECOMMENDATION_MINUTE = 0
        const val DEFAULT_BEDTIME_NUDGE_HOUR = 21
        const val DEFAULT_BEDTIME_NUDGE_MINUTE = 30
    }

    val settingsFlow: Flow<NotificationSettings> = context.notificationDataStore.data.map { prefs ->
        NotificationSettings(
            hydrationReminders = prefs[HYDRATION_ENABLED] ?: true,
            mealReminders = prefs[MEAL_REMINDERS_ENABLED] ?: true,
            recommendationReminders = prefs[RECOMMENDATIONS_ENABLED] ?: true,
            missedMealChecks = prefs[MISSED_MEAL_CHECKS] ?: true,
            goalAchievementNotifications = prefs[GOAL_ACHIEVEMENTS] ?: true,
            recommendationHour = prefs[RECOMMENDATION_HOUR] ?: DEFAULT_RECOMMENDATION_HOUR,
            recommendationMinute = prefs[RECOMMENDATION_MINUTE] ?: DEFAULT_RECOMMENDATION_MINUTE,
            usualBedtimeHour = prefs[USUAL_BEDTIME_HOUR],
            usualBedtimeMinute = prefs[USUAL_BEDTIME_MINUTE],
        )
    }

    suspend fun current(): NotificationSettings = settingsFlow.first()

    suspend fun setHydrationReminders(enabled: Boolean) {
        context.notificationDataStore.edit { it[HYDRATION_ENABLED] = enabled }
    }

    suspend fun setMealReminders(enabled: Boolean) {
        context.notificationDataStore.edit { it[MEAL_REMINDERS_ENABLED] = enabled }
    }

    suspend fun setRecommendationReminders(enabled: Boolean) {
        context.notificationDataStore.edit { it[RECOMMENDATIONS_ENABLED] = enabled }
    }

    suspend fun setMissedMealChecks(enabled: Boolean) {
        context.notificationDataStore.edit { it[MISSED_MEAL_CHECKS] = enabled }
    }

    suspend fun setGoalAchievementNotifications(enabled: Boolean) {
        context.notificationDataStore.edit { it[GOAL_ACHIEVEMENTS] = enabled }
    }

    suspend fun setUsualBedtime(hour: Int, minute: Int) {
        context.notificationDataStore.edit {
            it[USUAL_BEDTIME_HOUR] = hour.coerceIn(0, 23)
            it[USUAL_BEDTIME_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setRecommendationReminderTime(hour: Int, minute: Int) {
        val h = hour.coerceIn(0, 23)
        val m = minute.coerceIn(0, 59)
        context.notificationDataStore.edit {
            it[RECOMMENDATION_HOUR] = h
            it[RECOMMENDATION_MINUTE] = m
        }
    }
}

data class NotificationSettings(
    val hydrationReminders: Boolean = true,
    val mealReminders: Boolean = true,
    val recommendationReminders: Boolean = true,
    val missedMealChecks: Boolean = true,
    val goalAchievementNotifications: Boolean = true,
    val recommendationHour: Int = NotificationPrefs.DEFAULT_RECOMMENDATION_HOUR,
    val recommendationMinute: Int = NotificationPrefs.DEFAULT_RECOMMENDATION_MINUTE,
    val usualBedtimeHour: Int? = null,
    val usualBedtimeMinute: Int? = null,
) {
    fun recommendationTimeLabel(): String =
        "%02d:%02d".format(recommendationHour, recommendationMinute)
}
