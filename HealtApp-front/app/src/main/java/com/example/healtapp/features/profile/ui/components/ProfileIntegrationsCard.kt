package com.example.healtapp.features.profile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.features.profile.presentation.ProfileIntegrationUi

@Composable
fun ProfileIntegrationsCard(
    integrations: List<ProfileIntegrationUi>
) {
    AppCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Интеграции",
                style = MaterialTheme.typography.titleMedium
            )

            integrations.forEach { integration ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = integration.title,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = integration.status,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}