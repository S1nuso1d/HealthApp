package com.example.healtapp.features.social.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppMessageBanner
import com.example.healtapp.core.ui.components.AppMessageType
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.features.social.presentation.FriendProfileViewModel

@Composable
fun FriendProfileScreen(onBack: () -> Unit = {}) {
    val viewModel: FriendProfileViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AppScreen(
        title = uiState.user?.display_name ?: "Профиль",
        subtitle = if (uiState.isFriend) "Друг · активность и награды" else "Пользователь",
        headerIcon = Icons.Filled.Person,
        onNavigateBack = onBack,
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator()
            return@AppScreen
        }
        uiState.error?.let {
            AppMessageBanner(text = it, type = AppMessageType.Error)
            return@AppScreen
        }
        SectionHeader(title = "Тренировки", subtitle = "${uiState.activities.size} записей")
        if (uiState.activities.isEmpty()) {
            Text("Нет доступных записей", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            uiState.activities.forEach { a ->
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            a.activity_type ?: "Активность",
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "${a.duration_minutes ?: 0} мин · ${a.calories_burned?.toInt() ?: 0} ккал · шаги ${a.steps ?: 0}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
        SectionHeader(title = "Достижения", subtitle = "${uiState.achievements.size}")
        uiState.achievements.forEach { ach ->
            AppCard {
                Text(ach.title, fontWeight = FontWeight.Medium)
                Text("+${ach.points} очков", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
