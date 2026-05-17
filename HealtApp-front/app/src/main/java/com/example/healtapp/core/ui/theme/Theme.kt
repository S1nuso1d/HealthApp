package com.example.healtapp.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = SkyPrimary,
    onPrimary = AppSurface,
    primaryContainer = Color(0xFFD2E9FF),
    onPrimaryContainer = TextPrimary,
    secondary = MintPrimary,
    onSecondary = AppSurface,
    secondaryContainer = Color(0xFFD4F5E8),
    onSecondaryContainer = TextPrimary,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = AppSurface,
    onSurface = TextPrimary,
    surfaceVariant = AppSurfaceSoft,
    onSurfaceVariant = TextSecondary,
    outline = BorderSoft,
    error = ErrorColor
)

private val DarkColors = darkColorScheme(
    primary = BrutalWhite,
    onPrimary = BrutalBlack,
    primaryContainer = Color(0xFF2E2E2E),
    onPrimaryContainer = BrutalWhite,
    secondary = BrutalAccent,
    onSecondary = BrutalBlack,
    secondaryContainer = Color(0xFF333333),
    onSecondaryContainer = Color(0xFFE5E5E5),
    tertiary = Color(0xFF737373),
    onTertiary = BrutalWhite,
    background = AppBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = AppSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = AppSurfaceSoftDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderSoftDark,
    outlineVariant = Color(0xFF2A2A2A),
    error = Color(0xFFFF6B6B),
    onError = BrutalBlack,
)

@Composable
fun HealthAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}