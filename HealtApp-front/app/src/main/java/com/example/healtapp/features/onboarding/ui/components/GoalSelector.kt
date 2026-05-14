package com.example.healtapp.features.onboarding.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.common.Constants
import com.example.healtapp.core.ui.theme.CardBlue

private data class GoalOption(val id: String, val label: String)

private val goals = listOf(
    GoalOption(Constants.Goals.BETTER_SLEEP, "Лучше спать"),
    GoalOption(Constants.Goals.LOSE_WEIGHT, "Похудеть"),
    GoalOption(Constants.Goals.GAIN_MUSCLE, "Набрать массу"),
    GoalOption(Constants.Goals.IMPROVE_ENERGY, "Больше энергии"),
)

@Composable
fun GoalSelector(
    selectedGoal: String,
    onGoalSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Главная цель",
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            goals.forEach { option ->
                FilterChip(
                    selected = selectedGoal == option.id,
                    onClick = { onGoalSelected(option.id) },
                    label = { Text(option.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CardBlue,
                    ),
                )
            }
        }
    }
}
