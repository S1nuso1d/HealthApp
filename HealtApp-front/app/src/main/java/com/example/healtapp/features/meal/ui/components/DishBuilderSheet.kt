package com.example.healtapp.features.meal.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppFormMetrics
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.core.ui.theme.heroBlockGradient
import com.example.healtapp.core.ui.theme.heroContentColor
import com.example.healtapp.data.network.dto.meal.FoodCatalogItemDto
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
    isEditing: Boolean = false,
    onDismiss: () -> Unit,
    onDishNameChange: (String) -> Unit,
    onIngredientsChange: (List<DishIngredient>) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearchDebounced: () -> Unit,
    onSearchNow: () -> Unit,
    onFetchFood: (FoodCatalogItemDto, (DishIngredient) -> Unit) -> Unit,
    onBarcodeLookup: (String, (DishIngredient) -> Unit) -> Unit,
    onSave: () -> Unit,
) {
    if (!visible) return
    val context = LocalContext.current
    var showBarcode by remember { mutableStateOf(false) }
    var replaceIndex by remember(visible) { mutableStateOf<Int?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) showBarcode = true }

    LaunchedEffect(uiState.foodSearchQuery) {
        delay(450)
        onSearchDebounced()
    }

    val refTotals = remember(ingredients) {
        DishIngredientsPayload(ingredients.map { it.per100gPreview() }).referencePer100g()
    }

    fun applyIngredient(template: DishIngredient) {
        val updated = if (replaceIndex != null) {
            ingredients.toMutableList().apply {
                val idx = replaceIndex!!
                if (idx in indices) set(idx, template)
            }
        } else {
            ingredients + template
        }
        onIngredientsChange(updated)
        replaceIndex = null
        onQueryChange("")
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(heroBlockGradient()))
                    .padding(18.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Restaurant, null, tint = heroContentColor())
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (isEditing) "Изменение блюда" else "Новое блюдо",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = heroContentColor(),
                        )
                        Text(
                            "Соберите состав из каталога — можно добавлять и удалять продукты",
                            style = MaterialTheme.typography.bodySmall,
                            color = heroContentColor().copy(alpha = 0.9f),
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, "Закрыть", tint = heroContentColor())
                    }
                }
            }

            AppTextField(
                value = dishName,
                onValueChange = onDishNameChange,
                label = "Название блюда",
            )

            if (ingredients.isNotEmpty()) {
                SectionHeader(
                    title = "Состав",
                    subtitle = "На 100 г: ${refTotals.calories.toInt()} ккал · " +
                        "Б ${refTotals.protein.toInt()} · Ж ${refTotals.fat.toInt()} · У ${refTotals.carbs.toInt()}",
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ingredients.forEachIndexed { index, ing ->
                        val isReplacing = replaceIndex == index
                        AppCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        ing.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isReplacing) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                    )
                                    Text(
                                        "${ing.caloriesPer100g.toInt()} ккал · Б ${ing.proteinPer100g.toInt()} · " +
                                            "Ж ${ing.fatPer100g.toInt()} · У ${ing.carbsPer100g.toInt()} / 100 г",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        replaceIndex = index
                                        onQueryChange("")
                                    },
                                ) {
                                    Text(if (isReplacing) "Меняем…" else "Заменить")
                                }
                                IconButton(
                                    onClick = {
                                        onIngredientsChange(ingredients.filterIndexed { i, _ -> i != index })
                                        if (replaceIndex == index) replaceIndex = null
                                    },
                                ) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = "Удалить продукт",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }
                if (replaceIndex != null) {
                    TextButton(onClick = { replaceIndex = null }) {
                        Text("Отменить замену")
                    }
                }
            }

            SectionHeader(
                title = if (replaceIndex != null) "Поиск замены" else "Добавить продукт",
                subtitle = "Поиск по названию или штрихкоду",
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppTextField(
                    value = uiState.foodSearchQuery,
                    onValueChange = onQueryChange,
                    label = if (replaceIndex != null) "Замена" else "Продукт",
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onSearchNow,
                    enabled = !uiState.isFoodSearchLoading,
                    modifier = Modifier.size(AppFormMetrics.ControlHeight),
                ) {
                    Icon(Icons.Filled.Search, contentDescription = "Искать")
                }
                IconButton(
                    onClick = {
                        if (hasCameraPermission(context)) showBarcode = true
                        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    modifier = Modifier.size(AppFormMetrics.ControlHeight),
                ) {
                    Icon(Icons.Filled.QrCode2, contentDescription = "Штрихкод")
                }
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
                        MealFoodCatalogHitRow(
                            hit = hit,
                            onClick = {
                                onFetchFood(hit) { template -> applyIngredient(template) }
                            },
                        )
                    }
                }
            }

            if (ingredients.isEmpty()) {
                Text(
                    text = "Добавьте хотя бы один продукт — его можно будет удалить или заменить.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                TextButton(
                    onClick = {
                        replaceIndex = null
                        onQueryChange("")
                    },
                ) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                    Text("  Добавить ещё")
                }
            }

            AppButton(
                text = if (isEditing) "Сохранить изменения" else "Сохранить блюдо",
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
            onBarcodeLookup(code) { template -> applyIngredient(template) }
        },
    )
}
