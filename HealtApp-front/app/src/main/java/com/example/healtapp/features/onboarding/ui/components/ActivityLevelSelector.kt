package com.example.healtapp.features.onboarding.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.common.Constants
import com.example.healtapp.core.ui.theme.CardMint

private data class LevelOption(val id: String, val label: String)

private val levels = listOf(
    LevelOption(Constants.ActivityLevel.LOW, "Низкая"),
    LevelOption(Constants.ActivityLevel.MEDIUM, "Средняя"),
    LevelOption(Constants.ActivityLevel.HIGH, "Высокая"),
)

@Composable
fun ActivityLevelSelector(
    selected: String,
    onSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Уровень активности",
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            levels.forEach { option ->
                FilterChip(
                    selected = selected == option.id,
                    onClick = { onSelected(option.id) },
                    label = { Text(option.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CardMint,
                    ),
                )
            }
        }
    }
}
