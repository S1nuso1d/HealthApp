package com.example.healtapp.features.aicoach.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.theme.screenBackgroundGradient
import com.example.healtapp.features.aicoach.presentation.AiAssistantViewModel
import com.example.healtapp.features.aicoach.presentation.AiSuggestedPrompts
import com.example.healtapp.features.aicoach.ui.components.AiAssistantMessage
import com.example.healtapp.features.aicoach.ui.components.AiChatHistorySheet
import com.example.healtapp.features.aicoach.ui.components.AiCoachHeroBar
import com.example.healtapp.features.aicoach.ui.components.AiComposerDock
import com.example.healtapp.features.aicoach.ui.components.AiGuestPanel
import com.example.healtapp.features.aicoach.ui.components.AiInlineNotice
import com.example.healtapp.features.aicoach.ui.components.AiThinkingBubble
import com.example.healtapp.features.aicoach.ui.components.AiWelcomePanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    onBack: () -> Unit = {},
) {
    val viewModel: AiAssistantViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.refreshLlmStatus()
    }

    val hasUserMessages = uiState.messages.any { it.isUser }
    val showWelcome = !uiState.isGuestMode && !hasUserMessages && !uiState.isLoading
    val chatMessages = if (showWelcome) emptyList() else uiState.messages

    LaunchedEffect(chatMessages.size, uiState.isLoading) {
        val extra = when {
            uiState.isLoading && chatMessages.lastOrNull()?.isUser == true -> 1
            showWelcome -> 1
            else -> 0
        }
        val target = chatMessages.size + extra - 1
        if (target >= 0) {
            listState.animateScrollToItem(target)
        }
    }

    val composerBottomPadding = if (!uiState.isGuestMode) 88.dp else 16.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(screenBackgroundGradient()))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AiCoachHeroBar(
                onBack = onBack,
                llmAvailable = uiState.llmAvailable,
                isGuestMode = uiState.isGuestMode,
                onRefreshStatus = viewModel::refreshLlmStatus,
                onNewChat = viewModel::clearChat,
                onOpenHistory = viewModel::openHistorySheet,
                showNewChat = hasUserMessages && !uiState.isGuestMode,
                enabled = !uiState.isLoading,
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 16.dp,
                    bottom = composerBottomPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                uiState.info?.let { info ->
                    item(key = "info") {
                        AiInlineNotice(text = info, isError = false)
                    }
                }

                if (uiState.isGuestMode) {
                    item(key = "guest") {
                        AiGuestPanel()
                    }
                }

                if (showWelcome) {
                    item(key = "welcome") {
                        AiWelcomePanel(
                            onTopicClick = viewModel::sendTopicAnalysis,
                            prompts = AiSuggestedPrompts,
                            onPromptClick = viewModel::sendSuggestedPrompt,
                            enabled = !uiState.isLoading,
                        )
                    }
                }

                items(chatMessages, key = { it.id }) { message ->
                    AiAssistantMessage(message = message)
                }

                if (uiState.isLoading && chatMessages.lastOrNull()?.isUser == true) {
                    item(key = "typing") {
                        AiThinkingBubble()
                    }
                }
            }
        }

        if (!uiState.isGuestMode) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                uiState.error?.let { err ->
                    AiInlineNotice(
                        text = err,
                        isError = true,
                        onDismiss = viewModel::clearError,
                    )
                }

                AiComposerDock(
                    value = uiState.input,
                    onValueChange = viewModel::updateInput,
                    onSend = { viewModel.sendMessage() },
                    enabled = !uiState.isLoading,
                )
            }
        }
    }

    if (uiState.showHistorySheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = viewModel::closeHistorySheet,
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                androidx.compose.material3.Text(
                    text = "История диалогов",
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                )
                AiChatHistorySheet(
                    sessions = uiState.historySessions,
                    modifier = Modifier.heightIn(max = 520.dp),
                )
            }
        }
    }
}
