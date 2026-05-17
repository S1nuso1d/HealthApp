package com.example.healtapp.features.aicoach.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.core.ui.theme.brandingGradient
import com.example.healtapp.features.aicoach.presentation.AiAssistantViewModel
import com.example.healtapp.features.aicoach.presentation.ChatMessageUi

@Composable
fun AiAssistantScreen(
    onBack: () -> Unit = {},
) {
    val viewModel: AiAssistantViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    AppScreen(
        title = "AI-советник",
        subtitle = "Ответы на основе вашего дневника",
        headerIcon = Icons.Filled.AutoAwesome,
        onNavigateBack = onBack,
        scrollable = false,
    ) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            uiState.weeklyBrief?.let { brief ->
                AppCard {
                    Text("Недельная сводка", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(brief, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Как спал?", "Вода за неделю", "Что улучшить?").forEach { q ->
                    AppCard(modifier = Modifier.weight(1f), onClick = { viewModel.askQuick(q) }) {
                        Text(q, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.messages, key = { it.id }) { msg ->
                    ChatBubble(msg)
                }
                if (uiState.isLoading) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            CircularProgressIndicator(Modifier.padding(8.dp))
                        }
                    }
                }
            }

            uiState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppTextField(
                    value = uiState.input,
                    onValueChange = viewModel::updateInput,
                    label = "Ваш вопрос",
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = viewModel::sendMessage,
                    enabled = !uiState.isLoading && uiState.input.isNotBlank(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessageUi) {
    val align = if (message.isUser) Alignment.End else Alignment.Start
    val colors = if (message.isUser) {
        Brush.linearGradient(brandingGradient())
    } else {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surface,
            ),
        )
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(18.dp))
                .background(colors)
                .padding(12.dp),
            horizontalAlignment = align,
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
