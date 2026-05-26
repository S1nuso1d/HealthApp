package com.example.healtapp.features.meal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.features.meal.DishIngredient
import com.example.healtapp.features.meal.DishIngredientsPayload

@Composable
fun SavedDishApplyDialog(
    dishName: String,
    initialIngredients: List<DishIngredient>,
    mealSlotLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (List<DishIngredient>, String) -> Unit,
) {
    var ingredients by remember(dishName, initialIngredients) {
        mutableStateOf(
            initialIngredients.map { ing ->
                if (ing.isTemplate) ing.copy(grams = if (ing.grams > 0f) ing.grams else 100f)
                else ing.copy(grams = if (ing.grams > 0f) ing.grams else 100f)
            },
        )
    }

    val totals = remember(ingredients) { DishIngredientsPayload(ingredients).totals() }
    val hasTemplates = initialIngredients.any { it.isTemplate }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("«$dishName»") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Приём: $mealSlotLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (hasTemplates) {
                        "Укажите граммы каждого продукта — калории посчитаются автоматически"
                    } else {
                        "Укажите граммы или скорректируйте порции"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Итого: ${totals.calories.toInt()} ккал · Б ${"%.0f".format(totals.protein)} · Ж ${"%.0f".format(totals.fat)} · У ${"%.0f".format(totals.carbs)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                ingredients.forEachIndexed { index, ing ->
                    PortionEditBlock(
                        ingredient = ing,
                        showPer100gHint = ing.isTemplate,
                        onChange = { updated ->
                            ingredients = ingredients.toMutableList().also { it[index] = updated }
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cleaned = ingredients.filter { it.name.isNotBlank() && it.grams > 0f }
                    if (cleaned.isNotEmpty()) onConfirm(cleaned, mealSlotLabel)
                },
            ) { Text("Добавить в дневник") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

@Composable
private fun PortionEditBlock(
    ingredient: DishIngredient,
    showPer100gHint: Boolean,
    onChange: (DishIngredient) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = ingredient.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )
        if (showPer100gHint) {
            Text(
                text = "${ingredient.caloriesPer100g.toInt()} ккал / 100 г",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AppTextField(
                value = if (ingredient.grams == 0f) "" else {
                    if (ingredient.grams % 1f == 0f) ingredient.grams.toInt().toString()
                    else "%.0f".format(ingredient.grams)
                },
                onValueChange = { onChange(ingredient.copy(grams = it.toFloatOrNull() ?: 0f)) },
                label = "Граммы",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
