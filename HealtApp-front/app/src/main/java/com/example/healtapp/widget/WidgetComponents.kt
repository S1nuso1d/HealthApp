package com.example.healtapp.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import com.example.healtapp.R
import kotlin.math.roundToInt

@Composable
fun WidgetSurface(
    modifier: GlanceModifier = GlanceModifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_surface_bg))
            .padding(2.dp),
    ) {
        content()
    }
}

@Composable
fun WidgetHeroHeader(
    healthScore: Int,
    briefTitle: String,
    modifier: GlanceModifier = GlanceModifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(ImageProvider(R.drawable.widget_brand_gradient))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(text = "HealthApp", style = WidgetTextStyles.heroLabel)
                if (briefTitle.isNotBlank()) {
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        text = briefTitle,
                        style = WidgetTextStyles.heroBrief,
                        maxLines = 2,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.Horizontal.End) {
                Text(text = "Индекс", style = WidgetTextStyles.heroLabel)
                Text(
                    text = if (healthScore > 0) healthScore.toString() else "—",
                    style = WidgetTextStyles.heroScore,
                )
            }
        }
    }
}

@Composable
fun WidgetMetricRow(
    label: String,
    value: String,
    subLabel: String,
    progress: Float,
    modifier: GlanceModifier = GlanceModifier,
    onClick: Action? = null,
) {
    val clickableMod = if (onClick != null) modifier.clickable(onClick) else modifier
    Column(
        modifier = clickableMod
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = label,
                style = WidgetTextStyles.sectionLabel,
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(text = value, style = WidgetTextStyles.metricValue)
        }
        Spacer(GlanceModifier.height(4.dp))
        WidgetProgressBar(progress = progress)
        Spacer(GlanceModifier.height(2.dp))
        Text(text = subLabel, style = WidgetTextStyles.metricSub)
    }
}

@Composable
fun WidgetProgressBar(
    progress: Float,
    modifier: GlanceModifier = GlanceModifier,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val filledSegments = (clamped * 20f).roundToInt().coerceIn(0, 20)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp),
    ) {
        repeat(20) { index ->
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .height(6.dp)
                    .padding(horizontal = 1.dp)
                    .background(
                        if (index < filledSegments) WidgetColors.AccentMint else WidgetColors.Track,
                    ),
            ) {}
        }
    }
}

@Composable
fun WidgetQuickChip(
    label: String,
    onClick: Action,
    modifier: GlanceModifier = GlanceModifier,
) {
    Box(
        modifier = modifier
            .background(WidgetColors.ChipBg)
            .clickable(onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = WidgetTextStyles.chip)
    }
}

@Composable
fun WidgetActionButton(
    label: String,
    onClick: Action,
    modifier: GlanceModifier = GlanceModifier,
) {
    Box(
        modifier = modifier
            .background(WidgetColors.ButtonFill)
            .clickable(onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = WidgetTextStyles.button)
    }
}
