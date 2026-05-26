package com.example.healtapp.features.social.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppMessageBanner
import com.example.healtapp.core.ui.components.AppMessageType
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.data.network.dto.social.FeedPostDto
import com.example.healtapp.data.network.dto.social.UserCardDto
import com.example.healtapp.data.network.dto.social.WeeklyChallengeEntryDto
import com.example.healtapp.features.social.presentation.SocialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onBack: () -> Unit = {},
    onOpenFriend: (Int) -> Unit = {},
) {
    val viewModel: SocialViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showPrivacy by remember { mutableStateOf(false) }

    AppScreen(
        title = "Друзья и лента",
        subtitle = if (uiState.guestMode) "Демо-режим" else "Соревнуйтесь и делитесь прогрессом",
        headerIcon = Icons.Filled.Group,
        onNavigateBack = onBack,
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator()
            return@AppScreen
        }
        AppButton(text = "Приватность профиля", onClick = { showPrivacy = true })
        uiState.message?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
        }
        PrimaryTabRow(selectedTabIndex = uiState.selectedTab) {
            Tab(selected = uiState.selectedTab == 0, onClick = { viewModel.selectTab(0) }, text = { Text("Лента") })
            Tab(selected = uiState.selectedTab == 1, onClick = { viewModel.selectTab(1) }, text = { Text("Друзья") })
            Tab(selected = uiState.selectedTab == 2, onClick = { viewModel.selectTab(2) }, text = { Text("Поиск") })
        }
        when (uiState.selectedTab) {
            0 -> FeedTab(uiState, viewModel, onOpenFriend)
            1 -> FriendsTab(uiState, onOpenFriend, viewModel)
            2 -> SearchTab(uiState, viewModel, onOpenFriend)
        }
        uiState.error?.let { AppMessageBanner(text = it, type = AppMessageType.Error) }
    }
    if (showPrivacy) {
        PrivacySheet(
            profileVisibility = uiState.profileVisibility,
            feedVisibility = uiState.feedVisibility,
            showActivity = uiState.showActivity,
            showAchievements = uiState.showAchievements,
            onProfileVisibility = viewModel::setProfileVisibility,
            onFeedVisibility = viewModel::setFeedVisibility,
            onShowActivity = viewModel::setShowActivity,
            onShowAchievements = viewModel::setShowAchievements,
            onSave = {
                viewModel.savePrivacy()
                showPrivacy = false
            },
            onDismiss = { showPrivacy = false },
        )
    }
}

@Composable
private fun FeedTab(
    uiState: com.example.healtapp.features.social.presentation.SocialUiState,
    viewModel: SocialViewModel,
    onOpenFriend: (Int) -> Unit,
) {
    SectionHeader(title = "Новая запись", subtitle = "Текст и ссылка на фото/видео")
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppTextField(
                value = uiState.newPostText,
                onValueChange = viewModel::updateNewPostText,
                label = "Что нового?",
            )
            AppTextField(
                value = uiState.newPostMediaUrl,
                onValueChange = viewModel::updateNewPostMediaUrl,
                label = "Ссылка на фото или видео (URL)",
            )
            AppButton(text = "Опубликовать", onClick = viewModel::publishPost)
        }
    }
    SectionHeader(title = "Лента друзей", subtitle = "${uiState.feed.size} записей")
    if (uiState.feed.isEmpty()) {
        Text("Пока пусто — добавьте друзей или опубликуйте первую запись.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        uiState.feed.forEach { post -> FeedPostCard(post, onOpenFriend) }
    }
}

@Composable
private fun FeedPostCard(post: FeedPostDto, onOpenFriend: (Int) -> Unit) {
    AppCard(modifier = Modifier.padding(bottom = 8.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                post.author.display_name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onOpenFriend(post.author.user_id) },
            )
            post.body?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            post.media_url?.let {
                Text("📎 $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            post.activity?.let { a ->
                Text(
                    "Тренировка: ${a.activity_type ?: "—"} · ${a.duration_minutes ?: 0} мин · ${a.calories_burned?.toInt() ?: 0} ккал",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            post.created_at?.let {
                Text(it.take(16).replace('T', ' '), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun FriendsTab(
    uiState: com.example.healtapp.features.social.presentation.SocialUiState,
    onOpenFriend: (Int) -> Unit,
    viewModel: SocialViewModel,
) {
    if (uiState.weeklyChallenge.isNotEmpty()) {
        SectionHeader(title = "Челлендж недели", subtitle = "Кто больше шагов с понедельника")
        WeeklyChallengeCard(entries = uiState.weeklyChallenge, onOpenFriend = onOpenFriend)
    }
    if (uiState.pending.isNotEmpty()) {
        SectionHeader(title = "Заявки", subtitle = "Примите приглашение")
        uiState.pending.forEach { p ->
            AppCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(p.display_name, fontWeight = FontWeight.SemiBold)
                    AppButton(text = "Принять", onClick = { viewModel.acceptFriend(p.friendship_id) })
                }
            }
        }
    }
    SectionHeader(title = "Мои друзья", subtitle = "${uiState.friends.size}")
    uiState.friends.forEach { f -> UserRow(f, onOpenFriend) }
}

@Composable
private fun WeeklyChallengeCard(
    entries: List<WeeklyChallengeEntryDto>,
    onOpenFriend: (Int) -> Unit,
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            entries.take(5).forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (!entry.is_me) Modifier.clickable { onOpenFriend(entry.user_id) }
                            else Modifier,
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${entry.rank}. ${entry.display_name}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (entry.is_me) FontWeight.Bold else FontWeight.Normal,
                    )
                    Text(
                        text = "%,d шагов".format(entry.steps).replace(',', ' '),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchTab(
    uiState: com.example.healtapp.features.social.presentation.SocialUiState,
    viewModel: SocialViewModel,
    onOpenFriend: (Int) -> Unit,
) {
    SectionHeader(title = "Поиск", subtitle = "По email (мин. 2 символа)")
    AppCard {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AppTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                label = "Email",
                modifier = Modifier.weight(1f),
            )
            AppButton(text = "Найти", onClick = viewModel::search)
        }
    }
    uiState.searchResults.forEach { u ->
        AppCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(u.display_name, modifier = Modifier.clickable { onOpenFriend(u.user_id) })
                AppButton(text = "Добавить", onClick = { viewModel.requestFriend(u.user_id) })
            }
        }
    }
}

@Composable
private fun UserRow(user: UserCardDto, onOpen: (Int) -> Unit) {
    AppCard(modifier = Modifier.padding(bottom = 6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpen(user.user_id) },
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(user.display_name, fontWeight = FontWeight.Medium)
            user.goal?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
        }
    }
}
