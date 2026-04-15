package com.example.healtapp.features.hydration.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.healtapp.features.hydration.presentation.HydrationViewModel

@Composable
fun HydrationScreen() {
    val context = LocalContext.current
    val viewModel = remember { HydrationViewModel(context) }
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Вода",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Сегодня: ${uiState.waterToday} мл",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "Цель: ${uiState.target} мл",
            style = MaterialTheme.typography.bodyLarge
        )

        if (uiState.isLoading) {
            CircularProgressIndicator()
        }

        uiState.error?.let { errorText ->
            Text(
                text = errorText,
                color = MaterialTheme.colorScheme.error
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.addWater(200) },
                modifier = Modifier.weight(1f)
            ) {
                Text("+200 мл")
            }

            Button(
                onClick = { viewModel.addWater(250) },
                modifier = Modifier.weight(1f)
            ) {
                Text("+250 мл")
            }

            Button(
                onClick = { viewModel.addWater(500) },
                modifier = Modifier.weight(1f)
            ) {
                Text("+500 мл")
            }
        }
    }
}