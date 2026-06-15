package com.example.healtapp.features.social.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import com.example.healtapp.core.ui.animation.appListItemModifier
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppMessageBanner
import com.example.healtapp.core.ui.components.AppMessageType
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.components.EmptyStateCard
import com.example.healtapp.core.ui.components.PullToRefreshContainer
import com.example.healtapp.features.social.presentation.SocialViewModel
import com.example.healtapp.features.social.presentation.buildCommunityTimeline
import com.example.healtapp.features.social.presentation.CommunityTimelineItem
import com.example.healtapp.features.social.ui.components.CommunityChallengeBanner
import com.example.healtapp.features.social.ui.components.CommunityClubPostCard
import com.example.healtapp.features.social.ui.components.CommunityClubsRail
import com.example.healtapp.features.social.ui.components.CommunityCommentsSheet
import com.example.healtapp.features.social.ui.components.CommunityComposeFab
import com.example.healtapp.features.social.ui.components.CommunityPostDeleteOverlay
import com.example.healtapp.features.social.ui.components.CommunityComposeSheet
import com.example.healtapp.features.social.ui.components.CommunityFeedPostCard
import com.example.healtapp.features.social.ui.components.CommunityFeedTopBar
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
    var expandedChallengeId by remember { mutableIntStateOf(-1) }
    val myClubs = remember(uiState.clubs) { uiState.clubs.filter { it.is_member } }
    val timeline = remember(uiState.feed, uiState.clubFeedEntries) {
        buildCommunityTimeline(uiState.feed, uiState.clubFeedEntries)
    }

    Box(Modifier.fillMaxSize()) {
        AppScreen(
            title = "Сообщество",
            subtitle = if (uiState.guestMode) {
                "Демо-лента · достижения, рецепты и тренировки"
            } else {
                "Достижения, рецепты и мотивация друзей"
            },
            headerIcon = Icons.Filled.Groups,
            onNavigateBack = onBack,
            scrollable = false,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 0.dp),
        ) {
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@AppScreen
            }

            PullToRefreshContainer(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 72.dp),
                ) {
                    uiState.message?.let { msg ->
                        item(key = "message") {
                            AppMessageBanner(text = msg, type = AppMessageType.Info)
                        }
                    }

                    item(key = "stories") {
                        CommunityStoriesRail(
                            stories = uiState.stories,
                            onAddStory = { viewModel.setShowStoryComposeSheet(true) },
                            onOpenStory = viewModel::openStoryViewer,
                        )
                    }

                    uiState.challenges.firstOrNull()?.let { challenge ->
                        item(key = "challenge_${challenge.id}") {
                            CommunityChallengeBanner(
                                challenge = challenge,
                                leaderboard = uiState.challengeLeaderboards[challenge.id].orEmpty(),
                                expanded = expandedChallengeId == challenge.id,
                                onToggleExpand = {
                                    expandedChallengeId = if (expandedChallengeId == challenge.id) -1 else challenge.id
                                },
                                onJoin = { viewModel.joinChallenge(challenge.id) },
                                onLeave = { viewModel.leaveChallenge(challenge.id) },
                                onOpenFriend = onOpenFriend,
                            )
                        }
                    }

                    item(key = "clubs_rail") {
                        CommunityClubsRail(
                            clubs = myClubs,
                            onOpenClub = onOpenClub,
                        )
                    }

                    item(key = "feed_header") {
                        CommunityFeedTopBar(
                            postsCount = uiState.feed.size,
                            clubPostsCount = uiState.clubFeedEntries.size,
                            onOpenFriends = onOpenFriends,
                        )
                    }

                    if (timeline.isEmpty()) {
                        item(key = "empty_feed") {
                            EmptyStateCard(
                                title = "Лента пуста",
                                text = "Поделитесь тренировкой, рецептом ПП или загляните в клубы — здесь появятся новости друзей и сообществ.",
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
                            AppMessageBanner(text = err, type = AppMessageType.Error)
                        }
                    }
                }
            }
        }

        CommunityComposeFab(
            onClick = { viewModel.setShowComposeSheet(true) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp),
        )
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
