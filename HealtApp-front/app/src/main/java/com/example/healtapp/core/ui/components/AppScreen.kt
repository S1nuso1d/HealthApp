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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.theme.screenBackgroundGradient

@Composable
fun AppScreen(
    title: String? = null,
    subtitle: String? = null,
    headerIcon: ImageVector? = null,
    headerLeading: (@Composable () -> Unit)? = null,
    onHeaderLeadingClick: (() -> Unit)? = null,
    onNavigateBack: (() -> Unit)? = null,
    scrollable: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = screenBackgroundGradient()

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
                icon = if (headerLeading == null) headerIcon else null,
                leadingContent = headerLeading,
                onLeadingClick = onHeaderLeadingClick,
                onBackClick = onNavigateBack,
            )
        }
        content()
    }
}

