package com.example.healtapp.features.social.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.core.ui.components.EmptyStateCard
import com.example.healtapp.core.ui.components.FeatureHeroChip
import com.example.healtapp.core.ui.components.FeatureInlineNotice
import com.example.healtapp.core.ui.components.FeatureScreenShell
import com.example.healtapp.core.ui.components.RoundedSectionTabs
import com.example.healtapp.core.ui.components.RoundedTabItem
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.core.ui.theme.contentPrimaryColor
import com.example.healtapp.core.ui.theme.contentSecondaryColor
import com.example.healtapp.core.ui.theme.heroContentColor
import com.example.healtapp.data.network.dto.social.ClubResponseDto
import com.example.healtapp.features.social.presentation.ClubsViewModel
import com.example.healtapp.features.social.ui.components.ClubAvatar
import com.example.healtapp.features.social.ui.components.CreateClubSheet

@Composable
fun ClubsScreen(
    onBack: () -> Unit,
    onOpenClub: (Int) -> Unit,
    viewModel: ClubsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.clubsState.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val myClubs = uiState.clubs.filter { it.is_member }
    val otherClubs = uiState.clubs.filter { !it.is_member }
    val filteredOther = if (searchQuery.isBlank()) {
        otherClubs
    } else {
        otherClubs.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.orEmpty().contains(searchQuery, ignoreCase = true)
        }
    }

    FeatureScreenShell(
        title = "Клубы",
        subtitle = if (uiState.guestMode) {
            "Демо-режим · сообщества по интересам"
        } else {
            "Сообщества по интересам и целям"
        },
        icon = Icons.Filled.Groups,
        onBack = onBack,
        heroActions = {
            IconButton(onClick = { showCreateDialog = true }) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Создать клуб",
                    tint = heroContentColor(),
                )
            }
        },
        heroFooter = if (!uiState.isLoading || uiState.clubs.isNotEmpty()) {
            {
                Row(
                    modifier = Modifier.padding(start = 12.dp, top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FeatureHeroChip(label = "Мои: ${myClubs.size}")
                    FeatureHeroChip(label = "Всего: ${uiState.clubs.size}")
                }
            }
        } else {
            null
        },
    ) {
        if (uiState.guestMode) {
            FeatureInlineNotice(text = "Демо-режим: клубы показаны как пример. Войдите, чтобы вступать и создавать сообщества.")
        }

        uiState.message?.let { FeatureInlineNotice(text = it) }
        uiState.error?.let { FeatureInlineNotice(text = it, isError = true) }

        RoundedSectionTabs(
            tabs = listOf(
                RoundedTabItem(0, "Мои клубы", Icons.Filled.Groups),
                RoundedTabItem(1, "Найти клуб", Icons.Filled.Search),
            ),
            selected = tab,
            onSelect = { tab = it },
        )

        if (uiState.isLoading && uiState.clubs.isEmpty()) {
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

        when (tab) {
            0 -> MyClubsTab(myClubs, onOpenClub, viewModel)
            else -> DiscoverClubsTab(filteredOther, searchQuery, { searchQuery = it }, onOpenClub, viewModel)
        }
    }

    CreateClubSheet(
        visible = showCreateDialog,
        onDismiss = { showCreateDialog = false },
        onConfirm = { name, description, rules, avatarUri ->
            viewModel.createClub(name, description, rules, avatarUri)
            showCreateDialog = false
        },
    )
}

@Composable
private fun MyClubsTab(
    clubs: List<ClubResponseDto>,
    onOpenClub: (Int) -> Unit,
    viewModel: ClubsViewModel,
) {
    SectionHeader(
        title = "Мои клубы",
        subtitle = if (clubs.isEmpty()) "Здесь появятся сообщества, в которых вы состоите" else "Всего: ${clubs.size}",
    )
    if (clubs.isEmpty()) {
        EmptyStateCard(
            text = "Вы ещё не состоите в клубах. Перейдите во вкладку «Найти клуб» или создайте свой.",
            icon = Icons.Filled.Groups,
            title = "Пока пусто",
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            clubs.forEach { club ->
                ClubListCard(club = club, onOpen = { onOpenClub(club.id) }, onJoin = { viewModel.joinClub(club.id) })
            }
        }
    }
}

@Composable
private fun DiscoverClubsTab(
    clubs: List<ClubResponseDto>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onOpenClub: (Int) -> Unit,
    viewModel: ClubsViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Поиск клуба",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentPrimaryColor(),
                )
                Text(
                    text = "Введите название или тему — список ниже обновится сразу",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AppTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    label = "Название или тема",
                    leadingIcon = Icons.Filled.Search,
                    imeAction = ImeAction.Search,
                )
            }
        }

        SectionHeader(
            title = "Доступные клубы",
            subtitle = when {
                searchQuery.isNotBlank() -> "Найдено: ${clubs.size}"
                clubs.isEmpty() -> "Пока нет открытых сообществ"
                else -> "Можно вступить · ${clubs.size}"
            },
        )

        if (clubs.isEmpty()) {
            EmptyStateCard(
                title = if (searchQuery.isBlank()) "Клубов пока нет" else "Ничего не найдено",
                text = if (searchQuery.isBlank()) {
                    "Создайте первый клуб — для обсуждений, опросов и обмена достижениями."
                } else {
                    "Попробуйте другое название или сократите запрос."
                },
                icon = Icons.Filled.Groups,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                clubs.forEach { club ->
                    ClubListCard(
                        club = club,
                        onOpen = { onOpenClub(club.id) },
                        onJoin = { viewModel.joinClub(club.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ClubListCard(
    club: ClubResponseDto,
    onOpen: () -> Unit,
    onJoin: () -> Unit,
) {
    AppCard(onClick = onOpen) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ClubAvatar(club = club)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = club.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentPrimaryColor(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                club.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentSecondaryColor(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Groups,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "${club.members_count} участников",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (club.is_member) {
                ClubMemberBadge()
            } else {
                AppButton(
                    text = "Вступить",
                    onClick = onJoin,
                    isSecondary = true,
                    modifier = Modifier.width(112.dp),
                )
            }
        }
    }
}

@Composable
private fun ClubMemberBadge() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
    ) {
        Text(
            text = "В клубе",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
