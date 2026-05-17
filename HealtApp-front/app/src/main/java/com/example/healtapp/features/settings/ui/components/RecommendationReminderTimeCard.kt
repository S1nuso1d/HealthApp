package com.example.healtapp.features.settings.ui.components

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.theme.chipSelectedColor
import com.example.healtapp.core.ui.theme.themedCardMint

private data class PresetTime(val label: String, val hour: Int, val minute: Int)

private val presets = listOf(
    PresetTime("08:00", 8, 0),
    PresetTime("10:00", 10, 0),
    PresetTime("12:00", 12, 0),
    PresetTime("18:00", 18, 0),
    PresetTime("21:00", 21, 0),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecommendationReminderTimeCard(
    hour: Int,
    minute: Int,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val timeLabel = "%02d:%02d".format(hour, minute)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column {
                    Text(
                        text = "Время уведомления",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Каждый день в $timeLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            TextButton(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, h, m -> onTimeSelected(h, m) },
                        hour,
                        minute,
                        true,
                    ).show()
                },
            ) {
                Text("Изменить")
            }
        }

        Text(
            text = "Быстрый выбор",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            presets.forEach { preset ->
                val selected = preset.hour == hour && preset.minute == minute
                FilterChip(
                    selected = selected,
                    onClick = { onTimeSelected(preset.hour, preset.minute) },
                    label = { Text(preset.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chipSelectedColor(themedCardMint()),
                    ),
                )
            }
        }

        Text(
            text = "После выбора времени следующее уведомление придёт в ближайший такой час (сегодня или завтра).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
