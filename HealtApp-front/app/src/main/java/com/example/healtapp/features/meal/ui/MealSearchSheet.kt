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
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppMessageBanner
import com.example.healtapp.core.ui.components.AppMessageType
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.data.network.dto.meal.FoodCatalogItemDto
import com.example.healtapp.data.network.dto.meal.SavedDishDto
import com.example.healtapp.features.meal.presentation.MealUiState
import com.example.healtapp.features.meal.ui.components.AddCustomFoodSheet
import com.example.healtapp.features.meal.ui.components.FoodMacroCompletionSheet
import com.example.healtapp.features.meal.ui.components.MealFoodCatalogHitRow
import com.example.healtapp.features.meal.ui.components.MealServingPicker
import com.example.healtapp.features.meal.ui.components.MealSheetHeader
import java.io.File
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealSearchSheet(
    visible: Boolean,
    mealSlotLabel: String,
    uiState: MealUiState,
    savedDishes: List<SavedDishDto> = emptyList(),
    onDismiss: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearchDebounced: () -> Unit,
    onSearchNow: () -> Unit,
    onSelectFood: (FoodCatalogItemDto) -> Unit,
    onSelectSavedDish: (SavedDishDto) -> Unit = {},
    onOpenBarcode: () -> Unit,
    onOpenAddCustomFood: () -> Unit = {},
    onOpenMacroCompletion: () -> Unit = {},
    onSelectServing: (Int) -> Unit,
    onMultiplierChange: (Float) -> Unit,
    onAddToDiary: () -> Unit,
    onDismissMacroCompletion: () -> Unit = {},
    onMacroProteinChange: (String) -> Unit = {},
    onMacroFatChange: (String) -> Unit = {},
    onMacroCarbsChange: (String) -> Unit = {},
    onSaveMacroCompletion: () -> Unit = {},
    onDismissAddCustomFood: () -> Unit = {},
    onSaveCustomFood: (
        name: String,
        barcode: String?,
        brand: String?,
        calories100g: Float?,
        proteinG100g: Float?,
        fatG100g: Float?,
        carbsG100g: Float?,
        photoFile: File?,
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MealSheetHeader(
                title = "Добавить в $mealSlotLabel",
                subtitle = "Поиск по каталогу или сканирование штрихкода",
                icon = Icons.Filled.Restaurant,
                onDismiss = onDismiss,
            )

            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppTextField(
                        value = uiState.foodSearchQuery,
                        onValueChange = onQueryChange,
                        label = "Название продукта",
                        leadingIcon = Icons.Filled.Search,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        AppButton(
                            text = if (uiState.isFoodSearchLoading) "Ищем…" else "Найти",
                            onClick = onSearchNow,
                            enabled = !uiState.isFoodSearchLoading,
                            modifier = Modifier.weight(1f),
                        )
                        AppButton(
                            text = "Штрихкод",
                            onClick = onOpenBarcode,
                            enabled = !uiState.isFoodSearchLoading,
                            isSecondary = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    AppButton(
                        text = "Свой продукт",
                        onClick = onOpenAddCustomFood,
                        isSecondary = true,
                    )
                }
            }

            uiState.foodSearchError?.let {
                AppMessageBanner(text = it, type = AppMessageType.Error)
            }
            uiState.error?.let {
                AppMessageBanner(text = it, type = AppMessageType.Error)
            }

            if (uiState.isFoodSearchLoading) {
                CircularProgressIndicator(
                    Modifier
                        .size(32.dp)
                        .align(Alignment.CenterHorizontally),
                )
            }

            AnimatedVisibility(
                visible = savedDishes.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(title = "Мои блюда", subtitle = "Готовые составные блюда")
                    savedDishes.forEach { dish ->
                        AppCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = dish.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                )
                                AppButton(
                                    text = "Выбрать",
                                    onClick = { onSelectSavedDish(dish) },
                                    isSecondary = true,
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState.foodSearchResults.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(
                        title = "Результаты",
                        subtitle = "Open Food Facts и ваш каталог",
                    )
                    uiState.foodSearchResults.take(16).forEach { hit ->
                        MealFoodCatalogHitRow(
                            hit = hit,
                            onClick = { onSelectFood(hit) },
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState.mealName.isNotBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                AppCard {
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
                        if (uiState.selectedCatalogItem?.needsCompletion == true) {
                            AppMessageBanner(
                                text = "У продукта неполное КБЖУ — дополните и сохраните в каталог",
                                type = AppMessageType.Warning,
                            )
                            AppButton(
                                text = "Дополнить БЖУ",
                                onClick = onOpenMacroCompletion,
                                enabled = !uiState.isCatalogSaving,
                                isSecondary = true,
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

    FoodMacroCompletionSheet(
        visible = uiState.showMacroCompletionSheet,
        uiState = uiState,
        onDismiss = onDismissMacroCompletion,
        onProteinChange = onMacroProteinChange,
        onFatChange = onMacroFatChange,
        onCarbsChange = onMacroCarbsChange,
        onSave = onSaveMacroCompletion,
    )

    AddCustomFoodSheet(
        visible = uiState.showAddCustomFoodSheet,
        uiState = uiState,
        onDismiss = onDismissAddCustomFood,
        onSave = onSaveCustomFood,
    )
}
