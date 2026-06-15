package com.example.healtapp.features.hydration.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.data.preferences.HydrationPrefs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAddWaterButtons(
    defaultAmounts: List<Int>,
    customAmounts: List<Int>,
    onAdd: (Int) -> Unit,
    onAddCustomButton: (Int) -> Unit,
    onRemoveCustomButton: (Int) -> Unit,
    canAddMoreCustom: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        defaultAmounts.forEach { ml ->
            AppButton(
                text = "+$ml мл",
                onClick = { onAdd(ml) },
                isSecondary = true,
                enabled = enabled,
                modifier = Modifier,
            )
        }
        customAmounts.forEach { ml ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppButton(
                    text = "+$ml мл",
                    onClick = { onAdd(ml) },
                    isSecondary = true,
                    enabled = enabled,
                    modifier = Modifier,
                )
                IconButton(
                    onClick = { onRemoveCustomButton(ml) },
                    enabled = enabled,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Удалить кнопку $ml мл",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (canAddMoreCustom) {
            AppButton(
                text = "Своя кнопка",
                onClick = { showAddDialog = true },
                isSecondary = true,
                enabled = enabled,
                modifier = Modifier,
            )
        }
    }

    if (showAddDialog) {
        AddCustomQuickButtonDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { ml ->
                onAddCustomButton(ml)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun AddCustomQuickButtonDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var amountInput by remember { mutableStateOf("") }
    val parsed = amountInput.toIntOrNull()
    val isValid = parsed != null && parsed in HydrationPrefs.MIN_ML..HydrationPrefs.MAX_ML

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Своя кнопка") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it.filter { ch -> ch.isDigit() }.take(4) },
                    label = "Объём, мл",
                    keyboardType = KeyboardType.Number,
                )
                Text(
                    text = "От ${HydrationPrefs.MIN_ML} до ${HydrationPrefs.MAX_ML} мл",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onConfirm) },
                enabled = isValid,
            ) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}
