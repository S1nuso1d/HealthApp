package com.example.healtapp.features.profile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.theme.ThemeMode
import com.example.healtapp.core.ui.theme.chipSelectedColor
import com.example.healtapp.core.ui.theme.themedCardMint

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileThemeSelector(
    selected: ThemeMode,
    onSelected: (ThemeMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Тема приложения",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "Светлая, тёмная или как в системе Android",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ThemeModeChip(
                label = "Светлая",
                icon = Icons.Filled.LightMode,
                selected = selected == ThemeMode.LIGHT,
                onClick = { onSelected(ThemeMode.LIGHT) },
            )
            ThemeModeChip(
                label = "Тёмная",
                icon = Icons.Filled.DarkMode,
                selected = selected == ThemeMode.DARK,
                onClick = { onSelected(ThemeMode.DARK) },
            )
            ThemeModeChip(
                label = "Системная",
                icon = Icons.Outlined.BrightnessAuto,
                selected = selected == ThemeMode.SYSTEM,
                onClick = { onSelected(ThemeMode.SYSTEM) },
            )
        }
    }
}

@Composable
private fun ThemeModeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = chipSelectedColor(themedCardMint()),
        ),
    )
}
