package com.example.healtapp.features.aicoach.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.ScreenHeader
import com.example.healtapp.core.ui.components.AppMessageBanner
import com.example.healtapp.core.ui.components.AppMessageType
import com.example.healtapp.core.ui.theme.screenBackgroundGradient
import com.example.healtapp.features.aicoach.presentation.AiAssistantViewModel
import com.example.healtapp.features.aicoach.ui.components.AiChatBubble
import com.example.healtapp.features.aicoach.ui.components.AiComposerBar
import com.example.healtapp.features.aicoach.ui.components.AiMetricsStrip
import com.example.healtapp.features.aicoach.ui.components.AiTypingBubble

@Composable
fun AiAssistantScreen(
    onBack: () -> Unit = {},
) {
    val viewModel: AiAssistantViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size, uiState.isLoading) {
        val last = uiState.messages.lastIndex.coerceAtLeast(0)
        val target = if (uiState.isLoading) last + 1 else last
        if (target >= 0) {
            listState.animateScrollToItem(target)
        }
    }

    val gradient = screenBackgroundGradient()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradient))
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ScreenHeader(
                    title = "AI-советник",
                    subtitle = "Персональные ответы по вашему дневнику",
                    icon = Icons.Filled.AutoAwesome,
                    onBackClick = onBack,
                )

                if (uiState.metricChips.isNotEmpty()) {
                    AiMetricsStrip(
                        chips = uiState.metricChips,
                        hint = uiState.contextHint,
                    )
                } else if (uiState.isGuestMode) {
                    Text(
                        uiState.contextHint.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.messages, key = { it.id }) { msg ->
                    AiChatBubble(msg)
                }
                if (uiState.isLoading) {
                    item { AiTypingBubble() }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                uiState.error?.let {
                    AppMessageBanner(text = it, type = AppMessageType.Error)
                }
                AiComposerBar(
                    value = uiState.input,
                    onValueChange = viewModel::updateInput,
                    onSend = viewModel::sendMessage,
                    enabled = !uiState.isLoading && !uiState.isGuestMode,
                )
            }
        }
    }
}
