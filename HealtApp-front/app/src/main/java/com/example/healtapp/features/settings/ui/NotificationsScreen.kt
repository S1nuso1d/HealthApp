package com.example.healtapp.features.settings.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.features.settings.presentation.NotificationsViewModel
import com.example.healtapp.features.settings.ui.components.RecommendationReminderTimeCard
import com.example.healtapp.notifications.HealthNotificationHelper
import kotlinx.coroutines.delay

@Composable
fun NotificationsScreen(
    onBack: () -> Unit = {},
) {
    val viewModel: NotificationsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingEnable: (() -> Unit)? by remember { mutableStateOf(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            pendingEnable?.invoke()
        }
        pendingEnable = null
    }

    val requestAndRun: (() -> Unit) -> Unit = { action ->
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            HealthNotificationHelper.canPost(context)
        ) {
            action()
        } else {
            pendingEnable = action
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(uiState.message) {
        if (uiState.message != null) {
            delay(2800)
            viewModel.clearMessage()
        }
    }

    AppScreen(
        title = "Уведомления",
        subtitle = "Вода, еда, цели и советы",
        headerIcon = Icons.Filled.Notifications,
        onNavigateBack = onBack,
        scrollable = true,
    ) {
        AnimatedVisibility(
            visible = uiState.message != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            uiState.message?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }

        SectionHeader(
            title = "Напоминания",
            subtitle = "Локальные уведомления без интернета",
        )

        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ReminderToggleRow(
                    icon = Icons.Outlined.LocalDrink,
                    title = "Вода",
                    subtitle = "Каждые 3 часа с 8:00 до 22:00",
                    checked = uiState.settings.hydrationReminders,
                    onCheckedChange = { enabled ->
                        requestAndRun { viewModel.setHydrationReminders(enabled) }
                    },
                )
                ReminderToggleRow(
                    icon = Icons.Filled.Restaurant,
                    title = "Приёмы пищи",
                    subtitle = "Завтрак 8:00 · обед 13:00 · ужин 19:00",
                    checked = uiState.settings.mealReminders,
                    onCheckedChange = { enabled ->
                        requestAndRun { viewModel.setMealReminders(enabled) }
                    },
                )
                ReminderToggleRow(
                    icon = Icons.Outlined.EventBusy,
                    title = "Пропущенные приёмы пищи",
                    subtitle = "Если завтрак, обед или ужин не записан к 10:30 / 15:00 / 21:00",
                    checked = uiState.settings.missedMealChecks,
                    onCheckedChange = { enabled ->
                        requestAndRun { viewModel.setMissedMealChecks(enabled) }
                    },
                )
                ReminderToggleRow(
                    icon = Icons.Outlined.CheckCircle,
                    title = "Достижение целей",
                    subtitle = "Шаги и вода — когда дневная цель выполнена",
                    checked = uiState.settings.goalAchievementNotifications,
                    onCheckedChange = { enabled ->
                        requestAndRun { viewModel.setGoalAchievementNotifications(enabled) }
                    },
                )
                ReminderToggleRow(
                    icon = Icons.Outlined.TipsAndUpdates,
                    title = "Рекомендации",
                    subtitle = if (uiState.settings.recommendationReminders) {
                        "Каждый день в ${uiState.settings.recommendationTimeLabel()}"
                    } else {
                        "Персональные советы по вашим данным"
                    },
                    checked = uiState.settings.recommendationReminders,
                    onCheckedChange = { enabled ->
                        requestAndRun { viewModel.setRecommendationReminders(enabled) }
                    },
                )

                AnimatedVisibility(
                    visible = uiState.settings.recommendationReminders,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    RecommendationReminderTimeCard(
                        hour = uiState.settings.recommendationHour,
                        minute = uiState.settings.recommendationMinute,
                        onTimeSelected = { h, m ->
                            requestAndRun { viewModel.setRecommendationReminderTime(h, m) }
                        },
                    )
                }
            }
        }

        Text(
            text = "На Android 13+ нужно разрешение «Уведомления». Напоминания не отправляют данные на сервер.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun ReminderToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}
