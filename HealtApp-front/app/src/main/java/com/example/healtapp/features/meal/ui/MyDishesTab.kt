package com.example.healtapp.features.meal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.EmptyStateCard
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.core.ui.theme.Dimens
import com.example.healtapp.core.ui.theme.contentPrimaryColor
import com.example.healtapp.core.ui.theme.heroBlockGradient
import com.example.healtapp.core.ui.theme.heroContentColor
import com.example.healtapp.data.network.dto.meal.SavedDishDto
import com.example.healtapp.features.meal.DishIngredient
import com.example.healtapp.features.meal.DishIngredientsJson
import com.example.healtapp.features.meal.DishIngredientsPayload
import com.example.healtapp.features.meal.presentation.MealViewModel
import com.example.healtapp.features.meal.ui.components.DishBuilderSheet
import com.example.healtapp.features.meal.ui.components.SavedDishApplyDialog

@Composable
fun MyDishesTab(
    snackbarHostState: SnackbarHostState,
    openBuilderRequest: Boolean = false,
    onBuilderRequestConsumed: () -> Unit = {},
) {
    val viewModel: MealViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showBuilder by remember { mutableStateOf(false) }
    var editingDishId by remember { mutableStateOf<Int?>(null) }
    var dishName by remember { mutableStateOf("") }
    var builderIngredients by remember { mutableStateOf<List<DishIngredient>>(emptyList()) }
    var savedDishToApply by remember { mutableStateOf<SavedDishDto?>(null) }

    LaunchedEffect(uiState.snackMessage) {
        uiState.snackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackMessage()
        }
    }

    LaunchedEffect(openBuilderRequest) {
        if (openBuilderRequest) {
            dishName = ""
            builderIngredients = emptyList()
            editingDishId = null
            viewModel.clearMealSearchSelection()
            viewModel.updateFoodSearchQuery("")
            showBuilder = true
            onBuilderRequestConsumed()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.RadiusXl))
                .background(Brush.linearGradient(heroBlockGradient()))
                .padding(20.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Restaurant,
                        contentDescription = null,
                        tint = heroContentColor(),
                        modifier = Modifier.size(26.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (uiState.savedDishes.isEmpty()) "Свои блюда" else "${uiState.savedDishes.size} блюд",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = heroContentColor(),
                    )
                    Text(
                        text = "Соберите рецепт из каталога и добавляйте в дневник одним тапом",
                        style = MaterialTheme.typography.bodyMedium,
                        color = heroContentColor().copy(alpha = 0.9f),
                    )
                }
            }
        }

        if (uiState.savedDishes.isEmpty()) {
            EmptyStateCard(
                title = "Пока нет своих блюд",
                text = "Нажмите + внизу, чтобы собрать блюдо из каталога продуктов или по штрихкоду",
                icon = Icons.Filled.MenuBook,
            )
        } else {
            SectionHeader(title = "Библиотека")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                uiState.savedDishes.forEach { dish ->
                    SavedDishListCard(
                        dish = dish,
                        onAddToDiary = { savedDishToApply = dish },
                        onEdit = {
                            dishName = dish.name
                            builderIngredients = DishIngredientsJson.decode(dish.notes)
                            editingDishId = dish.id
                            viewModel.clearMealSearchSelection()
                            viewModel.updateFoodSearchQuery("")
                            showBuilder = true
                        },
                        onDuplicate = { viewModel.duplicateSavedDish(dish) },
                        onDelete = { viewModel.deleteSavedDish(dish.id) },
                    )
                }
            }
        }
    }

    savedDishToApply?.let { dish ->
        val ings = DishIngredientsJson.decode(dish.notes)
        SavedDishApplyDialog(
            dishName = dish.name,
            initialIngredients = ings,
            mealSlotLabel = "Обед",
            onDismiss = { savedDishToApply = null },
            onConfirm = { cleaned, slot ->
                viewModel.addSavedDishToDiaryFromIngredients(dish.name, slot, cleaned)
                savedDishToApply = null
            },
        )
    }

    DishBuilderSheet(
        visible = showBuilder,
        uiState = uiState,
        ingredients = builderIngredients,
        dishName = dishName,
        isEditing = editingDishId != null,
        onDismiss = { showBuilder = false },
        onDishNameChange = { dishName = it },
        onIngredientsChange = { builderIngredients = it },
        onQueryChange = viewModel::updateFoodSearchQuery,
        onSearchDebounced = viewModel::searchFoodByNameDebounced,
        onSearchNow = viewModel::searchFoodByNameNow,
        onFetchFood = viewModel::fetchIngredientTemplate,
        onBarcodeLookup = { code, onFound ->
            viewModel.searchBarcodeForDishTemplate(code, onFound)
        },
        onSave = {
            val name = dishName.trim().ifBlank { "Моё блюдо" }
            if (builderIngredients.isEmpty()) return@DishBuilderSheet
            if (editingDishId != null) {
                viewModel.updateDishWithIngredients(editingDishId!!, name, null, builderIngredients)
            } else {
                viewModel.saveDishWithIngredients(name, null, builderIngredients)
            }
            showBuilder = false
            dishName = ""
            builderIngredients = emptyList()
            editingDishId = null
        },
    )
}

@Composable
private fun SavedDishListCard(
    dish: SavedDishDto,
    onAddToDiary: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    val ings = DishIngredientsJson.decode(dish.notes)
    val ref = if (ings.any { it.isTemplate }) {
        DishIngredientsPayload(ings).referencePer100g()
    } else {
        DishIngredientsPayload(ings).totals()
    }
    val subtitle = when {
        ings.isEmpty() -> "Без состава"
        ings.any { it.isTemplate } -> "${ings.size} продукт(ов) · эталон на 100 г"
        else -> "${ings.size} продукт(ов)"
    }

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(heroBlockGradient())),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.MenuBook,
                        contentDescription = null,
                        tint = heroContentColor(),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        dish.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = contentPrimaryColor(),
                    )
                    Text(
                        text = "${ref.calories.toInt()} ккал · Б ${ref.protein.toInt()} · Ж ${ref.fat.toInt()} · У ${ref.carbs.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ings.take(3).forEach { ing ->
                        Text(
                            text = "· ${ing.name}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (ings.size > 3) {
                        Text("… ещё ${ings.size - 3}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            AppButton(text = "В дневник", onClick = onAddToDiary)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onEdit) { Text("Изменить") }
                TextButton(onClick = onDuplicate) { Text("Копия") }
                TextButton(onClick = onDelete) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
