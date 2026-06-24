package com.example.healtapp.features.social.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.EmptyStateCard
import com.example.healtapp.core.ui.components.FeatureHeroChip
import com.example.healtapp.core.ui.components.FeatureInlineNotice
import com.example.healtapp.core.ui.components.FeatureScreenShell
import com.example.healtapp.core.ui.components.FeatureSectionTitle
import com.example.healtapp.core.ui.components.GradientFormPanel
import com.example.healtapp.core.ui.components.GradientOutlinedField
import com.example.healtapp.core.ui.theme.contentPrimaryColor
import com.example.healtapp.core.ui.theme.contentSecondaryColor
import com.example.healtapp.core.ui.theme.heroContentColor
import com.example.healtapp.core.ui.theme.subtleFillGradient
import com.example.healtapp.data.network.dto.social.UserCardDto
import com.example.healtapp.data.network.dto.social.WeeklyChallengeEntryDto
import androidx.compose.foundation.layout.width
import com.example.healtapp.features.social.presentation.SocialViewModel
import com.example.healtapp.features.social.ui.components.SocialUserAvatar
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onBack: () -> Unit = {},
    onOpenFriend: (Int) -> Unit = {},
) {
    val viewModel: SocialViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showPrivacy by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        FeatureScreenShell(
            title = "Друзья",
            subtitle = if (uiState.guestMode) {
                "Демо-режим · заявки и челлендж"
            } else {
                "Заявки, челлендж и поиск"
            },
            icon = Icons.Filled.Group,
            onBack = onBack,
            heroActions = {
                IconButton(onClick = { showPrivacy = true }) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = "Приватность",
                        tint = heroContentColor(),
                    )
                }
            },
            heroFooter = if (!uiState.isLoading || uiState.friends.isNotEmpty()) {
                {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FeatureHeroChip(label = "Друзей: ${uiState.friends.size}")
                        if (uiState.pending.isNotEmpty()) {
                            FeatureHeroChip(label = "Заявок: ${uiState.pending.size}")
                        }
                    }
                }
            } else {
                null
            },
        ) {
            if (uiState.isLoading && uiState.friends.isEmpty()) {
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
                FeatureInlineNotice(text = "Демо-режим: список друзей и челлендж — пример. Войдите для реального общения.")
            }

            uiState.message?.let { FeatureInlineNotice(text = it) }
            uiState.error?.let { FeatureInlineNotice(text = it, isError = true) }

            FriendsTab(
                uiState = uiState,
                onOpenFriend = onOpenFriend,
                viewModel = viewModel,
            )
        }

        FloatingActionButton(
            onClick = { showSearch = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
        ) {
            Icon(Icons.Filled.Search, contentDescription = "Найти друзей")
        }
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

    if (showSearch) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSearch = false },
            sheetState = sheetState,
        ) {
            SearchTab(
                uiState = uiState,
                viewModel = viewModel,
                onOpenFriend = { userId ->
                    showSearch = false
                    onOpenFriend(userId)
                },
            )
        }
    }
}

@Composable
private fun FriendsTab(
    uiState: com.example.healtapp.features.social.presentation.SocialUiState,
    onOpenFriend: (Int) -> Unit,
    viewModel: SocialViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (uiState.weeklyChallenge.isNotEmpty()) {
            FeatureSectionTitle(
                title = "Челлендж недели",
                subtitle = "Кто больше шагов с понедельника",
            )
            WeeklyChallengeCard(entries = uiState.weeklyChallenge, onOpenFriend = onOpenFriend)
        }
        if (uiState.pending.isNotEmpty()) {
            FeatureSectionTitle(
                title = "Новые заявки",
                subtitle = "Хотят добавить вас в друзья",
            )
            uiState.pending.forEach { p ->
                SocialListCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SocialUserAvatar(
                                user = UserCardDto(
                                    user_id = p.user_id,
                                    display_name = p.display_name,
                                    nickname = null,
                                    goal = null,
                                    has_avatar = p.has_avatar,
                                    is_self = false,
                                ),
                            )
                            Text(p.display_name, fontWeight = FontWeight.SemiBold, color = contentPrimaryColor())
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppButton(
                                text = "Принять",
                                onClick = { viewModel.acceptFriend(p.friendship_id) },
                                modifier = Modifier.width(112.dp),
                            )
                            AppButton(
                                text = "Отклонить",
                                onClick = { viewModel.declineFriend(p.friendship_id) },
                                isSecondary = true,
                                modifier = Modifier.width(112.dp),
                            )
                        }
                    }
                }
            }
        }
        FeatureSectionTitle(
            title = "Мои друзья",
            subtitle = "Всего: ${uiState.friends.size}",
        )
        if (uiState.friends.isEmpty()) {
            EmptyStateCard(
                text = "У вас пока нет друзей. Нажмите кнопку поиска внизу справа, чтобы найти знакомых.",
                icon = Icons.Filled.Group,
            )
        } else {
            uiState.friends.forEach { f ->
                UserRow(user = f, onOpen = onOpenFriend, onRemove = { viewModel.removeFriend(f.user_id) })
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
    val query = uiState.searchQuery.trim()
    LaunchedEffect(query) {
        if (query.length < 2) return@LaunchedEffect
        delay(400)
        viewModel.search()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        FeatureSectionTitle(
            title = "Найти друзей",
            subtitle = "Поиск по имени, фамилии, никнейму или email",
        )

        GradientFormPanel {
            GradientOutlinedField(
                value = uiState.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                label = "Имя, фамилия, никнейм или email",
                imeAction = ImeAction.Search,
                onImeAction = viewModel::search,
            )
            AppButton(
                text = if (uiState.isSearching) "Ищем..." else "Найти",
                onClick = viewModel::search,
                enabled = !uiState.isSearching,
                modifier = Modifier.fillMaxWidth(),
            )
            uiState.searchHint?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            uiState.error?.let {
                FeatureInlineNotice(text = it, isError = true)
            }
        }

        if (uiState.isSearching) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 4.dp))
        }

        if (query.length >= 2 && uiState.searchResults.isEmpty() && !uiState.isSearching && uiState.error == null) {
            EmptyStateCard(
                "Ничего не найдено. Убедитесь, что у человека заполнены имя и фамилия в профиле, или попробуйте email / никнейм.",
            )
        }

        uiState.searchResults.forEach { user ->
            SearchUserRow(
                user = user,
                onOpen = { onOpenFriend(user.user_id) },
                onAdd = { viewModel.requestFriend(user.user_id) },
            )
        }
    }
}

@Composable
private fun SearchUserRow(
    user: UserCardDto,
    onOpen: () -> Unit,
    onAdd: () -> Unit,
) {
    SocialListCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpen),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SocialUserAvatar(user = user, size = 44.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        user.display_name,
                        fontWeight = FontWeight.SemiBold,
                        color = contentPrimaryColor(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    user.nickname?.let {
                        Text(
                            "@$it",
                            style = MaterialTheme.typography.labelSmall,
                            color = contentSecondaryColor(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    formatUserAge(user.age)?.let { ageText ->
                        Text(
                            ageText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
            Surface(
                onClick = onAdd,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Filled.PersonAdd,
                        contentDescription = "Добавить в друзья",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

private fun formatUserAge(age: Int?): String? {
    if (age == null || age <= 0) return null
    val mod10 = age % 10
    val mod100 = age % 100
    val suffix = when {
        mod100 in 11..14 -> "лет"
        mod10 == 1 -> "год"
        mod10 in 2..4 -> "года"
        else -> "лет"
    }
    return "$age $suffix"
}

@Composable
private fun SocialListCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                shape = RoundedCornerShape(22.dp),
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        content()
    }
}

@Composable
private fun WeeklyChallengeCard(
    entries: List<WeeklyChallengeEntryDto>,
    onOpenFriend: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(subtleFillGradient()))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                shape = RoundedCornerShape(24.dp),
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        entries.take(5).forEach { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (!entry.is_me) Modifier.clickable { onOpenFriend(entry.user_id) } else Modifier),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${entry.rank}. ${entry.display_name}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (entry.is_me) FontWeight.Bold else FontWeight.Normal,
                    color = contentPrimaryColor(),
                )
                Text(
                    "%,d шагов".format(entry.steps).replace(',', ' '),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun UserRow(
    user: UserCardDto,
    onOpen: (Int) -> Unit,
    onRemove: (() -> Unit)? = null,
) {
    SocialListCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpen(user.user_id) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SocialUserAvatar(user = user, size = 44.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        user.display_name,
                        fontWeight = FontWeight.SemiBold,
                        color = contentPrimaryColor(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    user.nickname?.let {
                        Text(
                            "@$it",
                            style = MaterialTheme.typography.labelSmall,
                            color = contentSecondaryColor(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    formatUserAge(user.age)?.let { ageText ->
                        Text(
                            ageText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
            if (onRemove != null) {
                TextButton(onClick = onRemove) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
