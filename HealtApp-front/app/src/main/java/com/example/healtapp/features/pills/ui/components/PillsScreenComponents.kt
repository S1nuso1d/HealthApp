package com.example.healtapp.features.pills.ui.components

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.animation.appPressScale
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.FeatureHeroBar
import com.example.healtapp.core.ui.components.FeatureHeroChip
import com.example.healtapp.core.ui.components.GradientFormPanel
import com.example.healtapp.core.ui.components.GradientOutlinedField
import com.example.healtapp.core.ui.theme.MintPrimary
import com.example.healtapp.core.ui.theme.brandingGradient
import com.example.healtapp.core.ui.theme.chipSelectedColor
import com.example.healtapp.core.ui.theme.contentPrimaryColor
import com.example.healtapp.core.ui.theme.contentSecondaryColor
import com.example.healtapp.core.ui.theme.iconBadgeGradient
import com.example.healtapp.core.ui.theme.metricIconGradient
import com.example.healtapp.core.ui.theme.subtleFillGradient
import com.example.healtapp.core.ui.theme.themedCardBlue
import com.example.healtapp.core.ui.theme.themedCardMint
import com.example.healtapp.data.network.dto.health.PillDto
import androidx.compose.foundation.interaction.MutableInteractionSource

private val PillCapabilityItems = listOf(
    Triple(Icons.Filled.Medication, "Витамины", "D, C, комплексы"),
    Triple(Icons.Filled.Schedule, "По времени", "утро и вечер"),
    Triple(Icons.Filled.NotificationsActive, "Push", "напоминания"),
)

private val CommonReminderTimes = listOf(
    "08:00" to (8 to 0),
    "12:00" to (12 to 0),
    "20:00" to (20 to 0),
    "21:00" to (21 to 0),
)

@Composable
fun PillsHeroBar(
    activeCount: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FeatureHeroBar(
        title = "Витамины и таблетки",
        subtitle = "Напоминания о приёме",
        icon = Icons.Filled.Medication,
        onBack = onBack,
        modifier = modifier,
        footer = {
            Spacer(Modifier.height(10.dp))
            FeatureHeroChip(
                label = if (activeCount > 0) {
                    "Активных: $activeCount"
                } else {
                    "Нет активных напоминаний"
                },
                modifier = Modifier.padding(start = 12.dp),
            )
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PillsWelcomePanel(
    showActiveTab: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (showActiveTab) "Пока нет активных" else "Список пуст",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (showActiveTab) {
                    "Нажмите «Напомнить о приёме» внизу — мы пришлём push в выбранное время."
                } else {
                    "Создайте первое напоминание — оно появится здесь карточкой, как сообщение в чате."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = contentSecondaryColor(),
            )
        }

        Text(
            text = "Как это работает",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentSecondaryColor(),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 2,
        ) {
            PillCapabilityItems.forEachIndexed { index, (icon, title, subtitle) ->
                PillCapabilityTile(
                    icon = icon,
                    title = title,
                    subtitle = subtitle,
                    mintTint = index % 2 == 0,
                )
            }
        }
    }
}

@Composable
private fun PillCapabilityTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    mintTint: Boolean,
) {
    val cardBase = if (mintTint) themedCardMint() else themedCardBlue()
    Row(
        modifier = Modifier
            .widthIn(min = 148.dp, max = 180.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                RoundedCornerShape(20.dp),
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(metricIconGradient(cardBase, mintTint))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        }
        Column {
            Text(text = title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = contentSecondaryColor())
        }
    }
}

@Composable
fun PillsFilterStrip(
    selectedTab: Int,
    activeCount: Int,
    totalCount: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(subtleFillGradient()))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                RoundedCornerShape(18.dp),
            )
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PillsFilterChip(
            label = "Активные ($activeCount)",
            selected = selectedTab == 0,
            onClick = { onSelect(0) },
            modifier = Modifier.weight(1f),
        )
        PillsFilterChip(
            label = "Все ($totalCount)",
            selected = selectedTab == 1,
            onClick = { onSelect(1) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PillsFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.background(Brush.linearGradient(brandingGradient()))
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) Color.White else contentSecondaryColor(),
        )
    }
}

@Composable
fun PillReminderBubble(
    pill: PillDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeLabel = pill.timeOfDay.take(5)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (pill.isActive) 1f else 0.72f),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        PillAvatar()
        Column(
            modifier = Modifier
                .padding(start = 10.dp)
                .widthIn(max = 340.dp)
                .clip(RoundedCornerShape(6.dp, 22.dp, 22.dp, 22.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                    RoundedCornerShape(6.dp, 22.dp, 22.dp, 22.dp),
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (pill.isActive) "Напоминание" else "На паузе",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (pill.isActive) MintPrimary else contentSecondaryColor(),
                )
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = pill.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentPrimaryColor(),
            )
            Text(
                text = pill.dosage,
                style = MaterialTheme.typography.bodyMedium,
                color = contentSecondaryColor(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = if (pill.isActive) "Вкл" else "Выкл",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentSecondaryColor(),
                    )
                    Switch(
                        checked = pill.isActive,
                        onCheckedChange = onToggleActive,
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Редактировать",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Удалить",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PillAvatar() {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(brandingGradient())),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Medication,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
fun PillsAddReminderDock(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(22.dp)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Каждый день в выбранное время придёт уведомление",
            style = MaterialTheme.typography.labelSmall,
            color = contentSecondaryColor(),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.55f)
                .clip(shape)
                .background(Brush.linearGradient(brandingGradient()))
                .appPressScale(interaction, pressedScale = 0.98f)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick,
                )
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Напомнить о приёме",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = "Добавить витамин или таблетку",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PillReminderSheet(
    visible: Boolean,
    pill: PillDto?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, dosage: String, timeOfDay: String) -> Unit,
) {
    if (!visible) return

    val isEditing = pill != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    var name by remember(pill?.id) { mutableStateOf(pill?.name.orEmpty()) }
    var dosage by remember(pill?.id) { mutableStateOf(pill?.dosage.orEmpty()) }
    var hour by remember(pill?.id) {
        mutableIntStateOf(pill?.timeOfDay?.take(2)?.toIntOrNull() ?: 8)
    }
    var minute by remember(pill?.id) {
        mutableIntStateOf(pill?.timeOfDay?.drop(3)?.take(2)?.toIntOrNull() ?: 0)
    }

    val timeLabel = "%02d:%02d".format(hour, minute)
    val canSave = name.isNotBlank() && dosage.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(brandingGradient())),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Medication,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (isEditing) "Изменить напоминание" else "Новое напоминание",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (isEditing) {
                                "Обновите название, дозу или время push-уведомления"
                            } else {
                                "Укажите препарат — мы напомним принять его каждый день"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Закрыть")
                }
            }

            if (canSave) {
                PillReminderPreviewCard(
                    name = name.trim(),
                    dosage = dosage.trim(),
                    timeLabel = timeLabel,
                )
            }

            GradientFormPanel {
                Text(
                    text = "1. Что принимаете?",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                GradientOutlinedField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Название препарата",
                )

                Text(
                    text = "2. Сколько за раз?",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                GradientOutlinedField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = "Дозировка",
                )

                Text(
                    text = "3. Когда напомнить?",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(iconBadgeGradient())),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Column {
                            Text(
                                text = "Каждый день в $timeLabel",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "Push-уведомление на телефон",
                                style = MaterialTheme.typography.bodySmall,
                                color = contentSecondaryColor(),
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, h, m ->
                                    hour = h
                                    minute = m
                                },
                                hour,
                                minute,
                                true,
                            ).show()
                        },
                    ) {
                        Text("Изменить")
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CommonReminderTimes.forEach { (label, time) ->
                        val (h, m) = time
                        FilterChip(
                            selected = hour == h && minute == m,
                            onClick = {
                                hour = h
                                minute = m
                            },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = chipSelectedColor(themedCardMint()),
                            ),
                        )
                    }
                }
            }

            AppButton(
                text = when {
                    !canSave -> "Заполните название и дозировку"
                    isEditing -> "Сохранить изменения"
                    else -> "Включить напоминание"
                },
                onClick = {
                    val formattedTime = "%02d:%02d:00".format(hour, minute)
                    onConfirm(name.trim(), dosage.trim(), formattedTime)
                },
                enabled = canSave,
            )
        }
    }
}

@Composable
private fun PillReminderPreviewCard(
    name: String,
    dosage: String,
    timeLabel: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(subtleFillGradient()))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                RoundedCornerShape(20.dp),
            )
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PillAvatar()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Так будет выглядеть",
                style = MaterialTheme.typography.labelSmall,
                color = contentSecondaryColor(),
            )
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "$dosage · напоминание в $timeLabel",
                style = MaterialTheme.typography.bodySmall,
                color = contentSecondaryColor(),
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
