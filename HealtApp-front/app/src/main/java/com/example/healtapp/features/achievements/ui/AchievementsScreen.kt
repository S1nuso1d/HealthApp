package com.example.healtapp.features.achievements.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.EmptyStateCard
import com.example.healtapp.core.ui.components.FeatureHeroChip
import com.example.healtapp.core.ui.components.FeatureHighlightPanel
import com.example.healtapp.core.ui.components.FeatureInlineNotice
import com.example.healtapp.core.ui.components.FeatureScreenShell
import com.example.healtapp.core.ui.components.FeatureSectionTitle
import com.example.healtapp.core.ui.components.RoundedSectionTabs
import com.example.healtapp.core.ui.components.RoundedTabItem
import com.example.healtapp.core.ui.theme.brandingGradient
import com.example.healtapp.core.ui.theme.contentPrimaryColor
import com.example.healtapp.core.ui.theme.contentSecondaryColor
import com.example.healtapp.core.ui.theme.iconBadgeGradient
import com.example.healtapp.core.ui.theme.iconTintColor
import com.example.healtapp.data.network.dto.gamification.AchievementItemDto
import com.example.healtapp.features.achievements.presentation.AchievementsViewModel
import com.example.healtapp.features.achievements.ui.components.achievementIcon
import com.example.healtapp.features.achievements.ui.components.formatAchievementProgressValue

@Composable
fun AchievementsScreen(onBack: () -> Unit = {}) {
    val viewModel: AchievementsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }

    FeatureScreenShell(
        title = "Достижения",
        subtitle = if (uiState.guestMode) {
            "Демо — войдите для синхронизации"
        } else {
            "Очки и награды за привычки"
        },
        icon = Icons.Filled.EmojiEvents,
        onBack = onBack,
        heroFooter = if (!uiState.isLoading) {
            {
                Row(
                    modifier = Modifier.padding(start = 12.dp, top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FeatureHeroChip(label = "${uiState.totalPoints} очков")
                    FeatureHeroChip(label = "Открыто ${uiState.unlockedCount}/${uiState.totalCount}")
                }
            }
        } else {
            null
        },
    ) {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@FeatureScreenShell
        }

        if (uiState.guestMode) {
            FeatureInlineNotice(
                text = "В демо-режиме показаны примеры наград. Войдите, чтобы копить очки за свои привычки.",
            )
        }

        FeatureHighlightPanel {
            Column {
                Text(
                    text = "Всего очков",
                    style = MaterialTheme.typography.labelMedium,
                    color = contentSecondaryColor(),
                )
                Text(
                    text = "${uiState.totalPoints}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentPrimaryColor(),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Открыто",
                    style = MaterialTheme.typography.labelMedium,
                    color = contentSecondaryColor(),
                )
                Text(
                    text = "${uiState.unlockedCount} / ${uiState.totalCount}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = contentPrimaryColor(),
                )
            }
        }

        RoundedSectionTabs(
            tabs = listOf(
                RoundedTabItem(0, "Ежедневные", Icons.Filled.CalendarToday),
                RoundedTabItem(1, "Путь", Icons.Filled.Timeline),
                RoundedTabItem(2, "Рекорды", Icons.Filled.Star),
                RoundedTabItem(3, "Все", Icons.Filled.EmojiEvents),
            ),
            selected = tab,
            onSelect = { tab = it },
        )

        val items = when (tab) {
            0 -> uiState.achievements.filter { it.kind == "daily" }
            1 -> uiState.achievements.filter { it.kind == "journey" }
            2 -> uiState.achievements.filter { it.kind == "record" }
            else -> uiState.achievements
        }

        val (title, subtitle) = when (tab) {
            0 -> "Ежедневные" to "Быстрые победы за сегодня"
            1 -> "Долгий путь" to "Награды, которые собираются не за один день"
            2 -> "Личные рекорды" to "Эти достижения можно улучшать снова и снова"
            else -> "Каталог" to "Выполняйте цели — награды откроются автоматически"
        }

        FeatureSectionTitle(title = title, subtitle = subtitle)

        if (items.isEmpty()) {
            EmptyStateCard(
                text = "В этой категории пока нет достижений. Продолжайте вести дневник — награды откроются автоматически.",
                icon = Icons.Filled.EmojiEvents,
            )
        } else {
            items.forEach { item -> AchievementRow(item) }
        }

        uiState.error?.let {
            FeatureInlineNotice(text = it, isError = true)
        }
    }
}

@Composable
private fun AchievementRow(item: AchievementItemDto) {
    val alpha = if (item.unlocked) 1f else 0.55f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                shape = RoundedCornerShape(22.dp),
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(iconBadgeGradient())),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                achievementIcon(item.icon_key),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = iconTintColor().copy(alpha = alpha),
            )
        }
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
            Text(
                "✓",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun AchievementProgress(item: AchievementItemDto, alpha: Float) {
    if (item.kind == "record") {
        Text(
            text = item.record_label?.let { "Рекорд: $it" }
                ?: "Запишите активность, чтобы открыть рекорд",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
        )
        return
    }
    if (item.progress_target <= 0f) return
    val progress = (item.progress_current / item.progress_target).coerceIn(0f, 1f)
    Column(
        modifier = Modifier.padding(top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
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
                    .background(Brush.horizontalGradient(brandingGradient())),
            )
        }
        Text(
            text = "${formatAchievementProgressValue(item.progress_current, item.progress_unit)} / " +
                formatAchievementProgressValue(item.progress_target, item.progress_unit),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
        )
    }
}
