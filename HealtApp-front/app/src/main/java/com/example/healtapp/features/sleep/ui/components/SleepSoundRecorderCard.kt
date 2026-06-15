package com.example.healtapp.features.sleep.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.animation.AppMotion
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.theme.MintPrimary
import com.example.healtapp.core.ui.theme.SkyPrimary
import com.example.healtapp.core.ui.theme.brandingGradient
import com.example.healtapp.core.ui.theme.cardHeaderGradient
import com.example.healtapp.core.ui.theme.themedCardMint
import com.example.healtapp.features.sleep.presentation.SleepSoundClipUi
import com.example.healtapp.features.sleep.presentation.SleepSoundDayGroupUi
import java.time.LocalDate

@Composable
fun SleepSoundRecorderCard(
    isTracking: Boolean,
    clipsThisSession: Int,
    isRecordingClip: Boolean,
    sessionClips: List<SleepSoundClipUi>,
    lastSessionClips: List<SleepSoundClipUi>,
    clipGroups: List<SleepSoundDayGroupUi>,
    canPlayback: Boolean,
    playingClipId: String?,
    showPlaybackHint: Boolean = false,
    aiSummary: String?,
    isGeneratingSummary: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onPlayClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
) {
    val todayKey = LocalDate.now().toString()

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(cardHeaderGradient(themedCardMint(), 0.45f))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Mic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Запись звуков сна",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Фиксируем активность. История хранится $RETENTION_DAYS_LABEL по дням.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (isTracking) {
                TrackingStatusRow(
                    clipsThisSession = clipsThisSession,
                    isRecordingClip = isRecordingClip,
                )
                AppButton(
                    text = "Остановить отслеживание",
                    onClick = onStopClick,
                    isSecondary = true,
                )
                if (sessionClips.isNotEmpty()) {
                    Text(
                        text = "Фрагменты сессии",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    sessionClips.take(6).forEach { clip ->
                        SleepSoundClipRow(
                            clip = clip,
                            isPlaying = false,
                            canPlayback = false,
                            onPlayClick = {},
                            onDeleteClick = { onDeleteClick(clip.id) },
                        )
                    }
                    Text(
                        text = "Прослушивание будет доступно после остановки отслеживания.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                AppButton(
                    text = "Начать отслеживание",
                    onClick = onStartClick,
                )
                if (showPlaybackHint && lastSessionClips.isNotEmpty()) {
                    PlaybackHintBanner()
                }
            }

            if (isGeneratingSummary) {
                Text(
                    text = "Анализируем звуки сна...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else if (aiSummary != null) {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MintPrimary)
                            Text(
                                "Умный анализ",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                        Text(
                            text = aiSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            val historyGroups = if (!isTracking && lastSessionClips.isNotEmpty()) {
                val sessionIds = lastSessionClips.map { it.id }.toSet()
                clipGroups.mapNotNull { group ->
                    val filtered = group.clips.filterNot { it.id in sessionIds }
                    if (filtered.isEmpty()) null else group.copy(clips = filtered)
                }
            } else {
                clipGroups
            }

            if (!isTracking && lastSessionClips.isNotEmpty()) {
                Text(
                    text = "Записи последней сессии",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                lastSessionClips.forEach { clip ->
                    SleepSoundClipRow(
                        clip = clip,
                        isPlaying = playingClipId == clip.id,
                        canPlayback = canPlayback,
                        onPlayClick = { onPlayClick(clip.id) },
                        onDeleteClick = { onDeleteClick(clip.id) },
                    )
                }
                if (historyGroups.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }
            }

            if (historyGroups.isNotEmpty()) {
                Text(
                    text = "История по дням",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                historyGroups.forEach { group ->
                    SleepSoundDayGroupSection(
                        group = group,
                        todayKey = todayKey,
                        playingClipId = playingClipId,
                        canPlayback = canPlayback && !isTracking,
                        onPlayClick = onPlayClick,
                        onDeleteClick = onDeleteClick,
                    )
                }
            }
        }
    }
}

private const val RETENTION_DAYS_LABEL = "14 дней"

@Composable
private fun PlaybackHintBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        MintPrimary.copy(alpha = 0.14f),
                        SkyPrimary.copy(alpha = 0.14f),
                    ),
                ),
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Нажмите ▶ у фрагмента, чтобы прослушать запись.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SleepSoundDayGroupSection(
    group: SleepSoundDayGroupUi,
    todayKey: String,
    playingClipId: String?,
    canPlayback: Boolean,
    onPlayClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
) {
    var expanded by rememberSaveable(group.dateKey) {
        mutableStateOf(group.dateKey == todayKey)
    }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = AppMotion.springGentle(),
        label = "sleepDayChevron",
    )
    val interaction = MutableInteractionSource()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = interaction, indication = null) {
                    expanded = !expanded
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.dayLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = pluralFragments(group.clips.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Свернуть" else "Развернуть",
                modifier = Modifier.rotate(chevronRotation),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                group.clips.forEach { clip ->
                    SleepSoundClipRow(
                        clip = clip,
                        isPlaying = playingClipId == clip.id,
                        canPlayback = canPlayback,
                        onPlayClick = { onPlayClick(clip.id) },
                        onDeleteClick = { onDeleteClick(clip.id) },
                    )
                }
            }
        }
    }
}

private fun pluralFragments(count: Int): String {
    val mod10 = count % 10
    val mod100 = count % 100
    val word = when {
        mod100 in 11..14 -> "фрагментов"
        mod10 == 1 -> "фрагмент"
        mod10 in 2..4 -> "фрагмента"
        else -> "фрагментов"
    }
    return "$count $word"
}

@Composable
private fun TrackingStatusRow(
    clipsThisSession: Int,
    isRecordingClip: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        MintPrimary.copy(alpha = 0.12f),
                        SkyPrimary.copy(alpha = 0.12f),
                    ),
                ),
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(
                    if (isRecordingClip) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                ),
        )
        Icon(
            Icons.Filled.GraphicEq,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isRecordingClip) "Запись фрагмента…" else "Слушаем комнату",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = if (clipsThisSession > 0) {
                    "Зафиксировано: $clipsThisSession"
                } else {
                    "Пока тихо — ждём активность"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SleepSoundClipRow(
    clip: SleepSoundClipUi,
    isPlaying: Boolean,
    canPlayback: Boolean,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(brandingGradient())),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${clip.timeLabel} · ${clip.label}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = clip.durationLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(
            onClick = onPlayClick,
            enabled = canPlayback,
        ) {
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = when {
                    !canPlayback -> "Доступно после остановки"
                    isPlaying -> "Пауза"
                    else -> "Воспроизвести"
                },
                tint = if (canPlayback) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                },
            )
        }
        TextButton(onClick = onDeleteClick) {
            Text("Удалить", color = MaterialTheme.colorScheme.error)
        }
    }
}
