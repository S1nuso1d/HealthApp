package com.example.healtapp.features.actionplan.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.healtapp.core.ui.components.AppCard

@Composable
fun ActionTaskCard(text: String) {
    AppCard { Text(text) }
}