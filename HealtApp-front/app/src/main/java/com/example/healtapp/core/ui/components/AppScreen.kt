package com.example.healtapp.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.theme.AppBackgroundBottom
import com.example.healtapp.core.ui.theme.AppBackgroundBottomDark
import com.example.healtapp.core.ui.theme.AppBackgroundTop
import com.example.healtapp.core.ui.theme.AppBackgroundTopDark

@Composable
fun AppScreen(
    title: String? = null,
    subtitle: String? = null,
    headerIcon: ImageVector? = null,
    scrollable: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val colors = if (isDark) {
        listOf(AppBackgroundTopDark, AppBackgroundBottomDark)
    } else {
        listOf(AppBackgroundTop, AppBackgroundBottom)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors))
            .statusBarsPadding()
            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (title != null) {
            ScreenHeader(
                title = title,
                subtitle = subtitle,
                icon = headerIcon,
            )
        }
        content()
    }
}

