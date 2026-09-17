package com.example.healtapp.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
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
import com.example.healtapp.core.ui.animation.AppAppearOnce
import com.example.healtapp.core.ui.animation.appPressScale
import com.example.healtapp.core.ui.theme.Dimens
import com.example.healtapp.core.ui.theme.isAppDarkTheme

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    animateEnter: Boolean = true,
    enterDelayMillis: Int = 0,
    quiet: Boolean = false,
    highlight: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val outline = when {
        highlight -> MaterialTheme.colorScheme.primary.copy(alpha = if (isAppDarkTheme()) 0.55f else 0.38f)
        quiet -> MaterialTheme.colorScheme.outline.copy(alpha = if (isAppDarkTheme()) 0.18f else 0.06f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = if (isAppDarkTheme()) 0.45f else 0.14f)
    }
    val card: @Composable () -> Unit = {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.RadiusL),
            border = BorderStroke(
                width = Dimens.BorderWidth,
                color = outline,
            ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(
                    alpha = when {
                        isAppDarkTheme() -> 1f
                        quiet -> 0.88f
                        else -> 0.97f
                    },
                ),
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = Dimens.CardElevation),
        ) {
            val interaction = remember { MutableInteractionSource() }
            val columnModifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.SpaceL)
                .then(
                    if (onClick != null) {
                        Modifier
                            .appPressScale(interaction)
                            .clickable(
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

    if (animateEnter) {
        AppAppearOnce(
            modifier = modifier.fillMaxWidth(),
            delayMillis = enterDelayMillis,
            content = card,
        )
    } else {
        Box(modifier = modifier.fillMaxWidth()) {
            card()
        }
    }
}
