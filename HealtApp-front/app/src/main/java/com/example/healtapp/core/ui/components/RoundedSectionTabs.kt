package com.example.healtapp.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.animation.appSelectionScale
import com.example.healtapp.core.ui.theme.accentColor
import com.example.healtapp.core.ui.theme.brandingGradient
import com.example.healtapp.core.ui.theme.contentSecondaryColor
import com.example.healtapp.core.ui.theme.isAppDarkTheme

data class RoundedTabItem(
    val index: Int,
    val label: String,
    val icon: ImageVector? = null,
)

@Composable
fun RoundedSectionTabs(
    tabs: List<RoundedTabItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tabs.forEach { tab ->
            val isSelected = selected == tab.index
            val shape = RoundedCornerShape(20.dp)
            val borderColor = MaterialTheme.colorScheme.outline.copy(
                alpha = if (isAppDarkTheme()) 0.25f else 0.12f,
            )
            Surface(
                onClick = { onSelect(tab.index) },
                shape = shape,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                },
                modifier = Modifier
                    .clip(shape)
                    .appSelectionScale(isSelected, selectedScale = 1.03f)
                    .then(
                        if (isSelected) {
                            Modifier.background(
                                Brush.linearGradient(
                                    brandingGradient().map { it.copy(alpha = 0.18f) },
                                ),
                                shape,
                            )
                        } else {
                            Modifier.border(1.dp, borderColor, shape)
                        },
                    ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    tab.icon?.let { icon ->
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) accentColor() else contentSecondaryColor(),
                        )
                    }
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) accentColor() else contentSecondaryColor(),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
