package com.example.healtapp.features.sleep.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nightlight
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
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.theme.cardHeaderGradient
import com.example.healtapp.core.ui.theme.themedCardLavender

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
                        text = "Дата в календаре; пробуждение на след. день — автоматически",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            SleepFormFields(
                sleepDate = sleepDate,
                sleepStart = sleepStart,
                sleepEnd = sleepEnd,
                quality = quality,
                note = note,
                isSaving = isSaving,
                onSleepDateChange = onSleepDateChange,
                onSleepStartChange = onSleepStartChange,
                onSleepEndChange = onSleepEndChange,
                onQualityChange = onQualityChange,
                onNoteChange = onNoteChange,
                onSaveClick = onSaveClick,
            )
        }
    }
}
