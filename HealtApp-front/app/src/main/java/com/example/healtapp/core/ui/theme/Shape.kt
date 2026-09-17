package com.example.healtapp.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

/**
 * Скругления берутся из [Dimens], а не задаются в компонентах Material по
 * умолчанию. Иначе диалоги, меню и нижние листы приходят со своими радиусами
 * и выбиваются из карточек приложения.
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(Dimens.SpaceS),
    small = RoundedCornerShape(Dimens.RadiusS),
    medium = RoundedCornerShape(Dimens.RadiusM),
    large = RoundedCornerShape(Dimens.RadiusL),
    extraLarge = RoundedCornerShape(Dimens.RadiusXl),
)
