package com.example.healtapp.features.social.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.example.healtapp.core.ui.components.GradientFormPanel
import com.example.healtapp.core.ui.components.GradientOutlinedField
import com.example.healtapp.core.ui.components.PersonAvatar
import com.example.healtapp.core.ui.components.RoundedSectionTabs
import com.example.healtapp.core.ui.components.RoundedTabItem
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.data.network.dto.social.ClubMemberResponseDto
import com.example.healtapp.data.network.dto.social.ClubPostResponseDto
import com.example.healtapp.features.social.presentation.ClubsViewModel

@Composable
fun ClubDetailScreen(
    clubId: Int,
    onBack: () -> Unit,
    onOpenMember: (Int) -> Unit = {},
    viewModel: ClubsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.detailState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }

    LaunchedEffect(clubId) { viewModel.loadClubDetail(clubId) }

    val club = uiState.club
    AppScreen(
        title = club?.name ?: "Клуб",
        subtitle = club?.description ?: "Обсуждения и достижения",
        headerIcon = Icons.Filled.Groups,
        onNavigateBack = onBack,
    ) {
        uiState.error?.let { AppMessageBanner(text = it, type = AppMessageType.Error) }
        uiState.message?.let { AppMessageBanner(text = it, type = AppMessageType.Info) }

        if (uiState.isLoading && club == null) {
            CircularProgressIndicator()
            return@AppScreen
        }

        club ?: return@AppScreen

        AppCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PersonAvatar(name = club.name, size = 64.dp, useGradient = true)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(club.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${club.members_count} участников", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    club.rules?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppButton(
                    text = "Пригласить",
                    onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, "Присоединяйся к клубу «${club.name}» в HealthSync!")
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Пригласить"))
                    },
                    isSecondary = true,
                )
                if (club.is_member) {
                    AppButton(text = "Выйти", onClick = { viewModel.leaveClub(clubId); onBack() }, isSecondary = true)
                }
            }
        }

        val tabs = buildList {
            add(RoundedTabItem(0, "Лента"))
            add(RoundedTabItem(1, "Участники"))
            if (uiState.myRole == "admin") add(RoundedTabItem(2, "Настройки"))
        }
        RoundedSectionTabs(tabs = tabs, selected = tab, onSelect = { tab = it })

        when (tab) {
            0 -> ClubFeedTab(clubId, uiState.posts, club.is_member, viewModel)
            1 -> ClubMembersTab(clubId, uiState.members, uiState.myRole == "admin", onOpenMember, viewModel)
            else -> ClubSettingsTab(club, viewModel)
        }
    }
}

@Composable
private fun ClubFeedTab(
    clubId: Int,
    posts: List<ClubPostResponseDto>,
    isMember: Boolean,
    viewModel: ClubsViewModel,
) {
    var composerType by remember { mutableIntStateOf(0) }
    var body by remember { mutableStateOf("") }
    var pollOptions by remember { mutableStateOf("") }
    val typeKey = when (composerType) {
        1 -> "achievement"
        2 -> "poll"
        else -> "discussion"
    }

    if (isMember) {
        SectionHeader(title = "Новая публикация", subtitle = "Обсуждение, достижение или опрос")
        GradientFormPanel {
            RoundedSectionTabs(
                tabs = listOf(
                    RoundedTabItem(0, "Обсуждение", Icons.Filled.Chat),
                    RoundedTabItem(1, "Достижение", Icons.Filled.EmojiEvents),
                    RoundedTabItem(2, "Опрос", Icons.Filled.Poll),
                ),
                selected = composerType,
                onSelect = { composerType = it },
            )
            GradientOutlinedField(
                value = body,
                onValueChange = { body = it },
                label = if (composerType == 1) "Поделитесь методом или результатом" else "Текст",
                singleLine = false,
            )
            if (composerType == 2) {
                GradientOutlinedField(
                    value = pollOptions,
                    onValueChange = { pollOptions = it },
                    label = "Варианты через запятую",
                )
            }
            AppButton(
                text = "Опубликовать",
                onClick = {
                    val opts = if (composerType == 2) {
                        pollOptions.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    } else null
                    viewModel.createPost(clubId, typeKey, body, opts)
                    body = ""
                    pollOptions = ""
                },
                enabled = body.isNotBlank() || composerType == 2,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    SectionHeader(title = "Лента клуба", subtitle = "Всего: ${posts.size}")

    if (posts.isEmpty()) {
        EmptyStateCard("Пока нет публикаций. Начните обсуждение или поделитесь достижением.")
    } else {
        posts.forEach { post -> ClubPostCard(clubId, post, isMember, viewModel) }
    }
}

@Composable
private fun ClubPostCard(
    clubId: Int,
    post: ClubPostResponseDto,
    isMember: Boolean,
    viewModel: ClubsViewModel,
) {
    val typeLabel = when (post.post_type) {
        "achievement" -> "Достижение"
        "poll" -> "Опрос"
        else -> "Обсуждение"
    }
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(post.user.display_name, fontWeight = FontWeight.SemiBold)
                Text(typeLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            post.body?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            if (post.post_type == "poll" && post.poll_options != null) {
                post.poll_options.forEach { option ->
                    val votes = post.poll_votes?.get(option) ?: 0
                    val selected = post.my_vote == option
                    AppCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isMember) Modifier.clickable { viewModel.votePoll(clubId, post.id, option) }
                                else Modifier,
                            ),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                option,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "$votes",
                                style = MaterialTheme.typography.labelLarge,
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
private fun ClubMembersTab(
    clubId: Int,
    members: List<ClubMemberResponseDto>,
    isAdmin: Boolean,
    onOpenMember: (Int) -> Unit,
    viewModel: ClubsViewModel,
) {
    SectionHeader(title = "Участники", subtitle = "Всего: ${members.size}")
    members.forEach { member ->
        AppCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenMember(member.user.user_id) },
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PersonAvatar(name = member.user.display_name)
                    Column {
                        Text(member.user.display_name, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (member.role == "admin") "Администратор" else "Участник",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (isAdmin && !member.user.is_self) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AppButton(
                            text = if (member.role == "admin") "−" else "Админ",
                            onClick = {
                                val next = if (member.role == "admin") "member" else "admin"
                                viewModel.setMemberRole(clubId, member.user.user_id, next)
                            },
                            isSecondary = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClubSettingsTab(
    club: com.example.healtapp.data.network.dto.social.ClubResponseDto,
    viewModel: ClubsViewModel,
) {
    var name by remember(club.id) { mutableStateOf(club.name) }
    var description by remember(club.id) { mutableStateOf(club.description.orEmpty()) }
    var rules by remember(club.id) { mutableStateOf(club.rules.orEmpty()) }
    var avatarUrl by remember(club.id) { mutableStateOf(club.avatar_url.orEmpty()) }

    GradientFormPanel {
        SectionHeader(title = "Настройки клуба", subtitle = "Только для администратора")
        GradientOutlinedField(value = name, onValueChange = { name = it }, label = "Название")
        GradientOutlinedField(value = description, onValueChange = { description = it }, label = "Описание")
        GradientOutlinedField(value = rules, onValueChange = { rules = it }, label = "Правила")
        GradientOutlinedField(value = avatarUrl, onValueChange = { avatarUrl = it }, label = "URL фото клуба")
        AppButton(
            text = "Сохранить",
            onClick = { viewModel.updateClub(club.id, name, description, rules, avatarUrl) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
