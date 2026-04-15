package com.example.healtapp.features.dashboard.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.ProgressRing
import com.example.healtapp.core.utils.FormatUtils

@Composable
fun WaterProgressCard(current: Int, target: Int) {
    AppCard {
        Text(
            text = "Вода",
            style = MaterialTheme.typography.titleMedium
        )
        ProgressRing(
            progress = FormatUtils.waterProgress(current, target),
            text = "$current/$target"
        )
    }
}