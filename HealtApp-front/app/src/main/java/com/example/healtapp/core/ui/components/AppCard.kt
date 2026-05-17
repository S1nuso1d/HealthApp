package com.example.healtapp.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.theme.isAppDarkTheme

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (isAppDarkTheme()) 12.dp else 26.dp),
        border = BorderStroke(
            width = if (isAppDarkTheme()) 1.5.dp else 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(
                alpha = if (isAppDarkTheme()) 0.45f else 0.14f,
            ),
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(
                alpha = if (isAppDarkTheme()) 1f else 0.97f,
            ),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        val interaction = remember { MutableInteractionSource() }
        val columnModifier = Modifier
            .fillMaxWidth()
            .padding(18.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = ripple(),
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
        Column(modifier = columnModifier, content = content)
    }
}