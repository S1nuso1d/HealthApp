package com.example.healtapp.features.aicoach.presentation

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.data.network.dto.wellness.ChatHistoryMessageDto
import com.example.healtapp.data.preferences.AiChatHistoryStore
import com.example.healtapp.data.preferences.StoredChatMessage
import com.example.healtapp.data.preferences.TokenStorage
import com.example.healtapp.domain.repository.AiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatMessageUi(
    val id: Long,
    val isUser: Boolean,
    val text: String,
    val apiText: String? = null,
)

data class ChatHistorySessionUi(
    val id: String,
    val title: String,
    val isCurrent: Boolean = false,
    val entries: List<ChatHistoryEntryUi>,
)

data class ChatHistoryEntryUi(
    val isUser: Boolean,
    val text: String,
)

data class AiAssistantUiState(
    val messages: List<ChatMessageUi> = emptyList(),
    val input: String = "",
    val isLoading: Boolean = false,
    val isGuestMode: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    val contextReady: Boolean = false,
    val llmAvailable: Boolean? = null,
    val llmStatusMessage: String? = null,
    val showHistorySheet: Boolean = false,
    val historySessions: List<ChatHistorySessionUi> = emptyList(),
)

val AiSuggestedPrompts = listOf(
    "Что улучшить сегодня до вечера?",
    "Почему мало энергии и что сделать?",
    "Краткий обзор всех показателей за неделю",
)

private const val FALLBACK_MARKER = "не удалось связаться с языковой моделью"

@HiltViewModel
class AiAssistantViewModel @Inject constructor(
    private val aiRepository: AiRepository,
    private val tokenStorage: TokenStorage,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val chatHistoryStore = AiChatHistoryStore(appContext)

    private val _uiState = MutableStateFlow(AiAssistantUiState())
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()
    private var messageId = 0L

    init {
        viewModelScope.launch {
            if (tokenStorage.isGuestMode()) {
                _uiState.value = AiAssistantUiState(
                    isGuestMode = true,
                    contextReady = true,
                )
                addBotMessage(
                    "Войдите в аккаунт — тогда я увижу ваш дневник (сон, воду, питание, шаги, настроение) и смогу отвечать персонально.",
                )
            } else {
                val saved = chatHistoryStore.load()
                if (!saved.isNullOrEmpty()) {
                    messageId = saved.maxOf { it.id }
                    _uiState.value = AiAssistantUiState(
                        contextReady = true,
                        messages = saved.map { ChatMessageUi(it.id, it.isUser, it.text) },
                    )
                } else {
                    _uiState.value = AiAssistantUiState(contextReady = true)
                    addBotMessage(
                        "Здравствуйте! Я ваш ИИ помощник HealthApp. Вижу данные из дневника и отвечаю на вопросы о здоровье, сне, питании и активности. Чем помочь?",
                    )
                }
                refreshLlmStatus()
            }
        }
    }

    fun refreshLlmStatus() {
        if (_uiState.value.isGuestMode) return
        viewModelScope.launch {
            if (_uiState.value.llmAvailable != true) {
                _uiState.update { it.copy(llmAvailable = null) }
            }
            aiRepository.getAiStatus()
                .onSuccess { status ->
                    _uiState.update {
                        it.copy(
                            llmAvailable = status.llm_available,
                            llmStatusMessage = status.message,
                            info = if (!status.llm_available) {
                                "На сервере ИИ недоступен: ${status.message}"
                            } else {
                                null
                            },
                            error = null,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { state ->
                        state.copy(llmStatusMessage = e.message)
                    }
                }
        }
    }

    fun updateInput(value: String) {
        _uiState.update { it.copy(input = value, error = null) }
    }

    fun sendMessage(text: String? = null, userDisplayText: String? = null) {
        val question = (text ?: _uiState.value.input).trim()
        if (question.isBlank() || _uiState.value.isLoading || _uiState.value.isGuestMode) return

        val shown = (userDisplayText ?: question).trim()
        addUserMessage(shown, apiText = if (userDisplayText != null) question else null)
        _uiState.update { it.copy(input = "", isLoading = true, error = null) }

        val history = _uiState.value.messages
            .dropLast(1)
            .takeLast(20)
            .map { msg ->
                ChatHistoryMessageDto(
                    role = if (msg.isUser) "user" else "assistant",
                    content = msg.apiText ?: msg.text,
                )
            }

        viewModelScope.launch {
            val request = com.example.healtapp.data.network.dto.wellness.AIChatRequestDto(
                question = question,
                periodDays = 14,
                history = history,
            )
            aiRepository.streamChat(request)
                .onSuccess { flow ->
                    _uiState.update { it.copy(isLoading = false) }
                    messageId++
                    val currentMessageId = messageId
                    _uiState.update { it.copy(messages = it.messages + ChatMessageUi(currentMessageId, false, "")) }

                    try {
                        flow.collect { chunk ->
                            _uiState.update { state ->
                                val updatedMessages = state.messages.map { msg ->
                                    if (msg.id == currentMessageId) {
                                        msg.copy(text = msg.text + chunk)
                                    } else {
                                        msg
                                    }
                                }
                                val lastText = updatedMessages.lastOrNull()?.text.orEmpty()
                                val usedFallback = lastText.contains(FALLBACK_MARKER, ignoreCase = true)
                                val fallbackInfo = if (usedFallback) {
                                    "Ответ сформирован без ИИ — по данным дневника. Запустите нейросеть на сервере для полноценного диалога."
                                } else {
                                    state.info
                                }
                                state.copy(
                                    messages = updatedMessages,
                                    info = fallbackInfo,
                                    llmAvailable = if (usedFallback) false else state.llmAvailable,
                                )
                            }
                        }
                        _uiState.update { state ->
                            val lastText = state.messages.lastOrNull()?.text.orEmpty()
                            if (!lastText.contains(FALLBACK_MARKER, ignoreCase = true) && lastText.isNotBlank()) {
                                state.copy(llmAvailable = true, info = null)
                            } else {
                                state
                            }
                        }
                        persistMessages()
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                error = e.message?.takeIf { m -> m.isNotBlank() }
                                    ?: "Ошибка потока ответа от сервера.",
                            )
                        }
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message?.takeIf { m -> m.isNotBlank() }
                                ?: "Не удалось получить ответ от сервера. Проверьте, что бэкенд запущен и ИИ включён.",
                        )
                    }
                }
        }
    }

    fun sendSuggestedPrompt(prompt: String) {
        sendMessage(prompt)
    }

    fun sendTopicAnalysis(topic: AiHealthTopic) {
        sendMessage(
            text = topic.analysisPrompt,
            userDisplayText = "Разбор: ${topic.label}",
        )
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun openHistorySheet() {
        if (_uiState.value.isGuestMode) return
        viewModelScope.launch {
            val sessions = buildHistorySessions()
            _uiState.update { it.copy(showHistorySheet = true, historySessions = sessions) }
        }
    }

    fun closeHistorySheet() {
        _uiState.update { it.copy(showHistorySheet = false) }
    }

    fun clearChat() {
        viewModelScope.launch {
            val toArchive = _uiState.value.messages
            if (toArchive.any { it.isUser }) {
                chatHistoryStore.archiveSession(
                    toArchive.map { StoredChatMessage(id = it.id, isUser = it.isUser, text = it.text) },
                )
            }
            messageId = 0L
            chatHistoryStore.clear()
            if (_uiState.value.isGuestMode) {
                _uiState.value = AiAssistantUiState(isGuestMode = true, contextReady = true)
                addBotMessage(
                    "Войдите в аккаунт — тогда я увижу ваш дневник (сон, воду, питание, шаги, настроение) и смогу отвечать персонально.",
                )
            } else {
                val info = _uiState.value.info
                val llmAvailable = _uiState.value.llmAvailable
                val llmStatus = _uiState.value.llmStatusMessage
                _uiState.value = AiAssistantUiState(
                    contextReady = true,
                    info = info,
                    llmAvailable = llmAvailable,
                    llmStatusMessage = llmStatus,
                )
                addBotMessage(
                    "Новый диалог. Я ваш ИИ помощник HealthApp — выберите тему разбора или задайте свой вопрос.",
                )
            }
        }
    }

    private suspend fun buildHistorySessions(): List<ChatHistorySessionUi> {
        val archived = chatHistoryStore.loadArchivedSessions().map { session ->
            ChatHistorySessionUi(
                id = session.id,
                title = AiChatHistoryStore.sessionTitle(session.savedAt, session.messages),
                entries = session.messages.map { ChatHistoryEntryUi(it.isUser, it.text) },
            )
        }.toMutableList()

        val current = _uiState.value.messages.dropWhile { !it.isUser }
        if (current.any { it.isUser }) {
            archived.add(
                0,
                ChatHistorySessionUi(
                    id = "current",
                    title = "Текущий диалог",
                    isCurrent = true,
                    entries = current.map { ChatHistoryEntryUi(it.isUser, it.text) },
                ),
            )
        }
        return archived
    }

    private fun addUserMessage(text: String, apiText: String? = null) {
        messageId++
        _uiState.update {
            it.copy(messages = it.messages + ChatMessageUi(messageId, true, text, apiText))
        }
        persistMessages()
    }

    private fun addBotMessage(text: String) {
        messageId++
        _uiState.update { it.copy(messages = it.messages + ChatMessageUi(messageId, false, text)) }
        persistMessages()
    }

    private fun persistMessages() {
        if (_uiState.value.isGuestMode) return
        val snapshot = _uiState.value.messages.map {
            StoredChatMessage(id = it.id, isUser = it.isUser, text = it.text)
        }
        viewModelScope.launch { chatHistoryStore.save(snapshot) }
    }
}
