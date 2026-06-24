package com.example.healtapp.features.social.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.healtapp.data.network.dto.social.FeedStoryDto
import com.example.healtapp.features.social.ui.components.SocialUserAvatar

@Composable
fun StoryViewerScreen(
    stories: List<FeedStoryDto>,
    initialIndex: Int,
    onClose: () -> Unit,
    onStoryViewed: (Int) -> Unit = {},
) {
    if (stories.isEmpty()) {
        onClose()
        return
    }

    var currentUserIndex by remember { mutableIntStateOf(initialIndex) }
    var currentStoryIndex by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }

    val currentUserStories = stories.getOrNull(currentUserIndex)
    if (currentUserStories == null || currentUserStories.items.isEmpty()) {
        onClose()
        return
    }

    val currentStory = currentUserStories.items.getOrNull(currentStoryIndex)
    if (currentStory == null) {
        onClose()
        return
    }

    LaunchedEffect(currentStory.id) {
        onStoryViewed(currentStory.id)
    }

    val progress = remember { Animatable(0f) }

    LaunchedEffect(currentUserIndex, currentStoryIndex, isPaused) {
        if (!isPaused) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = ((1f - progress.value) * 5000).toInt(),
                    easing = LinearEasing
                )
            )
            if (currentStoryIndex < currentUserStories.items.lastIndex) {
                currentStoryIndex++
                progress.snapTo(0f)
            } else if (currentUserIndex < stories.lastIndex) {
                currentUserIndex++
                currentStoryIndex = 0
                progress.snapTo(0f)
            } else {
                onClose()
            }
        } else {
            progress.stop()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPaused = true
                        val pressStartTime = System.currentTimeMillis()
                        try {
                            awaitRelease()
                        } finally {
                            isPaused = false
                            val pressDuration = System.currentTimeMillis() - pressStartTime
                            if (pressDuration < 200) {
                                val x = it.x
                                val width = size.width
                                if (x < width * 0.3f) {
                                    if (currentStoryIndex > 0) {
                                        currentStoryIndex--
                                        progress.snapTo(0f)
                                    } else if (currentUserIndex > 0) {
                                        currentUserIndex--
                                        currentStoryIndex = stories[currentUserIndex].items.lastIndex
                                        progress.snapTo(0f)
                                    }
                                } else {
                                    if (currentStoryIndex < currentUserStories.items.lastIndex) {
                                        currentStoryIndex++
                                        progress.snapTo(0f)
                                    } else if (currentUserIndex < stories.lastIndex) {
                                        currentUserIndex++
                                        currentStoryIndex = 0
                                        progress.snapTo(0f)
                                    } else {
                                        onClose()
                                    }
                                }
                            }
                        }
                    }
                )
            }
    ) {
        Crossfade(
            targetState = currentStory.media_url,
            label = "storyImage",
            animationSpec = tween(durationMillis = 280),
        ) { mediaUrl ->
            AsyncImage(
                model = mediaUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                currentUserStories.items.forEachIndexed { index, _ ->
                    LinearProgressIndicator(
                        progress = {
                            when {
                                index < currentStoryIndex -> 1f
                                index == currentStoryIndex -> progress.value
                                else -> 0f
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                SocialUserAvatar(
                    user = currentUserStories.author,
                    modifier = Modifier.border(1.5.dp, Color.White.copy(alpha = 0.85f), CircleShape),
                    size = 36.dp,
                    useGradientFallback = true,
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = currentUserStories.author.display_name,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )

                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Закрыть",
                        tint = Color.White
                    )
                }
            }
        }

        if (currentUserStories.author.is_self) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 32.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Visibility,
                    contentDescription = "Просмотры",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = currentStory.view_count.toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
