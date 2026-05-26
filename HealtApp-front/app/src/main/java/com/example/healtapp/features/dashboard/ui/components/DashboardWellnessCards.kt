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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.example.healtapp.core.ui.theme.metricIconGradient
import com.example.healtapp.core.ui.theme.themedCardBlue
import com.example.healtapp.core.ui.theme.themedCardLavender
import com.example.healtapp.core.ui.theme.themedCardMint
import com.example.healtapp.core.ui.theme.heroBlockGradient
import com.example.healtapp.core.ui.theme.heroContentColor
import com.example.healtapp.core.ui.theme.contentPrimaryColor
import com.example.healtapp.core.ui.theme.iconTintColor
import com.example.healtapp.core.ui.theme.isAppDarkTheme
import com.example.healtapp.core.common.ActionPlanProgressHint
import com.example.healtapp.features.dashboard.presentation.ActionPlanItemUi
import com.example.healtapp.features.dashboard.presentation.DailyBriefUi
import com.example.healtapp.features.dashboard.presentation.MoodCheckInUi
import com.example.healtapp.features.dashboard.presentation.ScoreBreakdownUi
import com.example.healtapp.features.dashboard.presentation.WeeklyMetricUi
import com.example.healtapp.features.dashboard.presentation.WeeklySummaryUi

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
    var expanded by rememberSaveable(state.savedToday) { mutableStateOf(!state.savedToday) }

    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Mood, null, tint = iconTintColor())
                    Column {
                        Text(
                            "Как вы сегодня?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (state.savedToday && !expanded) {
                            Text(
                                "Отметка сохранена · ${moodEmojis.getOrNull(state.mood - 1) ?: "🙂"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "Энергия ${state.energy}/10 · Стресс ${state.stress}/10",
                                style = MaterialTheme.typography.bodySmall,
                                color = contentPrimaryColor(),
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
                if (state.savedToday) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (expanded) "Свернуть" else "Развернуть",
                        )
                    }
                }
            }
            AnimatedVisibility(visible = expanded || !state.savedToday) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    Slider(
                        value = state.energy.toFloat(),
                        onValueChange = { onEnergyChange(it.toInt()) },
                        valueRange = 1f..10f,
                        steps = 8,
                    )
                    Text("Стресс: ${state.stress}", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = state.stress.toFloat(),
                        onValueChange = { onStressChange(it.toInt()) },
                        valueRange = 1f..10f,
                        steps = 8,
                    )
                    AppButton(
                        text = when {
                            state.isSaving -> "Сохраняем…"
                            state.savedToday -> "Обновить отметку"
                            else -> "Сохранить"
                        },
                        onClick = {
                            onSubmit()
                            if (!state.savedToday) expanded = false
                        },
                        enabled = !state.isSaving,
                    )
                }
            }
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
            Text("${scores.healthScore}/100", style = MaterialTheme.typography.headlineSmall, color = contentPrimaryColor(), fontWeight = FontWeight.Bold)
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
        StreakChip("Шаги", stepsStreak, Modifier.weight(1f), hint = "подряд с сегодня")
    }
}

@Composable
private fun StreakChip(label: String, days: Int, modifier: Modifier = Modifier, hint: String? = null) {
    AppCard(modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("$days", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = contentPrimaryColor())
            Text("$label · дней подряд", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            hint?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun DashboardActionPlanPreview(
    items: List<ActionPlanItemUi>,
    onOpenAll: () -> Unit,
    onToggle: (ActionPlanItemUi) -> Unit,
    waterMl: Int,
    waterTargetMl: Int,
    stepsToday: Int,
    stepsGoal: Int,
    caloriesBurnedToday: Int,
    caloriesBurnGoal: Int,
    sleepHours: Float,
    sleepTargetHours: Float,
    caloriesToday: Int,
    caloriesTarget: Int,
    activityMinutesToday: Int,
    moodSavedToday: Boolean,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title = "План на сегодня", subtitle = "Под ваши цели · отмечается при выполнении")
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
                            tint = iconTintColor(),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                        Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        ActionPlanProgressHint.label(
                            item = item,
                            waterMl = waterMl,
                            waterTargetMl = waterTargetMl,
                            stepsToday = stepsToday,
                            stepsGoal = stepsGoal,
                            caloriesBurnedToday = caloriesBurnedToday,
                            caloriesBurnGoal = caloriesBurnGoal,
                            sleepHours = sleepHours,
                            sleepTargetHours = sleepTargetHours,
                            caloriesToday = caloriesToday,
                            caloriesTarget = caloriesTarget,
                            activityMinutesToday = activityMinutesToday,
                            moodSavedToday = moodSavedToday,
                        )?.let { hint ->
                            Text(
                                text = hint,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
        AppButton(text = "Весь план действий", onClick = onOpenAll, isSecondary = true)
    }
}

@Composable
fun DashboardWeeklySummaryBlock(
    summary: WeeklySummaryUi?,
    onShare: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (summary == null) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = "Итоги недели",
            subtitle = "Пн–вс · ${summary.periodLabel} · только дни с записями",
        )
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (!summary.hasAnyData) {
                    Text(
                        text = "На этой неделе пока нет записей. Добавьте сон, воду, еду или шаги — средние появятся здесь сразу после сохранения.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    summary.metrics.take(2).forEach { metric ->
                        WeeklyMetricTile(metric = metric, modifier = Modifier.weight(1f))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    summary.metrics.drop(2).forEach { metric ->
                        WeeklyMetricTile(metric = metric, modifier = Modifier.weight(1f))
                    }
                }
                }
                if (summary.hasAnyData && onShare != null) {
                    AppButton(text = "Поделиться прогрессом", onClick = onShare, isSecondary = true)
                }
            }
        }
    }
}

@Composable
private fun WeeklyMetricTile(
    metric: WeeklyMetricUi,
    modifier: Modifier = Modifier,
) {
    val gradient = when (metric.key) {
        "sleep" -> metricIconGradient(themedCardLavender())
        "water" -> metricIconGradient(themedCardBlue())
        "steps" -> metricIconGradient(themedCardMint(), mintTint = true)
        else -> metricIconGradient(themedCardLavender(), mintTint = true)
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(gradient))
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = metric.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = metric.averageDisplay,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = contentPrimaryColor(),
        )
        Text(
            text = metric.hint,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
            Icon(icon, null, tint = iconTintColor())
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
