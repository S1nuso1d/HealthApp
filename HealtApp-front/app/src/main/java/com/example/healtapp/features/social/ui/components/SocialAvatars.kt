package com.example.healtapp.features.social.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.healtapp.core.ui.components.PersonAvatar
import com.example.healtapp.data.network.dto.social.ClubResponseDto
import com.example.healtapp.data.network.dto.social.UserCardDto
import com.example.healtapp.di.ApiServerConfigEntryPoint
import com.example.healtapp.di.ImageLoaderEntryPoint
import com.example.healtapp.features.social.util.SocialMediaUrls
import dagger.hilt.android.EntryPointAccessors

@Composable
fun SocialUserAvatar(
    user: UserCardDto,
    modifier: Modifier = Modifier,
    size: Dp? = 44.dp,
    useGradientFallback: Boolean = true,
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
    val avatarUrl = remember(user.user_id, user.has_avatar, baseUrl) {
        SocialMediaUrls.userAvatarUrl(baseUrl, user.user_id, user.has_avatar)
    }
    val layoutModifier = modifier.then(
        if (size != null) Modifier.size(size) else Modifier,
    )
    val displayName = user.display_name.ifBlank { user.nickname ?: "?" }

    if (avatarUrl != null) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(avatarUrl)
                .crossfade(true)
                .build(),
            imageLoader = imageLoader,
            contentDescription = displayName,
            modifier = layoutModifier
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                    shape = CircleShape,
                ),
            contentScale = ContentScale.Crop,
            loading = {
                PersonAvatar(
                    name = displayName,
                    modifier = Modifier.fillMaxSize(),
                    size = size ?: 44.dp,
                    useGradient = useGradientFallback,
                )
            },
            error = {
                PersonAvatar(
                    name = displayName,
                    modifier = Modifier.fillMaxSize(),
                    size = size ?: 44.dp,
                    useGradient = useGradientFallback,
                )
            },
        )
    } else {
        PersonAvatar(
            name = displayName,
            modifier = layoutModifier,
            size = size ?: 44.dp,
            useGradient = useGradientFallback,
        )
    }
}

@Composable
fun ClubAvatar(
    club: ClubResponseDto,
    modifier: Modifier = Modifier,
    size: Dp? = 52.dp,
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
    val avatarUrl = remember(club.id, club.avatar_url, baseUrl) {
        SocialMediaUrls.resolveMediaUrl(baseUrl, club.avatar_url)
    }
    val layoutModifier = modifier.then(
        if (size != null) Modifier.size(size) else Modifier,
    )

    if (avatarUrl != null) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(avatarUrl)
                .crossfade(true)
                .build(),
            imageLoader = imageLoader,
            contentDescription = club.name,
            modifier = layoutModifier
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                    shape = CircleShape,
                ),
            contentScale = ContentScale.Crop,
            loading = {
                PersonAvatar(
                    name = club.name,
                    modifier = Modifier.fillMaxSize(),
                    size = size ?: 52.dp,
                    useGradient = true,
                )
            },
            error = {
                PersonAvatar(
                    name = club.name,
                    modifier = Modifier.fillMaxSize(),
                    size = size ?: 52.dp,
                    useGradient = true,
                )
            },
        )
    } else {
        PersonAvatar(
            name = club.name,
            modifier = layoutModifier,
            size = size ?: 52.dp,
            useGradient = true,
        )
    }
}
