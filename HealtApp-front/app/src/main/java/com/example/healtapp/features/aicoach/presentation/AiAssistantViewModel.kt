package com.example.healtapp.features.aicoach.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.domain.repository.WellnessRepository
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
)

data class AiAssistantUiState(
    val messages: List<ChatMessageUi> = emptyList(),
    val input: String = "",
    val isLoading: Boolean = false,
    val weeklyBrief: String? = null,
    val error: String? = null,
)

@HiltViewModel
class AiAssistantViewModel @Inject constructor(
    private val wellnessRepository: WellnessRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiAssistantUiState())
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()
    private var messageId = 0L

    init {
        loadWeeklyBrief()
        addBotMessage(
            "Привет! Я ваш AI-советник HealthApp. Спросите о сне, питании, воде или активности — отвечу на основе ваших данных.",
        )
    }

    fun updateInput(value: String) {
        _uiState.update { it.copy(input = value, error = null) }
    }

    fun sendMessage() {
        val question = _uiState.value.input.trim()
        if (question.isBlank() || _uiState.value.isLoading) return
        addUserMessage(question)
        _uiState.update { it.copy(input = "", isLoading = true, error = null) }
        viewModelScope.launch {
            wellnessRepository.aiChat(question).onSuccess { response ->
                addBotMessage(response.answer)
                _uiState.update { it.copy(isLoading = false) }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Не удалось получить ответ")
                }
            }
        }
    }

    fun askQuick(prompt: String) {
        _uiState.update { it.copy(input = prompt) }
        sendMessage()
    }

    fun loadWeeklyBrief() {
        viewModelScope.launch {
            wellnessRepository.weeklyBrief(7).onSuccess { brief ->
                _uiState.update {
                    it.copy(weeklyBrief = "${brief.title}\n\n${brief.summary}")
                }
            }
        }
    }

    private fun addUserMessage(text: String) {
        messageId++
        _uiState.update { it.copy(messages = it.messages + ChatMessageUi(messageId, true, text)) }
    }

    private fun addBotMessage(text: String) {
        messageId++
        _uiState.update { it.copy(messages = it.messages + ChatMessageUi(messageId, false, text)) }
    }
}
