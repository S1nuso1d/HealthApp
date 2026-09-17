package com.example.healtapp.core.ui.components

import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.theme.Dimens

/** Единые размеры полей ввода и кнопок в формах. */
object AppFormMetrics {
    /** Минимальная высота кнопок и полей. Для TextField — только `heightIn(min)`, не фиксированный `height`. */
    val ControlHeight = 56.dp
    val FieldCornerRadius = Dimens.RadiusM
    val ButtonCornerRadiusLight = Dimens.RadiusM
    /** В тёмной теме кнопки больше не «брутальные» с радиусом 4dp — тот же бренд. */
    val ButtonCornerRadiusDark = Dimens.RadiusM
}
