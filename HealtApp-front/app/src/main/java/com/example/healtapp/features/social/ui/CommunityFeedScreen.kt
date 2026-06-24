package com.example.healtapp.features.social.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.animation.appListItemModifier
import com.example.healtapp.core.ui.components.EmptyStateCard
import com.example.healtapp.core.ui.components.FeatureHeroBar
import com.example.healtapp.core.ui.components.FeatureHeroChip
import com.example.healtapp.core.ui.components.FeatureInlineNotice
import com.example.healtapp.core.ui.components.FeatureSectionTitle
import com.example.healtapp.core.ui.components.PullToRefreshContainer
import com.example.healtapp.core.ui.theme.heroContentColor
import com.example.healtapp.core.ui.theme.screenBackgroundGradient
import com.example.healtapp.features.social.presentation.CommunityTimelineItem
import com.example.healtapp.features.social.presentation.SocialViewModel
import com.example.healtapp.features.social.presentation.buildCommunityTimeline
import com.example.healtapp.features.social.ui.components.CommunityChallengeBanner
import com.example.healtapp.features.social.ui.components.CommunityClubPostCard
import com.example.healtapp.features.social.ui.components.CommunityCommentsSheet
import com.example.healtapp.features.social.ui.components.CommunityComposeSheet
import com.example.healtapp.features.social.ui.components.CommunityFeedPostCard
import com.example.healtapp.features.social.ui.components.CommunityPostDeleteOverlay
import com.example.healtapp.features.social.ui.components.CommunityStoriesRail
import com.example.healtapp.features.social.ui.components.CommunityStoryComposeSheet

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CommunityFeedScreen(
    onBack: () -> Unit = {},
    onOpenFriend: (Int) -> Unit = {},
    onOpenFriends: () -> Unit = {},
    onOpenClub: (Int) -> Unit = {},
) {
    val viewModel: SocialViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val timeline = remember(uiState.feed, uiState.clubFeedEntries) {
        buildCommunityTimeline(uiState.feed, uiState.clubFeedEntries)
    }
    val totalPosts = uiState.feed.size + uiState.clubFeedEntries.size

    val currentUser = remember(uiState.stories, uiState.feed) {
        uiState.stories.firstOrNull { it.author.is_self }?.author
            ?: uiState.feed.firstOrNull { it.author.is_self }?.author
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(screenBackgroundGradient()))
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            FeatureHeroBar(
                title = "Сообщество",
                subtitle = if (uiState.guestMode) {
                    "Демо-лента · достижения, рецепты и тренировки"
                } else {
                    "Достижения, рецепты и мотивация друзей"
                },
                icon = Icons.Filled.Groups,
                onBack = onBack,
                actions = {
                    IconButton(onClick = onOpenFriends) {
                        Icon(
                            Icons.Filled.Group,
                            contentDescription = "Друзья",
                            tint = heroContentColor(),
                        )
                    }
                },
                footer = if (!uiState.isLoading || timeline.isNotEmpty()) {
                    {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.padding(start = 12.dp, top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FeatureHeroChip(label = "Публикаций: $totalPosts")
                            uiState.challenges.firstOrNull()?.let { challenge ->
                                FeatureHeroChip(label = "Челлендж: ${challenge.participants_count}")
                            }
                        }
                    }
                } else {
                    null
                },
            )

            if (uiState.isLoading && timeline.isEmpty() && uiState.stories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                PullToRefreshContainer(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 16.dp,
                            bottom = 88.dp,
                        ),
                    ) {
                        if (uiState.guestMode) {
                            item(key = "guest") {
                                FeatureInlineNotice(
                                    text = "Демо-режим: лента показана как пример. Войдите, чтобы публиковать и видеть друзей.",
                                )
                            }
                        }

                        uiState.message?.let { msg ->
                            item(key = "message") {
                                FeatureInlineNotice(text = msg)
                            }
                        }

                        item(key = "stories") {
                            CommunityStoriesRail(
                                stories = uiState.stories,
                                currentUser = currentUser,
                                onAddStory = { viewModel.setShowStoryComposeSheet(true) },
                                onOpenStory = viewModel::openStoryViewer,
                            )
                        }

                        uiState.challenges.firstOrNull()?.let { challenge ->
                            item(key = "challenge_${challenge.id}") {
                                CommunityChallengeBanner(
                                    challenge = challenge,
                                    leaderboard = uiState.challengeLeaderboards[challenge.id].orEmpty(),
                                    onJoin = { viewModel.joinChallenge(challenge.id) },
                                    onLeave = { viewModel.leaveChallenge(challenge.id) },
                                    onOpenFriend = onOpenFriend,
                                )
                            }
                        }

                        item(key = "feed_header") {
                            FeatureSectionTitle(
                                title = "Лента",
                                subtitle = when {
                                    totalPosts == 0 -> "Пока тихо — будьте первым"
                                    uiState.clubFeedEntries.isNotEmpty() ->
                                        "$totalPosts публикаций · друзья и клубы"
                                    else -> "$totalPosts публикаций"
                                },
                            )
                        }

                        if (timeline.isEmpty()) {
                            item(key = "empty_feed") {
                                EmptyStateCard(
                                    text = "Поделитесь тренировкой, рецептом ПП или достижением — здесь появятся новости друзей.",
                                    icon = Icons.Filled.Groups,
                                )
                            }
                        } else {
                            items(
                                items = timeline,
                                key = { item ->
                                    when (item) {
                                        is CommunityTimelineItem.UserPost -> "user_${item.post.id}"
                                        is CommunityTimelineItem.ClubPost -> "club_${item.club.id}_${item.post.id}"
                                    }
                                },
                            ) { item ->
                                when (item) {
                                    is CommunityTimelineItem.UserPost -> {
                                        CommunityFeedPostCard(
                                            post = item.post,
                                            onAuthorClick = { onOpenFriend(item.post.author.user_id) },
                                            onReaction = { emoji -> viewModel.toggleReaction(item.post.id, emoji) },
                                            onCommentClick = { viewModel.openComments(item.post.id) },
                                            onLongPress = if (item.post.author.is_self) {
                                                { viewModel.openDeleteOverlay(item.post) }
                                            } else {
                                                null
                                            },
                                            modifier = appListItemModifier(),
                                        )
                                    }
                                    is CommunityTimelineItem.ClubPost -> {
                                        CommunityClubPostCard(
                                            club = item.club,
                                            post = item.post,
                                            onOpenClub = { onOpenClub(item.club.id) },
                                            onOpenAuthor = { onOpenFriend(item.post.user.user_id) },
                                            modifier = appListItemModifier(),
                                        )
                                    }
                                }
                            }
                        }

                        uiState.error?.let { err ->
                            item(key = "error") {
                                FeatureInlineNotice(text = err, isError = true)
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { viewModel.setShowComposeSheet(true) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Новая публикация")
        }
    }

    if (uiState.selectedPostForComments != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val postId = uiState.selectedPostForComments!!
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeComments() },
            sheetState = sheetState,
        ) {
            CommunityCommentsSheet(
                comments = uiState.postComments[postId].orEmpty(),
                onSendComment = { text -> viewModel.postComment(postId, text) },
            )
        }
    }

    if (uiState.showComposeSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val context = LocalContext.current
        ModalBottomSheet(
            onDismissRequest = { viewModel.setShowComposeSheet(false) },
            sheetState = sheetState,
        ) {
            CommunityComposeSheet(
                text = uiState.newPostText,
                mediaUri = uiState.selectedMediaUri,
                activities = uiState.linkableActivities,
                selectedActivityId = uiState.selectedActivityId,
                onTextChange = viewModel::updateNewPostText,
                onMediaChange = viewModel::updateSelectedMediaUri,
                onSelectActivity = viewModel::selectActivityForPost,
                onPublish = { viewModel.publishPost(context) },
                onDismiss = { viewModel.setShowComposeSheet(false) },
                isLoading = uiState.isLoading,
                currentUser = currentUser,
            )
        }
    }

    if (uiState.showStoryComposeSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val context = LocalContext.current
        ModalBottomSheet(
            onDismissRequest = { viewModel.setShowStoryComposeSheet(false) },
            sheetState = sheetState,
        ) {
            CommunityStoryComposeSheet(
                onPublish = { uri -> viewModel.publishStory(context, uri) },
                onDismiss = { viewModel.setShowStoryComposeSheet(false) },
                isLoading = uiState.isLoading,
            )
        }
    }

    if (uiState.storyViewerIndex != null) {
        StoryViewerScreen(
            stories = uiState.stories,
            initialIndex = uiState.storyViewerIndex!!,
            onClose = { viewModel.closeStoryViewer() },
            onStoryViewed = viewModel::markStoryViewed,
        )
    }

    uiState.selectedPostForDelete?.let { post ->
        CommunityPostDeleteOverlay(
            post = post,
            isDeleting = uiState.isLoading,
            onDelete = { viewModel.deletePost(post.id) },
            onDismiss = viewModel::closeDeleteOverlay,
        )
    }
}
