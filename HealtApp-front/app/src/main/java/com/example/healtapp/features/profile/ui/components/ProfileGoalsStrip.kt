package com.example.healtapp.features.profile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.theme.metricIconGradient
import com.example.healtapp.core.ui.theme.themedCardBlue
import com.example.healtapp.core.ui.theme.themedCardLavender
import com.example.healtapp.core.ui.theme.themedCardMint
import java.text.DecimalFormat

@Composable
fun ProfileGoalsStrip(
    targetSleep: String,
    targetWater: String,
    targetSteps: String,
    onEditClick: () -> Unit,
) {
    val sleep = targetSleep.toFloatOrNull()?.let { DecimalFormat("#.#").format(it) } ?: "—"
    val water = targetWater.toIntOrNull()?.let { "%,d".format(it).replace(',', '\u00A0') } ?: "—"
    val steps = targetSteps.toIntOrNull()?.let { "%,d".format(it).replace(',', '\u00A0') } ?: "—"

    AppCard(
        modifier = Modifier.clickable(onClick = onEditClick),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Твои цели",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Нажмите, чтобы изменить",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Изменить цели")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProfileGoalChip(
                    icon = Icons.Filled.Bedtime,
                    label = "Сон",
                    value = "$sleep ч",
                    gradient = metricIconGradient(themedCardLavender()),
                    modifier = Modifier.weight(1f),
                )
                ProfileGoalChip(
                    icon = Icons.Filled.WaterDrop,
                    label = "Вода",
                    value = "$water мл",
                    gradient = metricIconGradient(themedCardBlue()),
                    modifier = Modifier.weight(1f),
                )
            }
            ProfileGoalChip(
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                label = "Шаги",
                value = steps,
                gradient = metricIconGradient(themedCardMint(), mintTint = true),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ProfileGoalChip(
    icon: ImageVector,
    label: String,
    value: String,
    gradient: List<androidx.compose.ui.graphics.Color>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(gradient))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
