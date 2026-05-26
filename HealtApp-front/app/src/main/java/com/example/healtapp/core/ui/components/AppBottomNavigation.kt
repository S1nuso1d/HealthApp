package com.example.healtapp.core.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.navigation.BottomNavItem
import com.example.healtapp.core.ui.theme.brandingGradient
import com.example.healtapp.core.ui.theme.accentColor
import com.example.healtapp.core.ui.theme.contentSecondaryColor
import com.example.healtapp.core.ui.theme.isAppDarkTheme

@Composable
fun AppBottomNavigation(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(
        topStart = if (isAppDarkTheme()) 8.dp else 26.dp,
        topEnd = if (isAppDarkTheme()) 8.dp else 26.dp,
    )
    val borderColor = MaterialTheme.colorScheme.outline.copy(
        alpha = if (isAppDarkTheme()) 0.5f else 0.18f,
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, borderColor, shape)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val selected = item.route == currentRoute
                BottomNavItemCell(
                    item = item,
                    selected = selected,
                    onClick = { onItemClick(item) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomNavItemCell(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "nav_scale",
    )
    val pillAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "nav_pill",
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (pillAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .size(width = 52.dp, height = 36.dp)
                        .scale(pillAlpha)
                        .clip(RoundedCornerShape(if (isAppDarkTheme()) 6.dp else 14.dp))
                        .background(
                            Brush.linearGradient(
                                brandingGradient().map { it.copy(alpha = 0.35f) },
                            ),
                        ),
                )
            }
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                modifier = Modifier
                    .size(26.dp)
                    .scale(scale),
                tint = if (selected) accentColor() else contentSecondaryColor(),
            )
        }
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) accentColor() else contentSecondaryColor(),
            maxLines = 1,
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .size(width = 20.dp, height = 3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(brandingGradient()),
                    ),
            )
        } else {
            Box(modifier = Modifier.height(3.dp))
        }
    }
}
