package com.example.healtapp.features.profile.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.ImageLoader
import com.example.healtapp.core.common.Constants
import com.example.healtapp.core.ui.theme.heroBlockGradient
import com.example.healtapp.core.ui.theme.heroContentColor
import com.example.healtapp.core.ui.theme.heroIconBackdrop
import com.example.healtapp.core.ui.theme.isAppDarkTheme
import com.example.healtapp.features.profile.ProfileRus

@Composable
fun ProfileHeroBlock(
    initial: String,
    avatarUrl: String?,
    imageLoader: ImageLoader,
    goal: String,
    activityLevel: String,
    guestMode: Boolean,
    isUploadingAvatar: Boolean,
    enabled: Boolean,
    onAvatarClick: () -> Unit,
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (isAppDarkTheme()) 8.dp else 26.dp))
            .background(Brush.linearGradient(heroBlockGradient()))
            .clickable(enabled = enabled, onClick = onAvatarClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(heroIconBackdrop()),
                contentAlignment = Alignment.Center,
            ) {
                if (avatarUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(avatarUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Фото профиля",
                        imageLoader = imageLoader,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = heroContentColor(),
                    )
                }
            }
            if (isUploadingAvatar) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = heroContentColor(),
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Твой аккаунт",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = heroContentColor(),
            )
            Text(
                text = if (goal.isNotBlank()) {
                    "Цель: ${ProfileRus.goalLabel(goal)}"
                } else {
                    "Заполни данные — рекомендации станут точнее"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = heroContentColor().copy(alpha = 0.92f),
            )
            if (activityLevel.isNotBlank()) {
                Text(
                    text = "Активность: ${ProfileRus.activityLevelLabel(activityLevel)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = heroContentColor().copy(alpha = 0.85f),
                )
            }
            Text(
                text = if (guestMode) {
                    "Демо-режим — фото и синхронизация недоступны"
                } else {
                    "Нажми на аватар · до ${Constants.AVATAR_MAX_BYTES / (1024 * 1024)} МБ"
                },
                style = MaterialTheme.typography.labelMedium,
                color = heroContentColor().copy(alpha = 0.75f),
            )
        }
    }
}
