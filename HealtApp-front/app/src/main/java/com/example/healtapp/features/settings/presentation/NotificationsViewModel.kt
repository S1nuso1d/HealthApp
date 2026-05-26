package com.example.healtapp.features.settings.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.data.preferences.NotificationPrefs
import com.example.healtapp.data.preferences.NotificationSettings
import com.example.healtapp.notifications.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val settings: NotificationSettings = NotificationSettings(),
    val message: String? = null,
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationPrefs: NotificationPrefs,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            notificationPrefs.settingsFlow.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun setHydrationReminders(enabled: Boolean) {
        viewModelScope.launch {
            notificationPrefs.setHydrationReminders(enabled)
            ReminderScheduler.rescheduleHydration(context, enabled)
            _uiState.update {
                it.copy(message = if (enabled) "Напоминания о воде включены" else "Напоминания о воде выключены")
            }
        }
    }

    fun setMealReminders(enabled: Boolean) {
        viewModelScope.launch {
            notificationPrefs.setMealReminders(enabled)
            ReminderScheduler.rescheduleMeals(context, enabled)
            _uiState.update {
                it.copy(message = if (enabled) "Напоминания о приёмах пищи включены" else "Напоминания о еде выключены")
            }
        }
    }

    fun setMissedMealChecks(enabled: Boolean) {
        viewModelScope.launch {
            notificationPrefs.setMissedMealChecks(enabled)
            ReminderScheduler.rescheduleMissedMealChecks(context, enabled)
            _uiState.update {
                it.copy(
                    message = if (enabled) {
                        "Проверка записей еды включена"
                    } else {
                        "Напоминания о пропущенных приёмах пищи выключены"
                    },
                )
            }
        }
    }

    fun setGoalAchievementNotifications(enabled: Boolean) {
        viewModelScope.launch {
            notificationPrefs.setGoalAchievementNotifications(enabled)
            ReminderScheduler.rescheduleGoalChecks(context, enabled)
            _uiState.update {
                it.copy(
                    message = if (enabled) {
                        "Уведомления о целях включены"
                    } else {
                        "Уведомления о достижении целей выключены"
                    },
                )
            }
        }
    }

    fun setRecommendationReminders(enabled: Boolean) {
        viewModelScope.launch {
            notificationPrefs.setRecommendationReminders(enabled)
            ReminderScheduler.rescheduleRecommendations(context, enabled)
            val time = notificationPrefs.current().recommendationTimeLabel()
            _uiState.update {
                it.copy(
                    message = if (enabled) {
                        "Советы придут каждый день в $time"
                    } else {
                        "Напоминания о рекомендациях выключены"
                    },
                )
            }
        }
    }

    fun setRecommendationReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            notificationPrefs.setRecommendationReminderTime(hour, minute)
            val settings = notificationPrefs.current()
            if (settings.recommendationReminders) {
                ReminderScheduler.scheduleRecommendationsAt(
                    context = context,
                    hour = settings.recommendationHour,
                    minute = settings.recommendationMinute,
                )
            }
            _uiState.update {
                it.copy(message = "Время напоминания: ${settings.recommendationTimeLabel()}")
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
