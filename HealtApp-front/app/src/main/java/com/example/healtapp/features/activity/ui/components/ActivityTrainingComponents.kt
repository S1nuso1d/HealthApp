package com.example.healtapp.features.activity.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.core.ui.theme.brandingGradient
import com.example.healtapp.core.ui.theme.cardHeaderGradient
import com.example.healtapp.core.ui.theme.themedCardBlue
import com.example.healtapp.data.network.dto.activity.ActivityDto
import com.example.healtapp.features.activity.presentation.activityTitleFromApi

private fun trainingIcon(typeRu: String): ImageVector = when (typeRu) {
    "Бег" -> Icons.AutoMirrored.Filled.DirectionsRun
    "Велосипед" -> Icons.AutoMirrored.Filled.DirectionsBike
    "Плавание" -> Icons.Filled.Pool
    "Йога", "Растяжка" -> Icons.Filled.SelfImprovement
    "Силовая тренировка" -> Icons.Filled.FitnessCenter
    else -> Icons.AutoMirrored.Filled.DirectionsRun
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivityTrainingFormCard(
    activityType: String,
    activityTypes: List<String>,
    onActivityTypeSelected: (String) -> Unit,
    durationMinutes: String,
    onDurationChange: (String) -> Unit,
    calories: String,
    onCaloriesChange: (String) -> Unit,
    distanceKm: String,
    onDistanceChange: (String) -> Unit,
    intensity: String,
    intensityOptions: List<String>,
    onIntensitySelected: (String) -> Unit,
    isSaving: Boolean,
    onSave: () -> Unit,
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "Новая тренировка",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Тип",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                activityTypes.forEach { type ->
                    FilterChip(
                        selected = activityType == type,
                        onClick = { onActivityTypeSelected(type) },
                        label = { Text(type) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
            AppTextField(
                value = durationMinutes,
                onValueChange = onDurationChange,
                label = "Длительность (мин)",
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppTextField(
                    value = calories,
                    onValueChange = onCaloriesChange,
                    label = "Ккал",
                    modifier = Modifier.weight(1f),
                )
                AppTextField(
                    value = distanceKm,
                    onValueChange = onDistanceChange,
                    label = "Км",
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = "Интенсивность",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                intensityOptions.forEach { level ->
                    FilterChip(
                        selected = intensity == level,
                        onClick = { onIntensitySelected(level) },
                        label = { Text(level) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    )
                }
            }
            AppButton(
                text = if (isSaving) "Сохраняем…" else "Добавить в дневник",
                onClick = onSave,
                enabled = !isSaving,
            )
        }
    }
}

@Composable
fun ActivityTrainingHistoryRow(
    activity: ActivityDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val title = activityTitleFromApi(activity.activity_type)
    val icon = trainingIcon(title)
    val meta = buildString {
        append("${activity.duration_minutes} мин")
        activity.calories_burned?.let { append(" · ~${it.toInt()} ккал") }
        activity.distance_km?.let { append(" · ${"%.1f".format(it)} км") }
        activity.intensity?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
    }

    val iconGrad = cardHeaderGradient(themedCardBlue(), 0.45f)

    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Brush.verticalGradient(brandingGradient())),
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(iconGrad)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = activity.start_time.take(16).replace('T', ' '),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onEdit, modifier = Modifier.padding(0.dp)) {
                    Text("Изм.")
                }
                TextButton(onClick = onDelete, modifier = Modifier.padding(0.dp)) {
                    Text("Удал.", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
