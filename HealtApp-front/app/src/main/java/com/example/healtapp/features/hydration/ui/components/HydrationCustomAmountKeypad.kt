package com.example.healtapp.features.hydration.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppTextField

/**
 * Компактный ввод своего объёма: поле + шаги ±50 мл вместо огромного кейпада.
 */
@Composable
fun HydrationCustomAmountKeypad(
    value: String,
    onValueChange: (String) -> Unit,
    onAdd: () -> Unit,
    addEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val current = value.toIntOrNull() ?: 0

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Или укажите объём",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedIconButton(
                onClick = {
                    val next = (current - 50).coerceAtLeast(0)
                    onValueChange(if (next == 0) "" else next.toString())
                },
                enabled = current > 0,
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "−50 мл")
            }
            AppTextField(
                value = value,
                onValueChange = { onValueChange(it.filter(Char::isDigit).take(5)) },
                label = "мл",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
            OutlinedIconButton(
                onClick = {
                    val next = (if (current == 0) 50 else current + 50).coerceAtMost(5000)
                    onValueChange(next.toString())
                },
            ) {
                Icon(Icons.Filled.Add, contentDescription = "+50 мл")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(100, 150, 200, 330).forEach { ml ->
                AppButton(
                    text = "$ml",
                    onClick = { onValueChange(ml.toString()) },
                    isSecondary = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        AppButton(
            text = if (current > 0) "Добавить $current мл" else "Добавить",
            onClick = onAdd,
            enabled = addEnabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
