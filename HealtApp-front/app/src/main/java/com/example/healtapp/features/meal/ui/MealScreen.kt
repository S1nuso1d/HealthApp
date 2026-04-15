package com.example.healtapp.features.meal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.di.AppModule
import com.example.healtapp.features.meal.presentation.MealViewModel

@Composable
fun MealScreen() {
    val context = LocalContext.current
    val viewModel = androidx.compose.runtime.remember {
        MealViewModel(AppModule.provideMealRepository(context))
    }

    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Питание",
            style = MaterialTheme.typography.headlineMedium
        )

        AppTextField(
            value = uiState.mealType,
            onValueChange = viewModel::updateMealType,
            label = "Тип приема пищи"
        )

        AppTextField(
            value = uiState.mealName,
            onValueChange = viewModel::updateMealName,
            label = "Название"
        )

        AppTextField(
            value = uiState.calories,
            onValueChange = viewModel::updateCalories,
            label = "Калории"
        )

        AppTextField(
            value = uiState.protein,
            onValueChange = viewModel::updateProtein,
            label = "Белки"
        )

        AppTextField(
            value = uiState.fat,
            onValueChange = viewModel::updateFat,
            label = "Жиры"
        )

        AppTextField(
            value = uiState.carbs,
            onValueChange = viewModel::updateCarbs,
            label = "Углеводы"
        )

        AppTextField(
            value = uiState.caffeineMg,
            onValueChange = viewModel::updateCaffeine,
            label = "Кофеин (мг)"
        )

        AppButton(
            text = if (uiState.isSaving) "Сохраняем..." else "Сохранить прием пищи",
            onClick = { viewModel.saveMeal() },
            enabled = !uiState.isSaving
        )

        uiState.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }

        uiState.todayMeal?.let { meal ->
            Text(
                text = "Сегодня: ${meal.name ?: "Прием пищи"}",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}