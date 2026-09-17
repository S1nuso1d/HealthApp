package com.example.healtapp.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.healtapp.core.common.BmiHelper

/**
 * Все места, где вид зависит от темы, собраны здесь.
 *
 * Тёмные ветки раньше были чёрно-серыми («брутальная» тема) — приложение
 * выглядело как два разных продукта. Теперь тёмная тема строится на тех же
 * мяте и небе, только осветлённых под тёмный фон: градиенты идут в том же
 * направлении, роли цветов совпадают со светлой темой.
 */

@Composable
fun brandingGradient(): List<Color> =
    if (isAppDarkTheme()) {
        listOf(MintOnDark, SkyOnDark)
    } else {
        listOf(MintPrimary, SkyPrimary)
    }

@Composable
fun heroBlockGradient(): List<Color> =
    if (isAppDarkTheme()) {
        listOf(MintOnDarkSoft, SkyOnDarkSoft, Color(0xFF1B4A6B))
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
fun heroContentColor(): Color = Color.White

@Composable
fun heroIconBackdrop(): Color =
    if (isAppDarkTheme()) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.18f)

@Composable
fun iconBadgeGradient(): List<Color> =
    if (isAppDarkTheme()) {
        listOf(MintOnDark.copy(alpha = 0.24f), SkyOnDark.copy(alpha = 0.24f))
    } else {
        listOf(MintPrimary.copy(alpha = 0.28f), SkyPrimary.copy(alpha = 0.28f))
    }

@Composable
fun scoreRingGradient(): List<Color> =
    if (isAppDarkTheme()) {
        listOf(Color.White, MintOnDark, Color.White)
    } else {
        listOf(Color.White, MintPrimary, Color.White)
    }

@Composable
fun brandingAccentColor(): Color = MaterialTheme.colorScheme.primary

/** Кольца и диаграммы (сон, шаги, БЖУ). */
@Composable
fun chartSweepGradient(): List<Color> =
    if (isAppDarkTheme()) {
        listOf(MintOnDark, SkyOnDark, MintOnDark)
    } else {
        listOf(MintPrimary, SkyPrimary, MintPrimary)
    }

@Composable
fun chartSweepGradientWithSurface(surface: Color): List<Color> =
    if (isAppDarkTheme()) {
        listOf(surface, SkyOnDark, MintOnDark, surface)
    } else {
        listOf(surface, SkyPrimary, MintPrimary, surface)
    }

/** Иконка в плитке дашборда / карточке метрики. */
@Composable
fun metricIconGradient(cardBase: Color, mintTint: Boolean = false): List<Color> =
    if (isAppDarkTheme()) {
        listOf(
            cardBase,
            if (mintTint) MintOnDark.copy(alpha = 0.35f) else SkyOnDark.copy(alpha = 0.45f),
        )
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
        listOf(cardBase.copy(alpha = 0.95f), SkyOnDark.copy(alpha = accentAlpha))
    } else {
        listOf(cardBase.copy(alpha = 0.95f), SkyPrimary.copy(alpha = accentAlpha))
    }

@Composable
fun cardHeaderGradientMuted(cardBase: Color): List<Color> =
    listOf(cardBase.copy(alpha = 0.85f), cardBase.copy(alpha = 0.45f))

@Composable
fun subtleTintGradient(): List<Color> =
    if (isAppDarkTheme()) {
        listOf(MintOnDark.copy(alpha = 0.18f), SkyOnDark.copy(alpha = 0.18f))
    } else {
        listOf(MintPrimary.copy(alpha = 0.22f), SkyPrimary.copy(alpha = 0.22f))
    }

@Composable
fun subtleFillGradient(): List<Color> =
    if (isAppDarkTheme()) {
        listOf(MintOnDark.copy(alpha = 0.1f), SkyOnDark.copy(alpha = 0.08f))
    } else {
        listOf(MintPrimary.copy(alpha = 0.12f), SkyPrimary.copy(alpha = 0.1f))
    }

@Composable
fun chartBarGuideColor(): Color =
    if (isAppDarkTheme()) MintOnDark.copy(alpha = 0.45f) else MintPrimary.copy(alpha = 0.5f)

@Composable
fun chartBarFillGradient(): List<Color> =
    if (isAppDarkTheme()) {
        listOf(SkyOnDark, MintOnDark)
    } else {
        listOf(SkyPrimary, MintPrimary)
    }

@Composable
fun chartBarFillGradientSoft(cardBase: Color): List<Color> =
    if (isAppDarkTheme()) {
        listOf(cardBase.copy(alpha = 0.7f), SkyOnDark.copy(alpha = 0.45f))
    } else {
        listOf(cardBase.copy(alpha = 0.7f), SkyPrimary.copy(alpha = 0.45f))
    }

/** Подложка цветной карточки: в тёмной теме — приглушённый аналог светлой. */
@Composable
fun cardTint(light: Color): Color =
    if (!isAppDarkTheme()) {
        light
    } else {
        when (light) {
            CardMint -> CardMintDark
            CardLavender -> CardLavenderDark
            else -> CardBlueDark
        }
    }

@Composable
fun recommendationPriorityColor(priority: String): Color {
    val p = priority.lowercase()
    return when {
        p in listOf("high", "high_priority", "высокий", "high_priority_recommendation") ->
            recommendationPriorityColorHigh()
        p in listOf("medium", "средний") -> recommendationPriorityColorMedium()
        else -> recommendationPriorityColorLow()
    }
}

@Composable
fun recommendationPriorityColorHigh(): Color = MaterialTheme.colorScheme.error

@Composable
fun recommendationPriorityColorMedium(): Color =
    if (isAppDarkTheme()) Color(0xFFFFC46B) else WarningColor

@Composable
fun recommendationPriorityColorLow(): Color =
    if (isAppDarkTheme()) MintOnDark else MintPrimary

@Composable
fun bmiCategoryColor(category: BmiHelper.Category): Color =
    when (category) {
        BmiHelper.Category.NORMAL -> if (isAppDarkTheme()) MintOnDark else MintPrimary
        BmiHelper.Category.UNDERWEIGHT -> if (isAppDarkTheme()) SkyOnDark else SkyPrimary
        BmiHelper.Category.OVERWEIGHT ->
            if (isAppDarkTheme()) Color(0xFFFFC46B) else SkyPrimary.copy(alpha = 0.85f)
        BmiHelper.Category.OBESE -> MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
    }

@Composable
fun bmiScaleGradient(): List<Color> {
    val sky = if (isAppDarkTheme()) SkyOnDark else SkyPrimary
    val mint = if (isAppDarkTheme()) MintOnDark else MintPrimary
    return listOf(
        sky.copy(alpha = 0.35f),
        mint.copy(alpha = 0.55f),
        sky.copy(alpha = 0.45f),
        MaterialTheme.colorScheme.error.copy(alpha = 0.35f),
    )
}

@Composable
fun tipBannerColors(): Pair<Color, Color> =
    if (isAppDarkTheme()) {
        SkyOnDark.copy(alpha = 0.14f) to SkyOnDark
    } else {
        SkyPrimary.copy(alpha = 0.08f) to SkyPrimary
    }

@Composable
fun priorityBadgeBackground(): Color =
    if (isAppDarkTheme()) CardLavenderDark else CardLavender

@Composable
fun chipSelectedColor(card: Color): Color {
    val base = cardTint(card)
    return if (isAppDarkTheme()) {
        base
    } else {
        base.copy(alpha = if (card == CardBlue || card == CardMint) 0.85f else 0.95f)
    }
}

@Composable
fun sliderAccentColor(): Color =
    if (isAppDarkTheme()) MintOnDark else MintPrimary
