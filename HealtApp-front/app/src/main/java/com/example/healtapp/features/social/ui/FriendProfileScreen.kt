package com.example.healtapp.features.social.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.EmptyStateCard
import com.example.healtapp.core.ui.components.FeatureHeroChip
import com.example.healtapp.core.ui.components.FeatureInlineNotice
import com.example.healtapp.core.ui.components.FeatureScreenShell
import com.example.healtapp.core.ui.components.RoundedSectionTabs
import com.example.healtapp.core.ui.components.RoundedTabItem
import com.example.healtapp.core.ui.theme.brandingGradient
import com.example.healtapp.core.ui.theme.contentPrimaryColor
import com.example.healtapp.core.ui.theme.contentSecondaryColor
import com.example.healtapp.core.ui.theme.heroContentColor
import com.example.healtapp.data.network.dto.social.FriendAchievementDto
import com.example.healtapp.data.network.dto.social.FriendActivityDto
import com.example.healtapp.data.network.dto.social.FeedPostDto
import com.example.healtapp.data.network.dto.social.UserCardDto
import com.example.healtapp.features.activity.presentation.activityTitleFromApi
import com.example.healtapp.features.profile.ProfileRus
import com.example.healtapp.features.social.presentation.FriendProfileUiState
import com.example.healtapp.features.social.presentation.FriendProfileViewModel
import com.example.healtapp.features.social.ui.components.CommunityFeedPostCard
import com.example.healtapp.features.social.ui.components.SocialUserAvatar

private const val TAB_FEED = 0
private const val TAB_ACTIVITIES = 1
private const val TAB_ACHIEVEMENTS = 2

@Composable
fun FriendProfileScreen(onBack: () -> Unit = {}) {
    val viewModel: FriendProfileViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(TAB_FEED) }

    FeatureScreenShell(
        title = uiState.user?.display_name ?: "Профиль",
        subtitle = when {
            uiState.isLoading -> "Загрузка…"
            uiState.isFriend -> "Друг · активность и публикации"
            uiState.user != null -> "Профиль пользователя"
            else -> "Сообщество"
        },
        icon = Icons.Filled.Person,
        onBack = onBack,
        scrollStateKey = "friend_profile_${uiState.user?.user_id ?: 0}_$tab",
    ) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                uiState.error?.let {
                    FeatureInlineNotice(text = it, isError = true)
                }
                uiState.actionMessage?.let {
                    FeatureInlineNotice(text = it)
                }

                uiState.user?.let { user ->
                    FriendProfileHero(
                        user = user,
                        isFriend = uiState.isFriend,
                        isBlocked = uiState.isBlocked,
                        onRequestFriend = viewModel::requestFriend,
                        onBlock = viewModel::blockUser,
                        onUnblock = viewModel::unblockUser,
                    )
                }

                if (uiState.isBlocked) return@FeatureScreenShell

                RoundedSectionTabs(
                    tabs = listOf(
                        RoundedTabItem(TAB_FEED, "Лента", Icons.Filled.Chat),
                        RoundedTabItem(TAB_ACTIVITIES, "Тренировки", Icons.AutoMirrored.Filled.DirectionsWalk),
                        RoundedTabItem(TAB_ACHIEVEMENTS, "Достижения", Icons.Filled.WorkspacePremium),
                    ),
                    selected = tab,
                    onSelect = { tab = it },
                )

                when (tab) {
                    TAB_FEED -> FriendProfileFeedTab(uiState)
                    TAB_ACTIVITIES -> FriendProfileActivitiesTab(uiState.activities)
                    TAB_ACHIEVEMENTS -> FriendProfileAchievementsTab(uiState.achievements)
                }
            }
        }
    }
}

@Composable
private fun FriendProfileFeedTab(uiState: FriendProfileUiState) {
    if (uiState.posts.isEmpty()) {
        EmptyStateCard(
            title = "Пока пусто",
            text = "Здесь появятся посты пользователя из сообщества.",
            icon = Icons.Filled.Chat,
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            uiState.posts.forEach { post ->
                FriendProfilePostCard(post = post)
            }
        }
    }
}

@Composable
private fun FriendProfilePostCard(post: FeedPostDto) {
    CommunityFeedPostCard(
        post = post,
        onAuthorClick = {},
        onReaction = {},
        onCommentClick = {},
    )
}

@Composable
private fun FriendProfileActivitiesTab(activities: List<FriendActivityDto>) {
    if (activities.isEmpty()) {
        EmptyStateCard(
            title = "Нет записей",
            text = "Тренировки скрыты настройками приватности или пока не добавлены.",
            icon = Icons.AutoMirrored.Filled.DirectionsWalk,
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            activities.forEach { activity ->
                AppCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(brandingGradient())),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.DirectionsWalk,
                                contentDescription = null,
                                tint = heroContentColor(),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                activityTitleFromApi(activity.activity_type.orEmpty()),
                                fontWeight = FontWeight.SemiBold,
                            )
                            val parts = buildList {
                                activity.duration_minutes?.let { add("$it мин") }
                                activity.steps?.takeIf { it > 0 }?.let { steps ->
                                    add("%,d шагов".format(steps).replace(',', ' '))
                                }
                                activity.calories_burned?.takeIf { it > 0 }?.let { cal ->
                                    add("${cal.toInt()} ккал")
                                }
                            }
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
    }
}

@Composable
private fun FriendProfileAchievementsTab(achievements: List<FriendAchievementDto>) {
    if (achievements.isEmpty()) {
        EmptyStateCard(
            title = "Пока пусто",
            text = "Достижения скрыты настройками приватности или пока не получены.",
            icon = Icons.Filled.WorkspacePremium,
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            achievements.forEach { achievement ->
                AppCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.WorkspacePremium,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(achievement.title, fontWeight = FontWeight.SemiBold)
                            Text(
                                "+${achievement.points} очков",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendProfileHero(
    user: UserCardDto,
    isFriend: Boolean,
    isBlocked: Boolean,
    onRequestFriend: () -> Unit,
    onBlock: () -> Unit,
    onUnblock: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SocialUserAvatar(user = user, size = 88.dp)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = user.display_name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentPrimaryColor(),
                    )
                    user.nickname?.let { nick ->
                        Text(
                            text = "@$nick",
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentSecondaryColor(),
                        )
                    }
                    listOfNotNull(user.first_name, user.last_name).joinToString(" ").takeIf { it.isNotBlank() }?.let { fullName ->
                        if (fullName != user.display_name.trim()) {
                            Text(
                                text = fullName,
                                style = MaterialTheme.typography.bodySmall,
                                color = contentSecondaryColor(),
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isFriend) {
                            FeatureHeroChip(label = "В друзьях")
                        }
                        if (isBlocked) {
                            FeatureHeroChip(label = "Заблокирован")
                        }
                        user.age?.takeIf { it > 0 }?.let { age ->
                            FeatureHeroChip(label = formatAgeLabel(age))
                        }
                        user.goal?.let { goal ->
                            FeatureHeroChip(label = ProfileRus.goalLabel(goal))
                        }
                    }
                }
                if (!user.is_self) {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "Действия с пользователем",
                                tint = contentSecondaryColor(),
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            if (isBlocked) {
                                DropdownMenuItem(
                                    text = { Text("Разблокировать") },
                                    onClick = {
                                        menuExpanded = false
                                        onUnblock()
                                    },
                                )
                            } else {
                                if (!isFriend) {
                                    DropdownMenuItem(
                                        text = { Text("Добавить в друзья") },
                                        onClick = {
                                            menuExpanded = false
                                            onRequestFriend()
                                        },
                                    )
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Заблокировать",
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onBlock()
                                    },
                                )
                            }
                        }
                    }
                }
            }
            if (isBlocked) {
                Text(
                    text = "Вы заблокировали этого пользователя — контент скрыт.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatAgeLabel(age: Int): String {
    val mod100 = age % 100
    val mod10 = age % 10
    val suffix = when {
        mod100 in 11..14 -> "лет"
        mod10 == 1 -> "год"
        mod10 in 2..4 -> "года"
        else -> "лет"
    }
    return "$age $suffix"
}
