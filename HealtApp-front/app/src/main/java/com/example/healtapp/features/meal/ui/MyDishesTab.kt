package com.example.healtapp.features.meal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.data.network.dto.meal.SavedDishDto
import com.example.healtapp.features.meal.DishIngredient
import com.example.healtapp.features.meal.DishIngredientsJson
import com.example.healtapp.features.meal.DishIngredientsPayload
import com.example.healtapp.features.meal.presentation.MealViewModel
import com.example.healtapp.features.meal.ui.components.DishBuilderSheet

@Composable
fun MyDishesTab(
    snackbarHostState: SnackbarHostState,
    openBuilderRequest: Boolean = false,
    onBuilderRequestConsumed: () -> Unit = {},
) {
    val viewModel: MealViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    var showBuilder by remember { mutableStateOf(false) }
    var dishName by remember { mutableStateOf("") }
    var builderIngredients by remember { mutableStateOf<List<DishIngredient>>(emptyList()) }

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
            viewModel.clearMealSearchSelection()
            viewModel.updateFoodSearchQuery("")
            showBuilder = true
            onBuilderRequestConsumed()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (uiState.savedDishes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((screenHeight * 0.52f).coerceAtLeast(320.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 24.dp),
                ) {
                    Icon(
                        Icons.Filled.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    )
                    Text(
                        text = "Пока нет своих блюд",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Нажмите + внизу, чтобы собрать блюдо из продуктов",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                uiState.savedDishes.forEach { dish ->
                    SavedDishListCard(
                        dish = dish,
                        onDelete = { viewModel.deleteSavedDish(dish.id) },
                    )
                }
            }
        }
    }

    DishBuilderSheet(
        visible = showBuilder,
        uiState = uiState,
        ingredients = builderIngredients,
        dishName = dishName,
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
            viewModel.saveDishWithIngredients(name, null, builderIngredients)
            showBuilder = false
            dishName = ""
            builderIngredients = emptyList()
        },
    )

    Spacer(Modifier.height(88.dp))
}

@Composable
private fun SavedDishListCard(
    dish: SavedDishDto,
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
        Column(
            modifier = Modifier.padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(dish.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = "${ref.calories.toInt()} ккал · $subtitle",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ings.take(4).forEach { ing ->
                Text(
                    text = "· ${ing.name}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (ings.size > 4) {
                Text("… ещё ${ings.size - 4}", style = MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = onDelete) {
                Text("Удалить", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
