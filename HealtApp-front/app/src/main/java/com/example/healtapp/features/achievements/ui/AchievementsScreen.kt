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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppCard
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
import com.example.healtapp.data.network.dto.gamification.AchievementLeaderboardEntryDto
import com.example.healtapp.data.network.dto.gamification.AchievementPointRuleDto
import com.example.healtapp.data.network.dto.gamification.AchievementTierDto
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
        subtitle = "Награды, сезон и турнирная таблица",
        icon = Icons.Filled.EmojiEvents,
        onBack = onBack,
        heroFooter = if (!uiState.isLoading) {
            {
                Row(
                    modifier = Modifier.padding(start = 12.dp, top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FeatureHeroChip(label = "${uiState.totalPoints} очков")
                    FeatureHeroChip(label = uiState.tier?.title ?: "Бронза")
                    FeatureHeroChip(label = "Сезон ${uiState.seasonPoints}")
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

        SeasonRankCard(
            seasonPoints = uiState.seasonPoints,
            seasonLabel = uiState.seasonLabel,
            tier = uiState.tier,
            lifetimePoints = uiState.totalPoints,
            unlocked = uiState.unlockedCount,
            total = uiState.totalCount,
        )

        RoundedSectionTabs(
            tabs = listOf(
                RoundedTabItem(0, "День", Icons.Filled.CalendarToday),
                RoundedTabItem(1, "Месяц", Icons.Filled.CalendarMonth),
                RoundedTabItem(2, "Путь", Icons.Filled.Timeline),
                RoundedTabItem(3, "Рекорды", Icons.Filled.Star),
                RoundedTabItem(4, "Турнир", Icons.Filled.Leaderboard),
            ),
            selected = tab,
            onSelect = { tab = it },
        )

        when (tab) {
            4 -> TournamentTab(
                leaderboard = uiState.leaderboard,
                pointRules = uiState.pointRules,
                seasonPoints = uiState.seasonPoints,
                tier = uiState.tier,
            )
            else -> {
                val items = when (tab) {
                    0 -> uiState.achievements.filter { it.kind == "daily" }
                    1 -> uiState.achievements.filter { it.kind == "monthly" }
                    2 -> uiState.achievements.filter { it.kind == "journey" }
                    3 -> uiState.achievements.filter { it.kind == "record" }
                    else -> uiState.achievements
                }
                val (title, subtitle) = when (tab) {
                    0 -> "Ежедневные" to "Быстрые победы за сегодня"
                    1 -> "Ежемесячные" to "Цели на текущий календарный месяц"
                    2 -> "Долгий путь" to "Награды, которые собираются не за один день"
                    3 -> "Личные рекорды" to "Можно улучшать снова и снова"
                    else -> "Каталог" to "Выполняйте цели — награды откроются сами"
                }
                FeatureSectionTitle(title = title, subtitle = subtitle)
                if (items.isEmpty()) {
                    EmptyStateCard(
                        text = "В этой категории пока нет достижений. Продолжайте вести дневник.",
                        icon = Icons.Filled.EmojiEvents,
                    )
                } else {
                    items.forEach { item -> AchievementRow(item) }
                }
            }
        }

        uiState.error?.let {
            FeatureInlineNotice(text = it, isError = true)
        }
    }
}

@Composable
private fun SeasonRankCard(
    seasonPoints: Int,
    seasonLabel: String?,
    tier: AchievementTierDto?,
    lifetimePoints: Int,
    unlocked: Int,
    total: Int,
) {
    val nextTitle = tier?.next_title
    val toNext = tier?.points_to_next
    val progress = if (tier?.next_code != null && toNext != null) {
        val gained = (seasonPoints - tier.min_points).coerceAtLeast(0)
        val need = toNext.coerceAtLeast(1) + gained
        (gained.toFloat() / need.toFloat()).coerceIn(0f, 1f)
    } else {
        1f
    }

    FeatureHighlightPanel {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Ранг сезона",
                        style = MaterialTheme.typography.labelMedium,
                        color = contentSecondaryColor(),
                    )
                    Text(
                        text = tier?.title ?: "Бронза",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentPrimaryColor(),
                    )
                    Text(
                        text = buildString {
                            append("$seasonPoints очков")
                            seasonLabel?.let { append(" · $it") }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = contentSecondaryColor(),
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Всего",
                        style = MaterialTheme.typography.labelMedium,
                        color = contentSecondaryColor(),
                    )
                    Text(
                        text = "$lifetimePoints",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = contentPrimaryColor(),
                    )
                    Text(
                        text = "$unlocked / $total",
                        style = MaterialTheme.typography.bodySmall,
                        color = contentSecondaryColor(),
                    )
                }
            }
            if (nextTitle != null && toNext != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
                Text(
                    text = "До ранга «$nextTitle»: ещё $toNext очков",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentSecondaryColor(),
                )
            } else {
                Text(
                    text = "Максимальный ранг сезона — Легенда",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentSecondaryColor(),
                )
            }
        }
    }
}

@Composable
private fun TournamentTab(
    leaderboard: List<AchievementLeaderboardEntryDto>,
    pointRules: List<AchievementPointRuleDto>,
    seasonPoints: Int,
    tier: AchievementTierDto?,
) {
    FeatureSectionTitle(
        title = "Турнирная таблица",
        subtitle = "Очки за записи воды, сна, питания и активности в этом месяце",
    )

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Как начисляются очки",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentPrimaryColor(),
            )
            pointRules.forEach { rule ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = rule.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentSecondaryColor(),
                    )
                    Text(
                        text = "+${rule.points}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                text = "Ранги: Бронза → Серебро → Золото → Платина → Алмаз → Легенда",
                style = MaterialTheme.typography.labelSmall,
                color = contentSecondaryColor(),
            )
        }
    }

    FeatureSectionTitle(
        title = "Лидеры месяца",
        subtitle = "Ваш результат: $seasonPoints · ${tier?.title ?: "Бронза"}",
    )

    if (leaderboard.isEmpty()) {
        EmptyStateCard(
            text = "Пока никого в таблице. Записывайте привычки — очки появятся здесь.",
            icon = Icons.Filled.Leaderboard,
        )
    } else {
        leaderboard.forEach { entry -> LeaderboardRow(entry) }
    }
}

@Composable
private fun LeaderboardRow(entry: AchievementLeaderboardEntryDto) {
    val highlight = entry.is_self
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (highlight) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )
            .border(
                width = 1.dp,
                color = if (highlight) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(22.dp),
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(brandingGradient().map { it.copy(alpha = 0.85f) })),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${entry.rank}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (entry.is_self) "${entry.display_name} · вы" else entry.display_name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentPrimaryColor(),
            )
            Text(
                text = entry.tier_title,
                style = MaterialTheme.typography.bodySmall,
                color = contentSecondaryColor(),
            )
        }
        Text(
            text = "${entry.season_points}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
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
