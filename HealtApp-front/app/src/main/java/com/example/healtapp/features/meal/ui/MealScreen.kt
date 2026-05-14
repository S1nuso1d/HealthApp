package com.example.healtapp.features.meal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.features.hydration.presentation.HydrationViewModel
import com.example.healtapp.features.meal.presentation.MealViewModel

@Composable
fun MealScreen() {
    val mealViewModel: MealViewModel = hiltViewModel()
    val hydrationViewModel: HydrationViewModel = hiltViewModel()

    val mealUiState by mealViewModel.uiState.collectAsState()
    val hydrationUiState by hydrationViewModel.uiState.collectAsState()

    val mealTypes = listOf(
        "Завтрак",
        "Обед",
        "Полдник",
        "Ужин",
        "Перекус"
    )

    var mealTypeExpanded by remember { mutableStateOf(false) }

    AppScreen(
        title = "Питание и вода",
        subtitle = "Калории, приёмы пищи, вода",
        headerIcon = Icons.Filled.Restaurant,
        scrollable = true,
    ) {
        Text(
            text = "Добавляй приёмы пищи и следи за водным балансом в одном разделе.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Приём пищи",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = mealUiState.mealType,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        label = { Text("Тип приёма пищи") },
                        placeholder = { Text("Выбери тип") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = "Открыть список"
                            )
                        },
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                mealTypeExpanded = true
                            }
                    )
                }

                DropdownMenu(
                    expanded = mealTypeExpanded,
                    onDismissRequest = { mealTypeExpanded = false }
                ) {
                    mealTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                mealViewModel.updateMealType(type)
                                mealTypeExpanded = false
                            }
                        )
                    }
                }

                AppTextField(
                    value = mealUiState.mealName,
                    onValueChange = mealViewModel::updateMealName,
                    label = "Название"
                )

                AppTextField(
                    value = mealUiState.calories,
                    onValueChange = mealViewModel::updateCalories,
                    label = "Калории"
                )

                AppTextField(
                    value = mealUiState.protein,
                    onValueChange = mealViewModel::updateProtein,
                    label = "Белки"
                )

                AppTextField(
                    value = mealUiState.fat,
                    onValueChange = mealViewModel::updateFat,
                    label = "Жиры"
                )

                AppTextField(
                    value = mealUiState.carbs,
                    onValueChange = mealViewModel::updateCarbs,
                    label = "Углеводы"
                )

                AppTextField(
                    value = mealUiState.caffeineMg,
                    onValueChange = mealViewModel::updateCaffeine,
                    label = "Кофеин (мг)"
                )

                AppButton(
                    text = if (mealUiState.isSaving) "Сохраняем..." else "Сохранить приём пищи",
                    onClick = { mealViewModel.saveMeal() },
                    enabled = !mealUiState.isSaving
                )

                mealUiState.error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                mealUiState.todayMeal?.let { meal ->
                    Text(
                        text = "Сегодня: ${meal.name ?: "Приём пищи"}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Вода",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Сегодня: ${hydrationUiState.waterToday} мл",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Цель: ${hydrationUiState.target} мл",
                    style = MaterialTheme.typography.bodyLarge
                )

                if (hydrationUiState.isLoading) {
                    CircularProgressIndicator()
                }

                hydrationUiState.error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { hydrationViewModel.addWater(200) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("+200")
                    }

                    Button(
                        onClick = { hydrationViewModel.addWater(250) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("+250")
                    }

                    Button(
                        onClick = { hydrationViewModel.addWater(500) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("+500")
                    }
                }
            }
        }
    }
}