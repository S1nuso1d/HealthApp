package com.example.healtapp.features.actionplan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.healtapp.features.actionplan.ui.components.ActionTaskCard
import com.example.healtapp.features.actionplan.ui.components.DailyPlanHeader

@Composable
fun ActionPlanScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DailyPlanHeader()
        ActionTaskCard("Не пить кофе после 16:00")
        ActionTaskCard("Пройтись вечером 20 минут")
        ActionTaskCard("Добрать воду до 2500 мл")
    }
}