package com.example.healtapp.features.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.core.common.AppRefreshBus
import com.example.healtapp.domain.repository.ImportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ImportUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val importRepository: ImportRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    fun importCsvText(text: String) {
        viewModelScope.launch {
            _uiState.value = ImportUiState(isLoading = true)
            importRepository.importCsv(text)
                .onSuccess { res ->
                    val parts = buildList {
                        add("Импортировано: вода ${res.hydration_created}, еда ${res.meals_created}, сон ${res.sleeps_created}, активность ${res.activities_created}")
                        if (res.errors.isNotEmpty()) {
                            add("Предупреждения: ${res.errors.take(5).joinToString("; ")}")
                        }
                    }
                    _uiState.value = ImportUiState(
                        isLoading = false,
                        message = parts.joinToString("\n"),
                    )
                    AppRefreshBus.notifyDataChanged()
                }
                .onFailure { e ->
                    _uiState.value = ImportUiState(
                        isLoading = false,
                        error = e.message ?: "Ошибка импорта",
                    )
                }
        }
    }
}
