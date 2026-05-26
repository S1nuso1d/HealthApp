package com.example.healtapp.features.dashboard.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.animation.AppMotion
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.ProgressRing
import kotlin.math.roundToInt

@Composable
fun CaloriesBurnProgressCard(
    burnedToday: Int,
    burnGoal: Int,
    stepsToday: Int,
    activityMinutes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = if (burnGoal > 0) (burnedToday.toFloat() / burnGoal).coerceIn(0f, 1.15f) else 0f
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = AppMotion.tweenMedium(),
        label = "burn_ring",
    )
    val percent = if (burnGoal > 0) ((burnedToday * 100f) / burnGoal).roundToInt().coerceAtMost(999) else 0
    val remaining = (burnGoal - burnedToday).coerceAtLeast(0)

    AppCard(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Сожжено сегодня",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "$burnedToday ккал",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (burnedToday >= burnGoal && burnGoal > 0) {
                        "Цель $burnGoal ккал достигнута · $percent%"
                    } else {
                        "Цель $burnGoal ккал · осталось $remaining · $percent%"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "$activityMinutes мин тренировок · ${"%,d".format(stepsToday).replace(',', '\u00A0')} шагов",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(modifier = Modifier.padding(4.dp)) {
                ProgressRing(progress = animated, text = "$percent%")
            }
        }
    }
}
