package com.example.healtapp.features.sleep.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.core.ui.components.DatePickerField
import com.example.healtapp.core.ui.theme.cardHeaderGradient
import com.example.healtapp.core.ui.theme.chipSelectedColor
import com.example.healtapp.core.ui.theme.themedCardLavender

private val qualityPresets = listOf(60, 70, 80, 90)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SleepFormCard(
    sleepDate: String,
    sleepStart: String,
    sleepEnd: String,
    quality: String,
    note: String,
    isSaving: Boolean,
    onSleepDateChange: (String) -> Unit,
    onSleepStartChange: (String) -> Unit,
    onSleepEndChange: (String) -> Unit,
    onQualityChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSaveClick: () -> Unit,
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(cardHeaderGradient(themedCardLavender(), 0.4f)),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Nightlight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Column {
                    Text(
                        text = "Добавить ночь",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Выберите дату в календаре; пробуждение на след. день — автоматически",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            DatePickerField(
                value = sleepDate,
                onValueChange = onSleepDateChange,
                label = "Дата засыпания",
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppTextField(
                    value = sleepStart,
                    onValueChange = onSleepStartChange,
                    label = "Засыпание",
                    modifier = Modifier.weight(1f),
                )
                AppTextField(
                    value = sleepEnd,
                    onValueChange = onSleepEndChange,
                    label = "Пробуждение",
                    modifier = Modifier.weight(1f),
                )
            }

            Text(
                text = "Качество сна",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                qualityPresets.forEach { preset ->
                    FilterChip(
                        selected = quality == preset.toString(),
                        onClick = { onQualityChange(preset.toString()) },
                        label = { Text("$preset") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = chipSelectedColor(themedCardLavender()),
                        ),
                    )
                }
            }
            AppTextField(
                value = quality,
                onValueChange = onQualityChange,
                label = "Качество (0–100)",
            )
            AppTextField(
                value = note,
                onValueChange = onNoteChange,
                label = "Заметка (необязательно)",
            )
            AppButton(
                text = if (isSaving) "Сохранение…" else "Сохранить ночь",
                onClick = onSaveClick,
                enabled = !isSaving,
            )
        }
    }
}
