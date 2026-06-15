package com.example.healtapp.features.cycle.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.data.network.dto.cycle.CycleEntryCreateDto
import com.example.healtapp.data.network.dto.cycle.CycleEntryDto
import com.example.healtapp.data.network.dto.cycle.CycleEntryUpdateDto
import com.example.healtapp.domain.repository.CycleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class CycleUiState(
    val isLoading: Boolean = false,
    val entries: List<CycleEntryDto> = emptyList(),
    val error: String? = null,
    val isSaving: Boolean = false
)

@HiltViewModel
class CycleViewModel @Inject constructor(
    private val repository: CycleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CycleUiState())
    val uiState: StateFlow<CycleUiState> = _uiState.asStateFlow()

    init {
        loadEntries()
    }

    fun loadEntries() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getEntries().fold(
                onSuccess = { entries ->
                    _uiState.update { it.copy(isLoading = false, entries = entries) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    fun addEntry(startDate: LocalDate, endDate: LocalDate?, symptoms: String?, notes: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val request = CycleEntryCreateDto(
                start_date = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                end_date = endDate?.format(DateTimeFormatter.ISO_LOCAL_DATE),
                symptoms = symptoms,
                notes = notes
            )
            repository.createEntry(request).fold(
                onSuccess = {
                    loadEntries()
                    _uiState.update { it.copy(isSaving = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSaving = false, error = e.message) }
                }
            )
        }
    }

    fun updateEntry(id: Int, startDate: LocalDate?, endDate: LocalDate?, symptoms: String?, notes: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val request = CycleEntryUpdateDto(
                start_date = startDate?.format(DateTimeFormatter.ISO_LOCAL_DATE),
                end_date = endDate?.format(DateTimeFormatter.ISO_LOCAL_DATE),
                symptoms = symptoms,
                notes = notes
            )
            repository.updateEntry(id, request).fold(
                onSuccess = {
                    loadEntries()
                    _uiState.update { it.copy(isSaving = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSaving = false, error = e.message) }
                }
            )
        }
    }

    fun deleteEntry(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            repository.deleteEntry(id).fold(
                onSuccess = {
                    loadEntries()
                    _uiState.update { it.copy(isSaving = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSaving = false, error = e.message) }
                }
            )
        }
    }
}
