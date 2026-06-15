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
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppMessageBanner
import com.example.healtapp.core.ui.components.AppMessageType
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.data.network.dto.meal.FoodCatalogItemDto
import com.example.healtapp.data.network.dto.meal.SavedDishDto
import com.example.healtapp.features.meal.presentation.MealUiState
import androidx.compose.material3.TextButton
import com.example.healtapp.features.meal.ui.components.MealFoodCatalogHitRow
import com.example.healtapp.features.meal.ui.components.FoodMacroCompletionSheet
import com.example.healtapp.features.meal.ui.components.AddCustomFoodSheet
import com.example.healtapp.features.meal.ui.components.MealServingPicker
import android.content.Intent
import android.speech.RecognizerIntent
import android.app.Activity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.activity.result.PickVisualMediaRequest
import android.net.Uri
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import java.io.File
import kotlinx.coroutines.delay

fun getFileFromUri(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("food_", ".jpg", context.cacheDir)
        tempFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        tempFile
    } catch (e: Exception) {
        null
    }
}

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
    onRecognizePhoto: (File) -> Unit = {},
    onRecognizeVoice: (String) -> Unit = {},
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
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val file = getFileFromUri(context, uri)
            if (file != null) {
                onRecognizePhoto(file)
            }
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                onRecognizeVoice(matches[0])
            }
        }
    }

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
                        text = "Open Food Facts и ваш каталог — поиск или штрихкод",
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
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = "Штрихкод")
                }
                IconButton(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Что вы съели?")
                        }
                        try {
                            voiceLauncher.launch(intent)
                        } catch (e: Exception) {
                            // No speech recognizer
                        }
                    },
                    enabled = !uiState.isFoodSearchLoading
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = "Голосовой ввод")
                }
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !uiState.isFoodSearchLoading
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = "Распознать по фото")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onOpenAddCustomFood) {
                    Text("Добавить свой продукт")
                }
            }

            uiState.foodSearchError?.let {
                AppMessageBanner(text = it, type = AppMessageType.Error)
            }
            uiState.error?.let {
                AppMessageBanner(text = it, type = AppMessageType.Error)
            }

            if (uiState.isFoodSearchLoading) {
                CircularProgressIndicator(Modifier.size(32.dp).align(Alignment.CenterHorizontally))
            }

            AnimatedVisibility(
                visible = savedDishes.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Мои блюда",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    savedDishes.forEach { dish ->
                        AppCard {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = dish.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { onSelectSavedDish(dish) }) {
                                    Text("Выбрать")
                                }
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
