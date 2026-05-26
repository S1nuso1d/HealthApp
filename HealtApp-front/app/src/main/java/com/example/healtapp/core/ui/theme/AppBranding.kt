package com.example.healtapp.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.healtapp.core.common.BmiHelper

/** Разбавляющий акцент в тёмной брутальной теме — холодный бетонный серый. */
val BrutalAccent = Color(0xFFB8B8B8)

val BrutalWhite = Color(0xFFF5F5F5)
val BrutalBlack = Color(0xFF0A0A0A)

@Composable
fun brandingGradient(): List<Color> =
    if (isAppDarkTheme()) {
        listOf(Color(0xFF5C5C5C), BrutalWhite, Color(0xFF8A8A8A))
    } else {
        listOf(MintPrimary, SkyPrimary)
    }

@Composable
fun heroBlockGradient(): List<Color> =
    if (isAppDarkTheme()) {
        listOf(Color(0xFF2A2A2A), BrutalBlack, Color(0xFF1A1A1A))
    } else {
        listOf(MintPrimaryDark, SkyPrimary, SkyPrimaryDark)
    }

@Composable
fun screenBackgroundGradient(): List<Color> =
    if (isAppDarkTheme()) {
        listOf(AppBackgroundTopDark, AppBackgroundBottomDark)
    } else {
        listOf(AppBackgroundTop, AppBackgroundBottom)
    }

@Composable
fun heroContentColor(): Color =
    if (isAppDarkTheme()) BrutalWhite else Color.White

@Composable
fun heroIconBackdrop(): Color =
    if (isAppDarkTheme()) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.18f)

@Composable
fun iconBadgeGradient(): List<Color> =
    if (isAppDarkTheme()) {
        listOf(Color(0xFF3A3A3A), Color(0xFF525252))
    } else {
        listOf(MintPrimary.copy(alpha = 0.28f), SkyPrimary.copy(alpha = 0.28f))
    }

@Composable
fun scoreRingGradient(): List<Color> =
    if (isAppDarkTheme()) {
        listOf(BrutalWhite, BrutalAccent, BrutalWhite)
    } else {
        listOf(Color.White, MintPrimary, Color.White)
    }

@Composable
fun brandingAccentColor(): Color =
    if (isAppDarkTheme()) BrutalWhite else MaterialTheme.colorScheme.primary

/** Кольца и диаграммы (сон, шаги, БЖУ). */
@Composable
fun chartSweepGradient(): List<Color> =
    if (isAppDarkTheme()) {
        listOf(Color(0xFF3A3A3A), BrutalWhite, Color(0xFF3A3A3A))
    } else {
        listOf(MintPrimary, SkyPrimary, MintPrimary)
    }

@Composable
fun chartSweepGradientWithSurface(surface: Color): List<Color> =
    if (isAppDarkTheme()) {
        listOf(surface, BrutalAccent, surface, Color(0xFF4A4A4A))
    } else {
        listOf(surface, SkyPrimary, MintPrimary, surface)
    }

/** Иконка в плитке дашборда / карточке метрики. */
@Composable
fun metricIconGradient(cardBase: Color, mintTint: Boolean = false): List<Color> =
    if (isAppDarkTheme()) {
        listOf(cardBase, Color(if (mintTint) 0xFF4A4A4A else 0xFF3D3D3D))
    } else {
        listOf(
            cardBase,
            if (mintTint) MintPrimary.copy(alpha = 0.35f) else SkyPrimary.copy(alpha = 0.45f),
        )
    }

/** Двухтоновый фон иконки (карточки сна, воды, HC). */
@Composable
fun cardHeaderGradient(cardBase: Color, accentAlpha: Float = 0.4f): List<Color> =
    if (isAppDarkTheme()) {
        listOf(cardBase.copy(alpha = 0.95f), Color(0xFF383838))
    } else {
        listOf(cardBase.copy(alpha = 0.95f), SkyPrimary.copy(alpha = accentAlpha))
    }

@Composable
fun cardHeaderGradientMuted(cardBase: Color): List<Color> =
    if (isAppDarkTheme()) {
        listOf(cardBase.copy(alpha = 0.85f), cardBase.copy(alpha = 0.45f))
    } else {
        listOf(cardBase.copy(alpha = 0.85f), cardBase.copy(alpha = 0.45f))
    }

@Composable
fun subtleTintGradient(): List<Color> =
    if (isAppDarkTheme()) {
        listOf(Color(0xFF333333).copy(alpha = 0.55f), Color(0xFF1F1F1F).copy(alpha = 0.45f))
    } else {
        listOf(MintPrimary.copy(alpha = 0.22f), SkyPrimary.copy(alpha = 0.22f))
    }

@Composable
fun subtleFillGradient(): List<Color> =
    if (isAppDarkTheme()) {
        listOf(Color(0xFF2A2A2A).copy(alpha = 0.65f), Color(0xFF1A1A1A).copy(alpha = 0.5f))
    } else {
        listOf(MintPrimary.copy(alpha = 0.12f), SkyPrimary.copy(alpha = 0.1f))
    }

@Composable
fun chartBarGuideColor(): Color =
    if (isAppDarkTheme()) BrutalAccent.copy(alpha = 0.5f) else MintPrimary.copy(alpha = 0.5f)

@Composable
fun chartBarFillGradient(): List<Color> =
    if (isAppDarkTheme()) {
        listOf(Color(0xFF6B6B6B), BrutalWhite)
    } else {
        listOf(SkyPrimary, MintPrimary)
    }

@Composable
fun chartBarFillGradientSoft(cardBase: Color): List<Color> =
    if (isAppDarkTheme()) {
        listOf(cardBase.copy(alpha = 0.7f), Color(0xFF525252).copy(alpha = 0.45f))
    } else {
        listOf(cardBase.copy(alpha = 0.7f), SkyPrimary.copy(alpha = 0.45f))
    }

@Composable
fun recommendationPriorityColor(priority: String): Color {
    val p = priority.lowercase()
    return when {
        p in listOf("high", "high_priority", "высокий", "high_priority_recommendation") ->
            if (isAppDarkTheme()) Color(0xFFFF6B6B) else ErrorColor
        p in listOf("medium", "средний") ->
            if (isAppDarkTheme()) BrutalAccent else WarningColor
        else ->
            if (isAppDarkTheme()) Color(0xFF9CA3AF) else MintPrimary
    }
}

@Composable
fun recommendationPriorityColorHigh(): Color =
    if (isAppDarkTheme()) Color(0xFFFF6B6B) else ErrorColor

@Composable
fun recommendationPriorityColorMedium(): Color =
    if (isAppDarkTheme()) BrutalAccent else WarningColor

@Composable
fun recommendationPriorityColorLow(): Color =
    if (isAppDarkTheme()) Color(0xFF9CA3AF) else MintPrimary

@Composable
fun bmiCategoryColor(category: BmiHelper.Category): Color =
    when (category) {
        BmiHelper.Category.NORMAL -> if (isAppDarkTheme()) BrutalWhite else MintPrimary
        BmiHelper.Category.UNDERWEIGHT -> if (isAppDarkTheme()) BrutalAccent else SkyPrimary
        BmiHelper.Category.OVERWEIGHT ->
            if (isAppDarkTheme()) Color(0xFF9CA3AF) else SkyPrimary.copy(alpha = 0.85f)
        BmiHelper.Category.OBESE -> MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
    }

@Composable
fun bmiScaleGradient(): List<Color> =
    if (isAppDarkTheme()) {
        listOf(
            Color(0xFF525252).copy(alpha = 0.35f),
            Color(0xFF9CA3AF).copy(alpha = 0.55f),
            Color(0xFF737373).copy(alpha = 0.45f),
            MaterialTheme.colorScheme.error.copy(alpha = 0.35f),
        )
    } else {
        listOf(
            SkyPrimary.copy(alpha = 0.35f),
            MintPrimary.copy(alpha = 0.55f),
            SkyPrimary.copy(alpha = 0.45f),
            MaterialTheme.colorScheme.error.copy(alpha = 0.35f),
        )
    }

@Composable
fun tipBannerColors(): Pair<Color, Color> =
    if (isAppDarkTheme()) {
        Color(0xFF2A2A2A) to BrutalAccent
    } else {
        SkyPrimary.copy(alpha = 0.08f) to SkyPrimary
    }

@Composable
fun priorityBadgeBackground(): Color =
    if (isAppDarkTheme()) Color(0xFF2A2A2A) else CardLavender

@Composable
fun chipSelectedColor(card: Color): Color =
    if (isAppDarkTheme()) card.copy(alpha = 0.95f) else card.copy(alpha = if (card == CardBlue || card == CardMint) 0.85f else 0.95f)

@Composable
fun sliderAccentColor(): Color =
    if (isAppDarkTheme()) BrutalWhite else MintPrimary
