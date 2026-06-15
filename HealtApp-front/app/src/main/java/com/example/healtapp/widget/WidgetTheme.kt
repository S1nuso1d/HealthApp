package com.example.healtapp.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.glance.color.ColorProvider
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle

object WidgetColors {
    val Mint = 0xFF1DD1A1
    val MintDark = 0xFF10AC84
    val Sky = 0xFF54A0FF
    val SkyDark = 0xFF2E86DE

    fun fixed(hex: Long) = ColorProvider(day = Color(hex), night = Color(hex))

    val Surface = ColorProvider(
        day = Color(0xFFF8FCFF),
        night = Color(0xFF1C1C1E),
    )
    val SurfaceBorder = ColorProvider(
        day = Color(0x1A54A0FF),
        night = Color(0x33FFFFFF),
    )
    val TextPrimary = ColorProvider(
        day = Color(0xFF16324F),
        night = Color(0xFFF5F5F5),
    )
    val TextMuted = ColorProvider(
        day = Color(0xFF5A6B7D),
        night = Color(0xB3EBEBF5),
    )
    val OnHero = fixed(0xFFFFFFFF)
    val HeroMuted = fixed(0xD9FFFFFF)
    val Track = ColorProvider(
        day = Color(0xFFE8F4FC),
        night = Color(0xFF2C2C2E),
    )
    val Accent = fixed(Sky)
    val AccentMint = fixed(Mint)
    val ButtonFill = fixed(Sky)
    val ButtonText = fixed(0xFFFFFFFF)
    val ChipBg = ColorProvider(
        day = Color(0xFFEAF5FF),
        night = Color(0xFF2A2A2E),
    )
    val ChipText = ColorProvider(
        day = Color(0xFF2E86DE),
        night = Color(0xFF54A0FF),
    )
}

object WidgetTextStyles {
    val heroScore = TextStyle(
        color = WidgetColors.OnHero,
        fontSize = 36.sp,
        fontWeight = FontWeight.Bold,
    )
    val heroLabel = TextStyle(
        color = WidgetColors.HeroMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
    )
    val heroBrief = TextStyle(
        color = WidgetColors.OnHero,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
    )
    val sectionLabel = TextStyle(
        color = WidgetColors.TextMuted,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
    )
    val metricValue = TextStyle(
        color = WidgetColors.TextPrimary,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
    )
    val metricSub = TextStyle(
        color = WidgetColors.TextMuted,
        fontSize = 9.sp,
    )
    val title = TextStyle(
        color = WidgetColors.TextPrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
    )
    val waterHero = TextStyle(
        color = WidgetColors.TextPrimary,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
    )
    val chip = TextStyle(
        color = WidgetColors.ChipText,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
    )
    val button = TextStyle(
        color = WidgetColors.ButtonText,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
fun widgetProgressFraction(current: Int, goal: Int): Float =
    if (goal > 0) (current.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f

@Composable
fun widgetProgressFraction(current: Float, goal: Float): Float =
    if (goal > 0f) (current / goal).coerceIn(0f, 1f) else 0f

fun formatSteps(value: Int): String =
    "%,d".format(value).replace(',', ' ')
