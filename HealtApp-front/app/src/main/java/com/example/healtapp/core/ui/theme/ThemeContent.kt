package com.example.healtapp.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Основной текст (заголовки, значения). */
@Composable
fun contentPrimaryColor(): Color = MaterialTheme.colorScheme.onSurface

/** Подзаголовки, подписи, вторичный текст. */
@Composable
fun contentSecondaryColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant

/** Иконки в шапках и карточках — в тёмной теме светлые, не синие. */
@Composable
fun iconTintColor(): Color =
    if (isAppDarkTheme()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary

/** Акцент для прогресса / выбранного пункта. */
@Composable
fun accentColor(): Color =
    if (isAppDarkTheme()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
