package com.example.healtapp.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.theme.accentColor
import com.example.healtapp.core.ui.theme.brandingGradient
import com.example.healtapp.core.ui.theme.contentPrimaryColor
import com.example.healtapp.core.ui.theme.isAppDarkTheme

@Composable
fun GradientFormPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = if (isAppDarkTheme()) 0.35f else 0.22f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(brandingGradient().map { it.copy(alpha = 0.12f) }))
            .border(1.dp, borderColor, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
fun GradientOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isPassword: Boolean = false,
) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = if (isAppDarkTheme()) 0.35f else 0.22f)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (singleLine) {
                    Modifier.heightIn(min = AppFormMetrics.ControlHeight)
                } else {
                    Modifier.heightIn(min = AppFormMetrics.ControlHeight * 2)
                },
            ),
        label = { Text(label) },
        singleLine = singleLine,
        maxLines = maxLines,
        shape = RoundedCornerShape(AppFormMetrics.FieldCornerRadius),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else visualTransformation,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            focusedTextColor = contentPrimaryColor(),
            unfocusedTextColor = contentPrimaryColor(),
            focusedBorderColor = accentColor(),
            unfocusedBorderColor = borderColor,
            focusedLabelColor = contentPrimaryColor(),
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = if (onImeAction != null) {
            KeyboardActions(onSearch = { onImeAction() }, onDone = { onImeAction() })
        } else {
            KeyboardActions.Default
        },
    )
}
