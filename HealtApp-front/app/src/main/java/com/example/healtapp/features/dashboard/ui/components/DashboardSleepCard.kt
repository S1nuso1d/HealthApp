package com.example.healtapp.features.dashboard.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.runtime.Composable
import com.example.healtapp.core.ui.theme.cardHeaderGradientMuted
import com.example.healtapp.core.ui.theme.themedCardLavender

@Composable
fun DashboardSleepCard(
    sleepHours: Float,
    sleepTargetHours: Float,
    sleepQuality: String,
    onClick: (() -> Unit)? = null,
) {
    DashboardSectionCard(
        title = "Сон",
        value = "$sleepHours / $sleepTargetHours ч",
        subtitle = "Среднее качество: $sleepQuality/100",
        icon = Icons.Filled.Bedtime,
        iconBackground = cardHeaderGradientMuted(themedCardLavender()),
        onClick = onClick,
    )
}
