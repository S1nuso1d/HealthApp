package com.example.healtapp.features.profile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.features.profile.presentation.ProfileGoalUi

@Composable
fun ProfileGoalsCard(
    goals: List<ProfileGoalUi>
) {
    AppCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Цели",
                style = MaterialTheme.typography.titleMedium
            )

            goals.forEach { goal ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = goal.value,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}