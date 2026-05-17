package com.example.healtapp.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

val CardBlueDark = Color(0xFF222222)
val CardMintDark = Color(0xFF1C1C1C)
val CardLavenderDark = Color(0xFF282828)

@Composable
fun isAppDarkTheme(): Boolean =
    MaterialTheme.colorScheme.background.luminance() < 0.5f

@Composable
fun themedCardBlue(): Color = if (isAppDarkTheme()) CardBlueDark else CardBlue

@Composable
fun themedCardMint(): Color = if (isAppDarkTheme()) CardMintDark else CardMint

@Composable
fun themedCardLavender(): Color = if (isAppDarkTheme()) CardLavenderDark else CardLavender
