package com.example.healtapp.features.achievements.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppMessageBanner
import com.example.healtapp.core.ui.components.AppMessageType
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.data.network.dto.gamification.AchievementItemDto
import com.example.healtapp.features.achievements.presentation.AchievementsViewModel
import com.example.healtapp.features.achievements.ui.components.achievementIcon
import com.example.healtapp.features.achievements.ui.components.formatAchievementProgressValue

@Composable
fun AchievementsScreen(onBack: () -> Unit = {}) {
    val viewModel: AchievementsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AppScreen(
        title = "Достижения",
        subtitle = if (uiState.guestMode) "Демо — войдите для синхронизации" else "Очки и награды за привычки",
        headerIcon = Icons.Filled.EmojiEvents,
        onNavigateBack = onBack,
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator()
            return@AppScreen
        }
        AppCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Всего очков", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${uiState.totalPoints}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Открыто", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${uiState.unlockedCount} / ${uiState.totalCount}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        SectionHeader(title = "Ежедневные", subtitle = "Быстрые победы за сегодня")
        uiState.achievements.filter { it.kind == "daily" }.forEach { item ->
            AchievementRow(item)
        }
        SectionHeader(title = "Долгий путь", subtitle = "Награды, которые собираются не за один день")
        uiState.achievements.filter { it.kind == "journey" }.forEach { item ->
            AchievementRow(item)
        }
        SectionHeader(title = "Личные рекорды", subtitle = "Эти достижения можно улучшать снова и снова")
        uiState.achievements.filter { it.kind == "record" }.forEach { item ->
            AchievementRow(item)
        }
        if (uiState.achievements.none { it.kind in setOf("daily", "journey", "record") }) {
            SectionHeader(title = "Каталог", subtitle = "Выполняйте цели — награды откроются автоматически")
        }
        uiState.achievements.forEach { item ->
            if (item.kind !in setOf("daily", "journey", "record")) AchievementRow(item)
        }
        uiState.error?.let {
            AppMessageBanner(text = it, type = AppMessageType.Error)
        }
    }
}

@Composable
private fun AchievementRow(item: AchievementItemDto) {
    val alpha = if (item.unlocked) 1f else 0.45f
    AppCard(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                achievementIcon(item.icon_key),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                )
                Text(
                    item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                )
                Text(
                    "+${item.points} очков",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                )
                AchievementProgress(item, alpha)
            }
            if (item.unlocked) {
                Text("✓", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun AchievementProgress(item: AchievementItemDto, alpha: Float) {
    if (item.kind == "record") {
        Text(
            text = item.record_label?.let { "Рекорд: $it" } ?: "Запишите активность, чтобы открыть рекорд",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
        )
        return
    }
    if (item.progress_target <= 0f) return
    val progress = (item.progress_current / item.progress_target).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(7.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha)),
            )
        }
        Text(
            text = "${formatAchievementProgressValue(item.progress_current, item.progress_unit)} / ${formatAchievementProgressValue(item.progress_target, item.progress_unit)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
        )
    }
}
