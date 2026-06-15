package com.example.healtapp.features.dashboard.ui.alt

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object DashboardAltColors {
    val Paper = Color(0xFFF3EDE4)
    val PaperDark = Color(0xFF141210)
    val Ink = Color(0xFF121212)
    val InkMuted = Color(0xFF5C574F)
    val Surface = Color(0xFFFFFCF7)
    val SurfaceDark = Color(0xFF1E1C19)
    val Terracotta = Color(0xFFC45C26)
    val Navy = Color(0xFF1B4965)
    val Sage = Color(0xFF4A6741)
    val Border = Color(0xFF121212)
    val BorderDark = Color(0xFFE8E0D4)
}

private val AltLightScheme = lightColorScheme(
    primary = DashboardAltColors.Terracotta,
    onPrimary = Color.White,
    secondary = DashboardAltColors.Navy,
    onSecondary = Color.White,
    tertiary = DashboardAltColors.Sage,
    background = DashboardAltColors.Paper,
    onBackground = DashboardAltColors.Ink,
    surface = DashboardAltColors.Surface,
    onSurface = DashboardAltColors.Ink,
    onSurfaceVariant = DashboardAltColors.InkMuted,
    outline = DashboardAltColors.Border.copy(alpha = 0.35f),
)

private val AltDarkScheme = darkColorScheme(
    primary = Color(0xFFE07A45),
    onPrimary = DashboardAltColors.Ink,
    secondary = Color(0xFF5B8FB9),
    onSecondary = DashboardAltColors.Ink,
    tertiary = Color(0xFF7FA874),
    background = DashboardAltColors.PaperDark,
    onBackground = DashboardAltColors.BorderDark,
    surface = DashboardAltColors.SurfaceDark,
    onSurface = DashboardAltColors.BorderDark,
    onSurfaceVariant = Color(0xFF9E968A),
    outline = DashboardAltColors.BorderDark.copy(alpha = 0.35f),
)

private val AltTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.75).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 1.2.sp,
        lineHeight = 18.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 1.4.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        letterSpacing = 1.sp,
    ),
)

@Composable
fun DashboardAltTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) AltDarkScheme else AltLightScheme,
        typography = AltTypography,
        content = content,
    )
}
