package com.example.healtapp.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

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

/**
 * Тёмная тема — те же мята и небо, только осветлённые под тёмный фон.
 *
 * Раньше здесь была отдельная чёрно-серая палитра: тёмная тема выглядела как
 * другое приложение. Теперь роли совпадают со светлой темой (primary — небо,
 * secondary — мята), поэтому один и тот же экран узнаётся в обеих темах.
 */
private val DarkColors = darkColorScheme(
    primary = SkyOnDark,
    onPrimary = Color(0xFF03252E),
    primaryContainer = Color(0xFF1B3E52),
    onPrimaryContainer = Color(0xFFD3E9FF),
    secondary = MintOnDark,
    onSecondary = Color(0xFF00251C),
    secondaryContainer = Color(0xFF14453A),
    onSecondaryContainer = Color(0xFFCFF6EA),
    tertiary = Color(0xFFA8C7CB),
    onTertiary = Color(0xFF0D2B30),
    background = AppBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = AppSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = AppSurfaceSoftDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderSoftDark,
    outlineVariant = Color(0xFF1E3A40),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF3A0906),
)

@Composable
fun HealthAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    /**
     * Material You: подхватывать цвета обоев системы.
     *
     * Выключено по умолчанию — мята и небо это бренд приложения, и подменять их
     * обоями стоит только по явному желанию пользователя.
     */
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        // Динамическая палитра появилась в Android 12; на более старых версиях
        // системного источника цветов просто нет.
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
