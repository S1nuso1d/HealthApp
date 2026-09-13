@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.healtapp.features.social.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.healtapp.core.ui.animation.AppMotion
import com.example.healtapp.di.ApiServerConfigEntryPoint
import com.example.healtapp.di.ImageLoaderEntryPoint
import com.example.healtapp.features.social.util.SocialMediaUrls
import dagger.hilt.android.EntryPointAccessors
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.core.ui.components.BrandedFilterChip
import com.example.healtapp.core.ui.components.FeatureCollapsibleCard
import com.example.healtapp.core.ui.components.FeatureGlassCard
import com.example.healtapp.core.ui.components.FeatureSectionTitle
import com.example.healtapp.core.ui.components.GradientFormPanel
import com.example.healtapp.core.ui.components.PersonAvatar
import com.example.healtapp.core.ui.theme.brandingGradient
import com.example.healtapp.core.ui.theme.contentPrimaryColor
import com.example.healtapp.core.ui.theme.contentSecondaryColor
import com.example.healtapp.data.network.dto.social.ChallengeLeaderboardEntryDto
import com.example.healtapp.data.network.dto.social.ChallengeResponseDto
import com.example.healtapp.data.network.dto.social.ClubPostResponseDto
import com.example.healtapp.data.network.dto.social.ClubResponseDto
import com.example.healtapp.data.network.dto.social.FeedActivityDto
import com.example.healtapp.data.network.dto.social.FeedCommentDto
import com.example.healtapp.data.network.dto.social.FeedPostDto
import com.example.healtapp.data.network.dto.social.FeedReactionDto
import com.example.healtapp.data.network.dto.social.FeedStoryDto
import com.example.healtapp.data.network.dto.social.UserCardDto
import com.example.healtapp.features.activity.presentation.activityTitleFromApi
import com.example.healtapp.features.social.presentation.COMMUNITY_REACTIONS

enum class PostShareKind(
    val label: String,
    val shortLabel: String,
    val placeholder: String,
    val icon: ImageVector,
) {
    WORKOUT("Тренировка", "Спорт", "Как прошла тренировка?", Icons.Filled.DirectionsRun),
    RECIPE("Рецепт ПП", "Еда", "Что приготовили?", Icons.Filled.Restaurant),
    ACHIEVEMENT("Достижение", "Успех", "Чем гордитесь сегодня?", Icons.Filled.EmojiEvents),
    LIFEHACK("Лайфхак", "Совет", "Поделитесь полезным советом…", Icons.Filled.Lightbulb),
}

fun inferPostKind(post: FeedPostDto): PostShareKind? {
    if (post.activity != null) return PostShareKind.WORKOUT
    val body = post.body?.lowercase().orEmpty()
    return when {
        listOf("рецепт", "пп ", "завтрак", "обед", "ужин", "кбжу").any { it in body } -> PostShareKind.RECIPE
        listOf("достижен", "рекорд", "цель", "норму", "streak", "медал").any { it in body } -> PostShareKind.ACHIEVEMENT
        listOf("совет", "лайфхак", "секрет", "лайф").any { it in body } -> PostShareKind.LIFEHACK
        else -> null
    }
}

@Composable
fun CommunityComposeFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = CircleShape,
    ) {
        Icon(Icons.Filled.Add, contentDescription = "Новая публикация")
    }
}

@Composable
fun CommunityPostDeleteOverlay(
    post: FeedPostDto,
    isDeleting: Boolean,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(AppMotion.tweenMedium()) + slideInVertically(
                initialOffsetY = { it / 4 },
                animationSpec = spring(stiffness = 340f, dampingRatio = 0.82f),
            ),
            exit = fadeOut(AppMotion.tweenShort()),
        ) {
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(36.dp),
                    )
                    Text(
                        "Удалить публикацию?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    post.body?.takeIf { it.isNotBlank() }?.let { body ->
                        Text(
                            body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        AppButton(
                            text = "Отмена",
                            onClick = onDismiss,
                            isSecondary = true,
                            enabled = !isDeleting,
                            modifier = Modifier.weight(1f),
                        )
                        AppButton(
                            text = if (isDeleting) "Удаляем…" else "Удалить",
                            onClick = onDelete,
                            enabled = !isDeleting,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommunityStoriesRail(
    stories: List<FeedStoryDto>,
    currentUser: UserCardDto? = null,
    onAddStory: () -> Unit,
    onOpenStory: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selfAuthor = stories.firstOrNull { it.author.is_self }?.author ?: currentUser
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FeatureSectionTitle(
            title = "Истории",
            subtitle = if (stories.isEmpty()) "Добавьте короткую историю за день" else "${stories.size} активных",
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(end = 4.dp),
        ) {
            item(key = "add_story") {
                StoryAvatarItem(
                    label = "Вы",
                    author = selfAuthor,
                    previewUrl = null,
                    isAdd = true,
                    onClick = onAddStory,
                )
            }
            itemsIndexed(stories, key = { _, s -> "story_${s.author.user_id}" }) { index, story ->
                StoryAvatarItem(
                    label = if (story.author.is_self) "Вы" else story.author.display_name,
                    author = story.author,
                    previewUrl = story.preview_url,
                    isAdd = false,
                    onClick = { onOpenStory(index) },
                )
            }
        }
    }
}

@Composable
private fun StoryAvatarItem(
    label: String,
    author: UserCardDto?,
    previewUrl: String?,
    isAdd: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val imageLoader = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            ImageLoaderEntryPoint::class.java,
        ).imageLoader()
    }
    val baseUrl = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            ApiServerConfigEntryPoint::class.java,
        ).apiServerConfig().baseUrl()
    }
    val resolvedPreview = remember(previewUrl, baseUrl) {
        SocialMediaUrls.resolveMediaUrl(baseUrl, previewUrl)
    }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(stiffness = 380f, dampingRatio = 0.62f),
        label = "storyScale",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(78.dp),
    ) {
        Box(
            modifier = Modifier
                .scale(scale)
                .size(72.dp)
                .border(
                    width = 2.5.dp,
                    brush = Brush.linearGradient(brandingGradient()),
                    shape = CircleShape,
                )
                .padding(3.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            when {
                isAdd -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Добавить историю",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                resolvedPreview != null -> {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(resolvedPreview)
                            .crossfade(true)
                            .build(),
                        imageLoader = imageLoader,
                        contentDescription = label,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            if (author != null) {
                                SocialUserAvatar(
                                    user = author,
                                    modifier = Modifier.fillMaxSize(),
                                    size = null,
                                )
                            }
                        },
                        error = {
                            if (author != null) {
                                SocialUserAvatar(
                                    user = author,
                                    modifier = Modifier.fillMaxSize(),
                                    size = null,
                                )
                            } else {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        },
                    )
                }
                author != null -> {
                    SocialUserAvatar(
                        user = author,
                        modifier = Modifier.fillMaxSize(),
                        size = null,
                        useGradientFallback = true,
                    )
                }
                else -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (isAdd) "Добавить" else label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun CommunityChallengeBanner(
    challenge: ChallengeResponseDto,
    leaderboard: List<ChallengeLeaderboardEntryDto>,
    onJoin: () -> Unit,
    onLeave: () -> Unit,
    onOpenFriend: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    FeatureCollapsibleCard(
        modifier = modifier,
        title = challenge.title,
        subtitle = "Челлендж · ${challenge.participants_count} участников",
    ) {
        CommunityChallengeDetails(
            challenge = challenge,
            leaderboard = leaderboard,
            onJoin = onJoin,
            onLeave = onLeave,
            onOpenFriend = onOpenFriend,
        )
    }
}

@Composable
private fun CommunityChallengeDetails(
    challenge: ChallengeResponseDto,
    leaderboard: List<ChallengeLeaderboardEntryDto>,
    onJoin: () -> Unit,
    onLeave: () -> Unit,
    onOpenFriend: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!challenge.description.isNullOrBlank()) {
            Text(
                challenge.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (challenge.my_progress != null) {
            val progressPercent = if (challenge.target_value > 0) {
                (challenge.my_progress.toFloat() / challenge.target_value).coerceIn(0f, 1f)
            } else {
                0f
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Мой прогресс", style = MaterialTheme.typography.labelSmall)
                Text(
                    "${challenge.my_progress} / ${challenge.target_value}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            LinearProgressIndicator(
                progress = { progressPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
            )
            if (leaderboard.isNotEmpty()) {
                Text("Лидеры", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                leaderboard.take(3).forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenFriend(entry.user_id) },
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "#${entry.rank} ${if (entry.is_me) "Вы" else entry.display_name}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text("${entry.progress}", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            OutlinedButton(onClick = onLeave, modifier = Modifier.fillMaxWidth()) {
                Text("Покинуть", color = MaterialTheme.colorScheme.error)
            }
        } else {
            AppButton(text = "Присоединиться", onClick = onJoin, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun CommunityFeedPostCard(
    post: FeedPostDto,
    onAuthorClick: () -> Unit,
    onReaction: (String) -> Unit,
    onCommentClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val kind = inferPostKind(post)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.75f),
        label = "postScale",
    )

    FeatureGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onLongPress != null) {
                    Modifier.pointerInput(post.id) {
                        detectTapGestures(onLongPress = { onLongPress() })
                    }
                } else {
                    Modifier
                },
            )
            .scale(cardScale),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAuthorClick)
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SocialUserAvatar(user = post.author, size = 44.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        post.author.display_name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        post.created_at?.let {
                            Text(
                                formatFeedTime(it),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        kind?.let { k ->
                            PostKindBadge(kind = k)
                        }
                    }
                }
            }

            post.body?.takeIf { it.isNotBlank() }?.let { body ->
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                extractFirstUrl(body)?.let { url ->
                    FeedLinkPreviewCard(url = url)
                }
            }

            post.media_url?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "Фото публикации",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.height(10.dp))
            }

            post.activity?.let { activity ->
                CommunityLinkedActivityChip(activity)
                Spacer(Modifier.height(8.dp))
            }

            CommunityReactionRow(
                reactions = post.reactions,
                total = post.reaction_total,
                myReaction = post.my_reaction,
                commentsCount = post.comments_count,
                onReaction = onReaction,
                onCommentClick = onCommentClick,
            )
        }
    }
}

@Composable
private fun FeedLinkPreviewCard(url: String) {
    val context = LocalContext.current
    val domain = remember(url) {
        runCatching { Uri.parse(url).host?.removePrefix("www.") }.getOrNull()
    }
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clickable {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Link,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = domain ?: "Ссылка",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private val FEED_URL_REGEX = Regex("""https?://[^\s<>"']+""")

private fun extractFirstUrl(text: String): String? =
    FEED_URL_REGEX.find(text)?.value?.trimEnd('.', ',', ';', ')', '"', '\'')

@Composable
private fun PostKindBadge(kind: PostShareKind) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                kind.icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                kind.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun CommunityLinkedActivityChip(activity: FeedActivityDto) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(brandingGradient())),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.DirectionsRun,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    activityTitleFromApi(activity.activity_type.orEmpty()),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                val parts = buildList {
                    activity.duration_minutes?.let { add("$it мин") }
                    activity.distance_km?.let { add(String.format("%.1f км", it)) }
                    activity.steps?.takeIf { it > 0 }?.let { add("%,d шагов".format(it).replace(',', ' ')) }
                    activity.calories_burned?.takeIf { it > 0 }?.let { add("${it.toInt()} ккал") }
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

@Composable
private fun CommunityReactionRow(
    reactions: List<FeedReactionDto>,
    total: Int,
    myReaction: String?,
    commentsCount: Int,
    onReaction: (String) -> Unit,
    onCommentClick: () -> Unit,
) {
    var showReactions by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { showReactions = !showReactions }) {
                    Icon(
                        Icons.Outlined.FavoriteBorder,
                        contentDescription = "Реакции",
                        tint = if (myReaction != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onCommentClick) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Комментарии")
                }
                if (commentsCount > 0) {
                    Text(
                        commentsCount.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
            }
            if (total > 0) {
                Text(
                    reactions.joinToString("  ") { "${it.emoji} ${it.count}" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        AnimatedVisibility(
            visible = showReactions,
            enter = fadeIn(AppMotion.tweenShort()) + slideInVertically(AppMotion.tweenShort()) { it / 2 },
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(COMMUNITY_REACTIONS) { emoji ->
                    val selected = myReaction == emoji
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1.12f else 1f,
                        animationSpec = spring(stiffness = 500f),
                        label = "emojiScale",
                    )
                    Surface(
                        onClick = { onReaction(emoji) },
                        shape = CircleShape,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.scale(scale),
                    ) {
                        Text(
                            emoji,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }
}

private fun formatFeedTime(iso: String): String {
    return iso.take(16).replace('T', ' ')
}

@Composable
fun CommunityComposeSheet(
    text: String,
    mediaUri: Uri?,
    activities: List<FeedActivityDto>,
    selectedActivityId: Int?,
    onTextChange: (String) -> Unit,
    onMediaChange: (Uri?) -> Unit,
    onSelectActivity: (Int?) -> Unit,
    onPublish: () -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean,
    currentUser: UserCardDto? = null,
) {
    var shareKind by remember { mutableStateOf(PostShareKind.WORKOUT) }
    val openCamera = rememberSocialCameraLauncher(filePrefix = "feed_post", onCaptured = onMediaChange)

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) onMediaChange(uri) },
    )

    val canPublish = !isLoading && (text.isNotBlank() || mediaUri != null || selectedActivityId != null)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Новая публикация",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Закрыть")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            PostShareKind.entries.forEach { kind ->
                ComposeKindOption(
                    kind = kind,
                    selected = shareKind == kind,
                    onClick = { shareKind = kind },
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    if (currentUser != null) {
                        SocialUserAvatar(user = currentUser, size = 44.dp)
                    }
                    OutlinedTextField(
                        value = text,
                        onValueChange = onTextChange,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 96.dp),
                        placeholder = {
                            Text(
                                shareKind.placeholder,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                        ),
                        maxLines = 8,
                    )
                }

                if (mediaUri != null) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        AsyncImage(
                            model = mediaUri,
                            contentDescription = "Выбранное фото",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        IconButton(
                            onClick = { onMediaChange(null) },
                            modifier = Modifier
                                .padding(6.dp)
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f), CircleShape),
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Удалить фото",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ComposeToolbarIcon(
                            icon = Icons.Filled.PhotoLibrary,
                            contentDescription = "Галерея",
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                        )
                        ComposeToolbarIcon(
                            icon = Icons.Filled.PhotoCamera,
                            contentDescription = "Камера",
                            onClick = openCamera,
                        )
                    }
                    FilledIconButton(
                        onClick = onPublish,
                        enabled = canPublish,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Опубликовать")
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = shareKind == PostShareKind.WORKOUT && activities.isNotEmpty(),
            enter = fadeIn(AppMotion.tweenShort()) + slideInVertically(AppMotion.tweenShort()) { it / 3 },
            exit = fadeOut(AppMotion.tweenShort()),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Привязать тренировку",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(activities.take(8), key = { it.id ?: it.hashCode() }) { act ->
                        val id = act.id ?: return@items
                        ComposeActivityChip(
                            label = "${activityTitleFromApi(act.activity_type.orEmpty())} · ${act.duration_minutes ?: 0} мин",
                            selected = selectedActivityId == id,
                            onClick = {
                                onSelectActivity(if (selectedActivityId == id) null else id)
                            },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun ComposeKindOption(
    kind: PostShareKind,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(min = 64.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .then(
                    if (selected) {
                        Modifier.background(Brush.linearGradient(brandingGradient()))
                    } else {
                        Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                kind.icon,
                contentDescription = kind.label,
                tint = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            kind.shortLabel,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
        )
    }
}

@Composable
private fun ComposeToolbarIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f), CircleShape),
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun ComposeActivityChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
            },
        ),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun CommunityStoryComposeSheet(
    onPublish: (Uri) -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean,
) {
    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    val openCamera = rememberSocialCameraLauncher(filePrefix = "story") { uri -> mediaUri = uri }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) mediaUri = uri },
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Новая история", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Фото из тренировки или кухни — видно друзьям 24 часа.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        MediaPickerRow(
            mediaUri = mediaUri,
            previewHeight = 300.dp,
            onGallery = {
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onCamera = openCamera,
            onClear = { mediaUri = null },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppButton(text = "Отмена", onClick = onDismiss, modifier = Modifier.weight(1f))
            AppButton(
                text = if (isLoading) "Публикация…" else "Опубликовать",
                onClick = { mediaUri?.let(onPublish) },
                modifier = Modifier.weight(1f),
                enabled = mediaUri != null && !isLoading,
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun MediaPickerRow(
    mediaUri: Uri?,
    previewHeight: androidx.compose.ui.unit.Dp = 200.dp,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onClear: () -> Unit,
) {
    if (mediaUri != null) {
        Box(contentAlignment = Alignment.TopEnd) {
            AsyncImage(
                model = mediaUri,
                contentDescription = "Выбранное фото",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(previewHeight)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
            )
            IconButton(
                onClick = onClear,
                modifier = Modifier
                    .padding(8.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape),
            ) {
                Icon(Icons.Filled.Close, "Удалить")
            }
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onGallery, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Галерея")
            }
            FilledTonalButton(onClick = onCamera, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Камера")
            }
        }
    }
}

@Composable
fun CommunityCommentsSheet(
    comments: List<FeedCommentDto>,
    onSendComment: (String) -> Unit,
) {
    var commentText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
            .padding(top = 8.dp),
    ) {
        Text(
            "Комментарии",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (comments.isEmpty()) {
                item {
                    Text(
                        "Пока нет комментариев. Будьте первым!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 20.dp),
                    )
                }
            } else {
                items(comments, key = { it.id }) { comment ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PersonAvatar(name = comment.author.display_name, size = 36.dp)
                        Column(Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    comment.author.display_name,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                comment.created_at?.let {
                                    Text(
                                        formatFeedTime(it),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Text(comment.text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Напишите комментарий…") },
                maxLines = 3,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (commentText.isNotBlank()) {
                        onSendComment(commentText)
                        commentText = ""
                        focusManager.clearFocus()
                    }
                }),
                shape = RoundedCornerShape(24.dp),
            )
            IconButton(
                onClick = {
                    if (commentText.isNotBlank()) {
                        onSendComment(commentText)
                        commentText = ""
                        focusManager.clearFocus()
                    }
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, "Отправить", tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun CommunityClubPostCard(
    club: ClubResponseDto,
    post: ClubPostResponseDto,
    onOpenClub: () -> Unit,
    onOpenAuthor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typeLabel = when (post.post_type) {
        "achievement" -> "Достижение"
        "poll" -> "Опрос"
        else -> "Обсуждение"
    }
    FeatureGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenClub),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                    .clickable(onClick = onOpenClub)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ClubAvatar(club = club, size = 36.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = club.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Клуб · $typeLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Filled.Groups,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenAuthor),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SocialUserAvatar(user = post.user, size = 40.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.user.display_name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    post.created_at?.let {
                        Text(
                            text = formatFeedTime(it),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            post.body?.takeIf { it.isNotBlank() }?.let { body ->
                Text(text = body, style = MaterialTheme.typography.bodyMedium)
            }
            if (post.post_type == "poll" && !post.poll_options.isNullOrEmpty()) {
                post.poll_options.forEach { option ->
                    val votes = post.poll_votes?.get(option) ?: 0
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(option, style = MaterialTheme.typography.bodySmall)
                            Text(
                                "$votes",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}
