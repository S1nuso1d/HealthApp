package com.example.healtapp.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
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
    /** Доп. отступ в конце прокручиваемого контента (например под FAB). */
    extraBottomPadding: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = screenBackgroundGradient()
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors))
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (scrollable) {
                        Modifier.verticalScroll(state = scrollState)
                    } else {
                        Modifier.fillMaxSize()
                    },
                )
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
            if (extraBottomPadding > 0.dp) {
                Spacer(Modifier.height(extraBottomPadding))
            }
        }
    }
}
