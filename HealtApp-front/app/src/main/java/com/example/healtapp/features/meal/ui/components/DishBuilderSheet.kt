package com.example.healtapp.features.meal.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.features.meal.DishIngredient
import com.example.healtapp.features.meal.DishIngredientsPayload
import com.example.healtapp.features.meal.presentation.MealUiState
import com.example.healtapp.features.meal.ui.BarcodeScannerSheet
import com.example.healtapp.features.meal.ui.hasCameraPermission
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishBuilderSheet(
    visible: Boolean,
    uiState: MealUiState,
    ingredients: List<DishIngredient>,
    dishName: String,
    onDismiss: () -> Unit,
    onDishNameChange: (String) -> Unit,
    onIngredientsChange: (List<DishIngredient>) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearchDebounced: () -> Unit,
    onSearchNow: () -> Unit,
    onFetchFood: (String, (DishIngredient) -> Unit) -> Unit,
    onBarcodeLookup: (String, (DishIngredient) -> Unit) -> Unit,
    onSave: () -> Unit,
) {
    if (!visible) return
    val context = LocalContext.current
    var showBarcode by remember { mutableStateOf(false) }
    var searchRowCount by remember(visible) { mutableIntStateOf(1) }
    var activeSearchRow by remember(visible) { mutableIntStateOf(0) }
    var replaceIndex by remember(visible) { mutableStateOf<Int?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) showBarcode = true }

    LaunchedEffect(uiState.foodSearchQuery, activeSearchRow) {
        delay(450)
        onSearchDebounced()
    }

    val refTotals = remember(ingredients) {
        DishIngredientsPayload(ingredients.map { it.per100gPreview() }).referencePer100g()
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
                        "Новое блюдо",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Добавьте один или несколько продуктов из базы",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Закрыть")
                }
            }

            AppTextField(
                value = dishName,
                onValueChange = onDishNameChange,
                label = "Название блюда",
            )

            if (replaceIndex != null) {
                Text(
                    text = "Выберите другой продукт в поиске или по штрихкоду — он заменит «${ingredients.getOrNull(replaceIndex!!)?.name ?: ""}»",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                TextButton(onClick = { replaceIndex = null }) {
                    Text("Отменить замену")
                }
            }

            if (ingredients.isNotEmpty()) {
                Text(
                    "Состав (на 100 г): ${refTotals.calories.toInt()} ккал",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                ingredients.forEachIndexed { index, ing ->
                    val isReplacing = replaceIndex == index
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                ing.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = if (isReplacing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "${ing.caloriesPer100g.toInt()} ккал / 100 г",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            TextButton(
                                onClick = {
                                    replaceIndex = index
                                    activeSearchRow = 0
                                    onQueryChange("")
                                },
                            ) {
                                Text(if (isReplacing) "Меняем…" else "Заменить")
                            }
                            TextButton(
                                onClick = {
                                    onIngredientsChange(ingredients.filterIndexed { i, _ -> i != index })
                                    if (replaceIndex == index) replaceIndex = null
                                },
                            ) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                                Text("Удалить", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            Text(
                text = if (replaceIndex != null) "Поиск замены" else "Добавить продукт",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            repeat(searchRowCount) { rowIndex ->
                val isActive = rowIndex == activeSearchRow
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppTextField(
                        value = if (isActive) uiState.foodSearchQuery else "",
                        onValueChange = { value ->
                            activeSearchRow = rowIndex
                            onQueryChange(value)
                        },
                        label = when {
                            replaceIndex != null -> "Название или штрихкод замены"
                            rowIndex == 0 -> "Поиск продукта"
                            else -> "Продукт ${rowIndex + 1}"
                        },
                        modifier = Modifier.weight(1f),
                    )
                    if (isActive) {
                        IconButton(onClick = onSearchNow, enabled = !uiState.isFoodSearchLoading) {
                            Icon(Icons.Filled.Search, contentDescription = "Искать")
                        }
                        IconButton(onClick = {
                            if (hasCameraPermission(context)) showBarcode = true
                            else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }) {
                            Icon(Icons.Filled.QrCode2, contentDescription = "Штрихкод")
                        }
                    }
                }
            }

            TextButton(
                onClick = {
                    searchRowCount += 1
                    activeSearchRow = searchRowCount - 1
                    onQueryChange("")
                },
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Добавить ещё продукт")
            }

            uiState.foodSearchError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            if (uiState.isFoodSearchLoading) {
                CircularProgressIndicator(Modifier.size(28.dp).align(Alignment.CenterHorizontally))
            }

            AnimatedVisibility(
                visible = uiState.foodSearchResults.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    uiState.foodSearchResults.take(12).forEach { hit ->
                        MealFatSecretHitRow(
                            hit = hit,
                            onClick = {
                                onFetchFood(hit.foodId) { template ->
                                    val updated = if (replaceIndex != null) {
                                        ingredients.toMutableList().apply {
                                            if (replaceIndex!! in indices) set(replaceIndex!!, template)
                                        }
                                    } else {
                                        ingredients + template
                                    }
                                    onIngredientsChange(updated)
                                    replaceIndex = null
                                    onQueryChange("")
                                    if (replaceIndex == null && activeSearchRow < searchRowCount - 1) {
                                        activeSearchRow += 1
                                    }
                                }
                            },
                        )
                    }
                }
            }

            AppButton(
                text = "Сохранить блюдо",
                onClick = onSave,
                enabled = dishName.isNotBlank() && ingredients.isNotEmpty() && !uiState.isSaving,
            )
        }
    }

    BarcodeScannerSheet(
        visible = showBarcode,
        onDismiss = { showBarcode = false },
        onBarcode = { code ->
            showBarcode = false
            onBarcodeLookup(code) { template ->
                val updated = if (replaceIndex != null) {
                    ingredients.toMutableList().apply {
                        if (replaceIndex!! in indices) set(replaceIndex!!, template)
                    }
                } else {
                    ingredients + template
                }
                onIngredientsChange(updated)
                replaceIndex = null
                onQueryChange("")
            }
        },
    )
}
