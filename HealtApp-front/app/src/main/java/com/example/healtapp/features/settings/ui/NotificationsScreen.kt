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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.FeatureCardDivider
import com.example.healtapp.core.ui.components.FeatureGlassCard
import com.example.healtapp.core.ui.components.FeatureHeroChip
import com.example.healtapp.core.ui.components.FeatureInlineNotice
import com.example.healtapp.core.ui.components.FeatureScreenShell
import com.example.healtapp.core.ui.components.FeatureSectionTitle
import com.example.healtapp.core.ui.theme.contentSecondaryColor
import com.example.healtapp.features.settings.presentation.NotificationsViewModel
import com.example.healtapp.features.settings.ui.components.RecommendationReminderTimeCard
import com.example.healtapp.features.settings.ui.components.SettingsCapabilityPanel
import com.example.healtapp.features.settings.ui.components.SettingsToggleRow
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

    val enabledCount = listOf(
        uiState.settings.hydrationReminders,
        uiState.settings.mealReminders,
        uiState.settings.missedMealChecks,
        uiState.settings.goalAchievementNotifications,
        uiState.settings.recommendationReminders,
    ).count { it }

    FeatureScreenShell(
        title = "Уведомления",
        subtitle = "Вода, еда, цели и советы",
        icon = Icons.Filled.Notifications,
        onBack = onBack,
        heroFooter = {
            Spacer(Modifier.height(10.dp))
            FeatureHeroChip(
                label = if (enabledCount > 0) "Активных: $enabledCount" else "Все выключены",
                modifier = Modifier.padding(start = 12.dp),
            )
        },
    ) {
        AnimatedVisibility(
            visible = uiState.message != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            uiState.message?.let { msg ->
                FeatureInlineNotice(text = msg)
            }
        }

        SettingsCapabilityPanel(
            title = "Напоминания на телефоне",
            subtitle = "Локальные push-уведомления без интернета — как напоминания о таблетках.",
            items = listOf(
                Triple(Icons.Outlined.LocalDrink, "Вода", "Каждые 3 часа"),
                Triple(Icons.Filled.Restaurant, "Еда", "Завтрак, обед, ужин"),
                Triple(Icons.Filled.NotificationsActive, "Советы", "По расписанию"),
            ),
        )

        FeatureSectionTitle(
            title = "Напоминания",
            subtitle = "Включите нужные категории",
        )

        FeatureGlassCard {
            SettingsToggleRow(
                icon = Icons.Outlined.LocalDrink,
                title = "Вода",
                subtitle = "Каждые 3 часа с 8:00 до 22:00",
                checked = uiState.settings.hydrationReminders,
                onCheckedChange = { enabled ->
                    requestAndRun { viewModel.setHydrationReminders(enabled) }
                },
            )
            FeatureCardDivider()
            SettingsToggleRow(
                icon = Icons.Filled.Restaurant,
                title = "Приёмы пищи",
                subtitle = "Завтрак 8:00 · обед 13:00 · ужин 19:00",
                checked = uiState.settings.mealReminders,
                onCheckedChange = { enabled ->
                    requestAndRun { viewModel.setMealReminders(enabled) }
                },
            )
            FeatureCardDivider()
            SettingsToggleRow(
                icon = Icons.Outlined.EventBusy,
                title = "Пропущенные приёмы пищи",
                subtitle = "Если завтрак, обед или ужин не записан к 10:30 / 15:00 / 21:00",
                checked = uiState.settings.missedMealChecks,
                onCheckedChange = { enabled ->
                    requestAndRun { viewModel.setMissedMealChecks(enabled) }
                },
            )
            FeatureCardDivider()
            SettingsToggleRow(
                icon = Icons.Outlined.CheckCircle,
                title = "Достижение целей",
                subtitle = "Шаги и вода — когда дневная цель выполнена",
                checked = uiState.settings.goalAchievementNotifications,
                onCheckedChange = { enabled ->
                    requestAndRun { viewModel.setGoalAchievementNotifications(enabled) }
                },
            )
            FeatureCardDivider()
            SettingsToggleRow(
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

        Text(
            text = "На Android 13+ нужно разрешение «Уведомления». Напоминания не отправляют данные на сервер.",
            style = MaterialTheme.typography.bodySmall,
            color = contentSecondaryColor(),
        )
    }
}
