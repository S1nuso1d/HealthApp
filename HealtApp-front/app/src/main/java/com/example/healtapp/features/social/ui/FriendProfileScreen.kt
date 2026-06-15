package com.example.healtapp.features.social.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppMessageBanner
import com.example.healtapp.core.ui.components.AppMessageType
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.components.EmptyStateCard
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
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!uiState.isFriend && uiState.user != null) {
                        AppButton(
                            text = "Добавить в друзья",
                            onClick = viewModel::requestFriend,
                        )
                    }
                    AppButton(
                        text = "Заблокировать",
                        onClick = viewModel::blockUser,
                    )
                }
            }
            return@AppScreen
        }
        uiState.actionMessage?.let {
            AppMessageBanner(text = it, type = AppMessageType.Success)
        }
        if (uiState.user != null) {
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (uiState.isBlocked) {
                        Text(
                            "Вы заблокировали этого пользователя.", 
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        AppButton(
                            text = "Разблокировать",
                            onClick = viewModel::unblockUser,
                        )
                    } else {
                        if (!uiState.isFriend) {
                            AppButton(
                                text = "Отправить заявку в друзья",
                                onClick = viewModel::requestFriend,
                            )
                        }
                        AppButton(
                            text = "Заблокировать",
                            onClick = viewModel::blockUser,
                        )
                    }
                }
            }
            if (uiState.isBlocked) {
                return@AppScreen
            }
        }
        SectionHeader(title = "Тренировки", subtitle = "${uiState.activities.size} записей")
        if (uiState.activities.isEmpty()) {
            EmptyStateCard("Нет доступных записей активности.")
        } else {
            uiState.activities.forEach { a ->
                AppCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                .background(androidx.compose.ui.graphics.Brush.linearGradient(com.example.healtapp.core.ui.theme.brandingGradient())),
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.material3.Icon(
                                androidx.compose.material.icons.Icons.Filled.DirectionsRun,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                com.example.healtapp.features.activity.presentation.activityTitleFromApi(a.activity_type.orEmpty()),
                                fontWeight = FontWeight.SemiBold,
                            )
                            val parts = mutableListOf<String>()
                            a.duration_minutes?.let { parts.add("$it мин") }
                            a.steps?.takeIf { it > 0 }?.let { steps -> parts.add("%,d шагов".format(steps).replace(',', ' ')) }
                            a.calories_burned?.takeIf { it > 0 }?.let { cal -> parts.add("${cal.toInt()} ккал") }
                            if (parts.isNotEmpty()) {
                                Text(
                                    parts.joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
        SectionHeader(title = "Достижения", subtitle = "${uiState.achievements.size}")
        uiState.achievements.forEach { ach ->
            AppCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.material3.Icon(
                            androidx.compose.material.icons.Icons.Filled.WorkspacePremium,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ach.title, fontWeight = FontWeight.SemiBold)
                        Text("+${ach.points} очков", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
