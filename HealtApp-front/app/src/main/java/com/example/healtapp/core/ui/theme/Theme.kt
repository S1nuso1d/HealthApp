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
    primary = SkyPrimary,
    onPrimary = Color(0xFF06101B),
    primaryContainer = Color(0xFF1A3550),
    onPrimaryContainer = TextPrimaryDark,
    secondary = MintPrimary,
    onSecondary = Color(0xFF06101B),
    secondaryContainer = Color(0xFF15352A),
    onSecondaryContainer = TextPrimaryDark,
    background = AppBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = AppSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = AppSurfaceSoftDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderSoftDark,
    error = ErrorColor,
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