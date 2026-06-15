package com.example.healtapp.core.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.theme.MintPrimary
import com.example.healtapp.core.ui.theme.SkyPrimary
import com.example.healtapp.core.ui.theme.brandingGradient
import com.example.healtapp.core.ui.theme.screenBackgroundGradient

data class FeatureGuidePage(
    val title: String,
    val lead: String,
    val bullets: List<String>,
    val icon: ImageVector,
    val miniIcons: List<ImageVector> = emptyList(),
)

@Composable
fun FeatureGuideOverlay(
    visible: Boolean,
    pages: List<FeatureGuidePage>,
    onDismiss: () -> Unit,
    sectionLabel: String? = null,
) {
    if (!visible || pages.isEmpty()) return

    var pageIndex by remember(visible, pages) { mutableIntStateOf(0) }
    val isFirst = pageIndex == 0
    val isLast = pageIndex == pages.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.58f)),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 28.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(screenBackgroundGradient()))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        sectionLabel?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = "Шаг ${pageIndex + 1} из ${pages.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Пропустить")
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    AnimatedContent(
                        targetState = pageIndex,
                        modifier = Modifier.fillMaxSize(),
                        transitionSpec = {
                            val forward = targetState > initialState
                            val enterOffset: (Int) -> Int = { full -> if (forward) full / 3 else -full / 3 }
                            val exitOffset: (Int) -> Int = { full -> if (forward) -full / 3 else full / 3 }
                            (slideInHorizontally(initialOffsetX = enterOffset) + fadeIn())
                                .togetherWith(slideOutHorizontally(targetOffsetX = exitOffset) + fadeOut())
                        },
                        label = "featureGuidePage",
                    ) { index ->
                        val current = pages[index]
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            FeatureGuideHeroIcon(
                                icon = current.icon,
                                miniIcons = current.miniIcons,
                            )
                            Text(
                                text = current.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = current.lead,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            current.bullets.forEach { bullet ->
                                FeatureGuideBulletRow(text = bullet)
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FeatureGuidePageDots(
                        count = pages.size,
                        selected = pageIndex,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        IconButton(
                            onClick = { if (!isFirst) pageIndex -= 1 },
                            enabled = !isFirst,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад",
                                tint = if (isFirst) {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            )
                        }

                        AppButton(
                            text = if (isLast) "Понятно" else "Далее",
                            onClick = {
                                if (isLast) onDismiss() else pageIndex += 1
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                        )

                        IconButton(
                            onClick = { if (!isLast) pageIndex += 1 },
                            enabled = !isLast,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Вперёд",
                                tint = if (isLast) {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureGuideHeroIcon(
    icon: ImageVector,
    miniIcons: List<ImageVector>,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (miniIcons.size > 1) 120.dp else 108.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(brandingGradient()))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(MintPrimary.copy(0.4f), SkyPrimary.copy(0.4f))),
                    shape = RoundedCornerShape(26.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.White,
            )
        }
        if (miniIcons.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                miniIcons.forEach { mini ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                RoundedCornerShape(12.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = mini,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureGuideBulletRow(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(MintPrimary, SkyPrimary))),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun FeatureGuidePageDots(
    count: Int,
    selected: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val active = index == selected
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (active) 10.dp else 7.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) {
                            Brush.linearGradient(brandingGradient())
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                ),
                            )
                        },
                    ),
            )
        }
    }
}
