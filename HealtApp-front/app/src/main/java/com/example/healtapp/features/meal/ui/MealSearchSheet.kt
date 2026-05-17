package com.example.healtapp.features.meal.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.features.meal.presentation.MealUiState
import com.example.healtapp.features.meal.ui.components.MealFatSecretHitRow
import com.example.healtapp.features.meal.ui.components.MealServingPicker
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealSearchSheet(
    visible: Boolean,
    mealSlotLabel: String,
    uiState: MealUiState,
    onDismiss: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearchDebounced: () -> Unit,
    onSearchNow: () -> Unit,
    onSelectFood: (String) -> Unit,
    onOpenBarcode: () -> Unit,
    onSelectServing: (Int) -> Unit,
    onMultiplierChange: (Float) -> Unit,
    onAddToDiary: () -> Unit,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(uiState.foodSearchQuery) {
        delay(450)
        onSearchDebounced()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Добавить в $mealSlotLabel",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Найдите продукт в базе или отсканируйте штрихкод",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Закрыть")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppTextField(
                    value = uiState.foodSearchQuery,
                    onValueChange = onQueryChange,
                    label = "Поиск продукта",
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onSearchNow, enabled = !uiState.isFoodSearchLoading) {
                    Icon(Icons.Filled.Search, contentDescription = "Искать")
                }
                IconButton(onClick = onOpenBarcode, enabled = !uiState.isFoodSearchLoading) {
                    Icon(Icons.Filled.QrCode2, contentDescription = "Штрихкод")
                }
            }

            uiState.foodSearchError?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            uiState.error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            if (uiState.isFoodSearchLoading) {
                CircularProgressIndicator(Modifier.size(32.dp).align(Alignment.CenterHorizontally))
            }

            AnimatedVisibility(
                visible = uiState.foodSearchResults.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.foodSearchResults.take(16).forEach { hit ->
                        MealFatSecretHitRow(
                            hit = hit,
                            onClick = { onSelectFood(hit.foodId) },
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState.mealName.isNotBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Выбрано: ${uiState.mealName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (uiState.servingOptions.isNotEmpty()) {
                        MealServingPicker(
                            servings = uiState.servingOptions,
                            selectedIndex = uiState.selectedServingIndex,
                            portionMultiplier = uiState.portionMultiplier,
                            onSelectServing = onSelectServing,
                            onMultiplierChange = onMultiplierChange,
                        )
                    }
                    AppButton(
                        text = if (uiState.isSaving) "Добавляем…" else "Добавить в $mealSlotLabel",
                        onClick = onAddToDiary,
                        enabled = !uiState.isSaving && !uiState.isFoodSearchLoading,
                    )
                }
            }
        }
    }
}
