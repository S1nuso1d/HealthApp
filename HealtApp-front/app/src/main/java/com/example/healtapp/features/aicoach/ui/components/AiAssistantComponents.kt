package com.example.healtapp.features.aicoach.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.FeatureHeroBar
import com.example.healtapp.core.ui.components.AppFormMetrics
import com.example.healtapp.core.ui.theme.MintPrimary
import com.example.healtapp.core.ui.theme.SkyPrimary
import com.example.healtapp.core.ui.theme.SuccessColor
import com.example.healtapp.core.ui.theme.WarningColor
import com.example.healtapp.core.ui.theme.brandingGradient
import com.example.healtapp.core.ui.theme.contentPrimaryColor
import com.example.healtapp.core.ui.theme.contentSecondaryColor
import com.example.healtapp.core.ui.theme.heroContentColor
import com.example.healtapp.core.ui.theme.iconBadgeGradient
import com.example.healtapp.core.ui.theme.isAppDarkTheme
import com.example.healtapp.core.ui.theme.subtleFillGradient
import com.example.healtapp.core.ui.theme.themedCardBlue
import com.example.healtapp.core.ui.theme.themedCardLavender
import com.example.healtapp.core.ui.theme.themedCardMint
import com.example.healtapp.core.ui.theme.metricIconGradient
import com.example.healtapp.features.aicoach.presentation.AiHealthTopic
import com.example.healtapp.features.aicoach.presentation.AiHealthTopics
import com.example.healtapp.features.aicoach.presentation.ChatHistoryEntryUi
import com.example.healtapp.features.aicoach.presentation.ChatHistorySessionUi
import com.example.healtapp.features.aicoach.presentation.ChatMessageUi

@Composable
fun AiCoachHeroBar(
    onBack: () -> Unit,
    llmAvailable: Boolean?,
    isGuestMode: Boolean,
    onRefreshStatus: () -> Unit,
    onNewChat: () -> Unit,
    onOpenHistory: () -> Unit,
    showNewChat: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    FeatureHeroBar(
        title = "ИИ помощник",
        subtitle = if (isGuestMode) {
            "Войдите, чтобы видеть ваш дневник"
        } else {
            ""
        },
        icon = Icons.Filled.AutoAwesome,
        onBack = onBack,
        modifier = modifier,
        actions = {
            if (!isGuestMode) {
                IconButton(onClick = onOpenHistory, enabled = enabled) {
                    Icon(
                        Icons.Outlined.History,
                        contentDescription = "История",
                        tint = heroContentColor(),
                    )
                }
            }
            if (!isGuestMode && llmAvailable == false) {
                IconButton(onClick = onRefreshStatus, enabled = enabled) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "Проверить ИИ",
                        tint = heroContentColor(),
                    )
                }
            }
            if (showNewChat) {
                IconButton(onClick = onNewChat, enabled = enabled) {
                    Icon(
                        Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Новый диалог",
                        tint = heroContentColor(),
                    )
                }
            }
        },
        footer = if (!isGuestMode) {
            {
                androidx.compose.foundation.layout.Spacer(Modifier.height(10.dp))
                AiLlmStatusChip(llmAvailable = llmAvailable)
            }
        } else {
            null
        },
    )
}

@Composable
private fun AiLlmStatusChip(llmAvailable: Boolean?) {
    val (dotColor, label) = when (llmAvailable) {
        true -> SuccessColor to "Нейросеть онлайн"
        false -> WarningColor to "ИИ недоступен на сервере"
        null -> heroContentColor().copy(alpha = 0.6f) to "Проверяем ИИ…"
    }
    Row(
        modifier = Modifier
            .padding(start = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = if (isAppDarkTheme()) 0.12f else 0.2f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = heroContentColor().copy(alpha = 0.95f),
        )
    }
}

@Composable
fun AiWelcomePanel(
    topics: List<AiHealthTopic> = AiHealthTopics,
    onTopicClick: (AiHealthTopic) -> Unit,
    prompts: List<String>,
    onPromptClick: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Чем помочь?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Выберите тему — разберу ваши данные, что мешает и какие шаги помогут улучшить показатели.",
                style = MaterialTheme.typography.bodyMedium,
                color = contentSecondaryColor(),
            )
        }

        Text(
            text = "Разбор по темам",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentSecondaryColor(),
        )
        AiTopicFocusGrid(
            topics = topics,
            onTopicClick = onTopicClick,
            enabled = enabled,
        )

        Text(
            text = "Быстрые вопросы",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentSecondaryColor(),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            prompts.forEach { prompt ->
                AiPromptCard(
                    text = prompt,
                    onClick = { if (enabled) onPromptClick(prompt) },
                    enabled = enabled,
                )
            }
        }
    }
}

@Composable
private fun AiTopicFocusGrid(
    topics: List<AiHealthTopic>,
    onTopicClick: (AiHealthTopic) -> Unit,
    enabled: Boolean,
) {
    val gradients = listOf(
        metricIconGradient(themedCardBlue()),
        metricIconGradient(themedCardMint()),
        metricIconGradient(themedCardLavender()),
        metricIconGradient(themedCardBlue(), mintTint = true),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        topics.chunked(2).forEachIndexed { rowIndex, rowTopics ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowTopics.forEachIndexed { colIndex, topic ->
                    val gradient = gradients[(rowIndex * 2 + colIndex) % gradients.size]
                    AiTopicFocusCard(
                        topic = topic,
                        iconGradient = gradient,
                        onClick = { if (enabled) onTopicClick(topic) },
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowTopics.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AiTopicFocusCard(
    topic: AiHealthTopic,
    iconGradient: List<Color>,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.55f)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                RoundedCornerShape(22.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(iconGradient)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                topic.icon,
                contentDescription = topic.label,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = topic.label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentPrimaryColor(),
        )
    }
}

@Composable
private fun AiPromptCard(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.55f)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                RoundedCornerShape(20.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(iconBadgeGradient())),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = contentPrimaryColor(),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun AiAssistantMessage(
    message: ChatMessageUi,
    modifier: Modifier = Modifier,
) {
    if (message.isUser) {
        AiUserBubble(message.text, modifier)
    } else if (message.text.isBlank()) {
        AiThinkingBubble(modifier)
    } else {
        AiAssistantBubble(message.text, modifier)
    }
}

@Composable
fun AiUserBubble(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp))
                .background(Brush.linearGradient(brandingGradient()))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
            )
        }
    }
}

@Composable
fun AiAssistantBubble(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        AiCoachAvatar()
        Column(
            modifier = Modifier
                .padding(start = 10.dp)
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(6.dp, 22.dp, 22.dp, 22.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                    RoundedCornerShape(6.dp, 22.dp, 22.dp, 22.dp),
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = "ИИ помощник",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MintPrimary,
            )
            Spacer(Modifier.height(4.dp))
            AiFormattedText(text = text)
        }
    }
}

@Composable
fun AiThinkingBubble(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AiCoachAvatar()
        Surface(
            modifier = Modifier.padding(start = 10.dp),
            shape = RoundedCornerShape(6.dp, 22.dp, 22.dp, 22.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AiTypingDots()
                Text(
                    text = "Думаю…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentSecondaryColor(),
                )
            }
        }
    }
}

@Composable
private fun AiCoachAvatar() {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(brandingGradient())),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun AiTypingDots() {
    val transition = rememberInfiniteTransition(label = "aiTyping")
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, delayMillis = index * 140, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot$index",
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .alpha(alpha)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(MintPrimary, SkyPrimary)),
                    ),
            )
        }
    }
}

@Composable
fun AiInlineNotice(
    text: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    val bg = if (isError) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    }
    val fg = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .then(if (onDismiss != null) Modifier.clickable(onClick = onDismiss) else Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = fg,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun AiGuestPanel(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(subtleFillGradient()))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                RoundedCornerShape(24.dp),
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Нужен аккаунт",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Войдите или зарегистрируйтесь — тогда ИИ помощник увидит сон, воду, питание и шаги из вашего дневника.",
            style = MaterialTheme.typography.bodyMedium,
            color = contentSecondaryColor(),
        )
    }
}

@Composable
fun AiComposerDock(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(26.dp)
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = if (isAppDarkTheme()) 0.22f else 0.1f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.97f))
            .border(1.dp, borderColor, shape)
            .padding(start = 18.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 10.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = contentPrimaryColor()),
            cursorBrush = SolidColor(MintPrimary),
            maxLines = 6,
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        "Спросите о сне, воде, питании или шагах…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentSecondaryColor(),
                    )
                }
                inner()
            },
        )
        IconButton(
            onClick = onSend,
            enabled = enabled && value.isNotBlank(),
            modifier = Modifier
                .size(AppFormMetrics.ControlHeight)
                .clip(RoundedCornerShape(AppFormMetrics.FieldCornerRadius))
                .background(
                    if (enabled && value.isNotBlank()) {
                        Brush.linearGradient(brandingGradient())
                    } else {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        )
                    },
                ),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Отправить",
                tint = if (enabled && value.isNotBlank()) Color.White else contentSecondaryColor(),
            )
        }
    }
}

@Composable
fun AiChatHistorySheet(
    sessions: List<ChatHistorySessionUi>,
    modifier: Modifier = Modifier,
) {
    if (sessions.isEmpty()) {
        AppCard(modifier = modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "История пока пуста",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Здесь появятся ваши вопросы и рекомендации ИИ. При «Новом диалоге» прошлая переписка сохраняется в историю.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(sessions, key = { it.id }) { session ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentPrimaryColor(),
                )
                session.entries.forEach { entry ->
                    AiHistoryEntryCard(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun AiHistoryEntryCard(entry: ChatHistoryEntryUi) {
    val label = if (entry.isUser) "Ваш вопрос" else "Рекомендация ИИ"
    val gradient = if (entry.isUser) {
        Brush.linearGradient(listOf(themedCardMint(), themedCardBlue()))
    } else {
        Brush.linearGradient(listOf(themedCardLavender(), themedCardBlue()))
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .background(gradient)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            if (entry.isUser) {
                Text(
                    text = entry.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentPrimaryColor(),
                )
            } else {
                AiFormattedText(text = entry.text)
            }
        }
    }
}
