package com.example.healtapp.features.hydration.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppButton

@Composable
fun QuickAddWaterButtons(
    onAdd: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppButton(
            text = "200 мл",
            onClick = { onAdd(200) }
        )
        AppButton(
            text = "250 мл",
            onClick = { onAdd(250) }
        )
        AppButton(
            text = "500 мл",
            onClick = { onAdd(500) }
        )
    }
}