package com.example.healtapp.features.social.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.healtapp.core.ui.components.EmptyStateCard
import com.example.healtapp.core.ui.components.FeatureHeroChip
import com.example.healtapp.core.ui.components.FeatureInlineNotice
import com.example.healtapp.core.ui.components.FeatureScreenShell
import com.example.healtapp.core.ui.components.GradientFormPanel
import com.example.healtapp.core.ui.components.GradientOutlinedField
import com.example.healtapp.core.ui.components.RoundedSectionTabs
import com.example.healtapp.core.ui.components.RoundedTabItem
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.core.ui.theme.heroContentColor
import com.example.healtapp.data.network.dto.social.ClubMemberResponseDto
import com.example.healtapp.data.network.dto.social.ClubPostResponseDto
import com.example.healtapp.data.network.dto.social.ClubResponseDto
import com.example.healtapp.features.social.presentation.ClubsViewModel
import com.example.healtapp.features.social.ui.components.ClubAvatar
import com.example.healtapp.features.social.ui.components.ClubPostComposerSheet
import com.example.healtapp.features.social.ui.components.SocialUserAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubDetailScreen(
    clubId: Int,
    onBack: () -> Unit,
    onOpenMember: (Int) -> Unit = {},
    viewModel: ClubsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.detailState.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    var showComposer by remember { mutableStateOf(false) }

    LaunchedEffect(clubId) { viewModel.loadClubDetail(clubId) }

    val club = uiState.club
    val showFab = club != null && club.is_member && (tab == 0 || tab == 1)

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading && club == null -> {
                FeatureScreenShell(
                    title = "Клуб",
                    subtitle = "Загрузка…",
                    icon = Icons.Filled.Groups,
                    onBack = onBack,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            club != null -> {
                FeatureScreenShell(
                    title = club.name,
                    subtitle = club.description?.takeIf { it.isNotBlank() }
                        ?: "Обсуждения, достижения и опросы",
                    icon = Icons.Filled.Groups,
                    onBack = onBack,
                    scrollStateKey = "club_detail_${club.id}_$tab",
                    extraBottomPadding = if (showFab) 72.dp else 16.dp,
                    heroActions = {
                        if (club.is_member) {
                            IconButton(onClick = { viewModel.leaveClub(clubId); onBack() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = "Выйти из клуба",
                                    tint = heroContentColor(),
                                )
                            }
                        }
                    },
                    heroFooter = {
                        Row(
                            modifier = Modifier.padding(start = 12.dp, top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ClubAvatar(club = club, size = 44.dp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FeatureHeroChip(label = "${club.members_count} участников")
                                FeatureHeroChip(label = "${uiState.posts.size} публикаций")
                                if (uiState.myRole == "admin") {
                                    FeatureHeroChip(label = "Админ")
                                }
                            }
                        }
                    },
                ) {
                    uiState.error?.let { FeatureInlineNotice(text = it, isError = true) }
                    uiState.message?.let { FeatureInlineNotice(text = it) }

                    club.rules?.takeIf { it.isNotBlank() }?.let { rules ->
                        AppCard {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Правила клуба",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = rules,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    val tabs = buildList {
                        add(RoundedTabItem(0, "Лента", Icons.Filled.Chat))
                        add(RoundedTabItem(1, "Участники", Icons.Filled.Groups))
                        if (uiState.myRole == "admin") {
                            add(RoundedTabItem(2, "Настройки", Icons.Filled.Settings))
                        }
                    }
                    RoundedSectionTabs(tabs = tabs, selected = tab, onSelect = { tab = it })

                    when (tab) {
                        0 -> ClubFeedTab(
                            clubId = clubId,
                            posts = uiState.posts,
                            isMember = club.is_member,
                            viewModel = viewModel,
                        )
                        1 -> ClubMembersTab(
                            members = uiState.members,
                            isAdmin = uiState.myRole == "admin",
                            clubId = clubId,
                            onOpenMember = onOpenMember,
                            viewModel = viewModel,
                        )
                        else -> ClubSettingsTab(club = club, viewModel = viewModel)
                    }
                }
            }
        }

        if (club != null && club.is_member) {
            when (tab) {
                0 -> ClubFab(
                    onClick = { showComposer = true },
                    icon = Icons.Filled.Add,
                    contentDescription = "Новая публикация",
                )
                1 -> ClubInviteFab(clubName = club.name)
                else -> Unit
            }
        }
    }

    ClubPostComposerSheet(
        visible = showComposer,
        onDismiss = { showComposer = false },
        onPublish = { type, body, pollOptions ->
            viewModel.createPost(clubId, type, body, pollOptions)
        },
    )
}

@Composable
private fun BoxScope.ClubFab(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 20.dp, bottom = 24.dp),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = CircleShape,
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}

@Composable
private fun BoxScope.ClubInviteFab(clubName: String) {
    val context = LocalContext.current
    ClubFab(
        onClick = {
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    android.content.Intent.EXTRA_TEXT,
                    "Присоединяйся к клубу «$clubName» в HealthSync!",
                )
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Пригласить"))
        },
        icon = Icons.Filled.PersonAdd,
        contentDescription = "Пригласить",
    )
}

@Composable
private fun ClubFeedTab(
    clubId: Int,
    posts: List<ClubPostResponseDto>,
    isMember: Boolean,
    viewModel: ClubsViewModel,
) {
    SectionHeader(
        title = "Лента клуба",
        subtitle = if (isMember) {
            "Всего: ${posts.size} · нажмите +, чтобы опубликовать"
        } else {
            "Всего: ${posts.size}"
        },
    )

    if (posts.isEmpty()) {
        EmptyStateCard(
            title = "Пока пусто",
            text = if (isMember) {
                "Начните обсуждение или поделитесь достижением — кнопка + внизу справа."
            } else {
                "Пока нет публикаций в этом клубе."
            },
            icon = Icons.Filled.Chat,
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            posts.forEach { post ->
                ClubPostCard(clubId = clubId, post = post, isMember = isMember, viewModel = viewModel)
            }
        }
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
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SocialUserAvatar(user = post.user, size = 36.dp)
                    Text(post.user.display_name, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    typeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
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
                                if (isMember) {
                                    Modifier.clickable { viewModel.votePoll(clubId, post.id, option) }
                                } else {
                                    Modifier
                                },
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
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
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
    SectionHeader(
        title = "Участники",
        subtitle = "Всего: ${members.size} · пригласите друзей кнопкой справа внизу",
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        SocialUserAvatar(user = member.user)
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
                        AppButton(
                            text = if (member.role == "admin") "−" else "Админ",
                            onClick = {
                                val next = if (member.role == "admin") "member" else "admin"
                                viewModel.setMemberRole(clubId, member.user.user_id, next)
                            },
                            isSecondary = true,
                            modifier = Modifier.width(88.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClubSettingsTab(
    club: ClubResponseDto,
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
