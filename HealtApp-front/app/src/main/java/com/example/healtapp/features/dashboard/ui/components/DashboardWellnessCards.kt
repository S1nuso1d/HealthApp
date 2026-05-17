package com.example.healtapp.features.dashboard.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.animation.AppMotion
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.core.ui.theme.accentColor
import com.example.healtapp.core.ui.theme.heroBlockGradient
import com.example.healtapp.core.ui.theme.heroContentColor
import com.example.healtapp.core.ui.theme.isAppDarkTheme
import com.example.healtapp.features.dashboard.presentation.ActionPlanItemUi
import com.example.healtapp.features.dashboard.presentation.DailyBriefUi
import com.example.healtapp.features.dashboard.presentation.MoodCheckInUi
import com.example.healtapp.features.dashboard.presentation.ScoreBreakdownUi
import com.example.healtapp.features.dashboard.presentation.SmartReminderUi

private val moodEmojis = listOf("😫", "😕", "😐", "🙂", "😊")

@Composable
fun DashboardDailyBriefCard(
    brief: DailyBriefUi,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (isAppDarkTheme()) 12.dp else 26.dp))
            .background(Brush.linearGradient(heroBlockGradient()))
            .padding(20.dp),
    ) {
        val onHero = heroContentColor()
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, null, tint = onHero, modifier = Modifier.size(22.dp))
                Text(brief.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = onHero)
            }
            Text(brief.summary, style = MaterialTheme.typography.bodyMedium, color = onHero.copy(alpha = 0.92f))
            brief.keyPoints.take(3).forEach { point ->
                Text("• $point", style = MaterialTheme.typography.bodySmall, color = onHero.copy(alpha = 0.85f))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardMoodCheckInCard(
    state: MoodCheckInUi,
    onMoodChange: (Int) -> Unit,
    onEnergyChange: (Int) -> Unit,
    onStressChange: (Int) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Mood, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Как вы сегодня?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                if (state.savedToday) {
                    Text("Сохранено ✓", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            Text("Настроение", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                moodEmojis.forEachIndexed { index, emoji ->
                    val value = index + 1
                    FilterChip(
                        selected = state.mood == value,
                        onClick = { onMoodChange(value) },
                        label = { Text(emoji) },
                    )
                }
            }
            Text("Энергия: ${state.energy}", style = MaterialTheme.typography.labelLarge)
            Slider(value = state.energy.toFloat(), onValueChange = { onEnergyChange(it.toInt()) }, valueRange = 1f..10f, steps = 8)
            Text("Стресс: ${state.stress}", style = MaterialTheme.typography.labelLarge)
            Slider(value = state.stress.toFloat(), onValueChange = { onStressChange(it.toInt()) }, valueRange = 1f..10f, steps = 8)
            AppButton(
                text = if (state.isSaving) "Сохраняем…" else if (state.savedToday) "Обновить отметку" else "Сохранить",
                onClick = onSubmit,
                enabled = !state.isSaving,
            )
        }
    }
}

@Composable
fun DashboardScoresCard(
    scores: ScoreBreakdownUi,
    modifier: Modifier = Modifier,
) {
    val animatedHealth by animateFloatAsState(scores.healthScore / 100f, tween(AppMotion.MEDIUM_MS), label = "health")
    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Индекс здоровья", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("${scores.healthScore}/100", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(
                progress = { animatedHealth },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            )
            ScoreRow("Сон", scores.sleepScore)
            ScoreRow("Вода", scores.hydrationScore)
            ScoreRow("Активность", scores.activityScore)
            ScoreRow("Питание", scores.nutritionScore)
            ScoreRow("Состояние", scores.stateScore)
        }
    }
}

@Composable
private fun ScoreRow(label: String, score: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("$score", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun DashboardStreaksRow(
    waterStreak: Int,
    stepsStreak: Int,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StreakChip("Вода", waterStreak, Modifier.weight(1f))
        StreakChip("Шаги", stepsStreak, Modifier.weight(1f))
    }
}

@Composable
private fun StreakChip(label: String, days: Int, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("$days", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("$label · дней подряд", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DashboardActionPlanPreview(
    items: List<ActionPlanItemUi>,
    onOpenAll: () -> Unit,
    onToggle: (ActionPlanItemUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title = "План на сегодня", subtitle = "Задачи от рекомендаций")
        items.take(3).forEach { item ->
            AppCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    IconButton(onClick = { onToggle(item) }) {
                        Icon(
                            if (item.status == "done") Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                        Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        AppButton(text = "Весь план действий", onClick = onOpenAll, isSecondary = true)
    }
}

@Composable
fun DashboardSmartRemindersBlock(
    reminders: List<SmartReminderUi>,
    onComplete: (Int) -> Unit,
    onDismiss: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (reminders.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title = "Умные напоминания", subtitle = "Персональные подсказки")
        reminders.take(4).forEach { reminder ->
            AppCard {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.NotificationsActive, null, tint = accentColor(), modifier = Modifier.padding(end = 10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(reminder.title, fontWeight = FontWeight.SemiBold)
                        Text(reminder.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onDismiss(reminder.id) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Отклонить", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                AppButton(text = "Готово", onClick = { onComplete(reminder.id) }, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
fun DashboardQuickLinksRow(
    onOpenAi: () -> Unit,
    onOpenTimeline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        QuickLinkCard("AI-советник", Icons.AutoMirrored.Filled.Chat, onOpenAi, Modifier.weight(1f))
        QuickLinkCard("Лента", Icons.Filled.Timeline, onOpenTimeline, Modifier.weight(1f))
    }
}

@Composable
private fun QuickLinkCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier, onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun AnimatedDashboardSection(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(AppMotion.MEDIUM_MS)) + expandVertically(tween(AppMotion.MEDIUM_MS)),
        exit = fadeOut(tween(AppMotion.SHORT_MS)) + shrinkVertically(tween(AppMotion.SHORT_MS)),
    ) {
        content()
    }
}
