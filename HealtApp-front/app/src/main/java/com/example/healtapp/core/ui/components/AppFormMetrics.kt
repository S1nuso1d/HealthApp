package com.example.healtapp.core.ui.components

import androidx.compose.ui.unit.dp

/** Единые размеры полей ввода и кнопок в формах. */
object AppFormMetrics {
    /** Минимальная высота кнопок и полей. Для TextField — только `heightIn(min)`, не фиксированный `height`. */
    val ControlHeight = 56.dp
    val FieldCornerRadius = 18.dp
    val ButtonCornerRadiusLight = 18.dp
    val ButtonCornerRadiusDark = 4.dp
}
