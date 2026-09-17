package com.example.healtapp.core.ui.theme

import androidx.compose.ui.graphics.Color

// Modern Premium Palette
val MintPrimary = Color(0xFF1DD1A1)
val MintPrimaryDark = Color(0xFF10AC84)
val SkyPrimary = Color(0xFF54A0FF)
val SkyPrimaryDark = Color(0xFF2E86DE)

val AppBackgroundTop = Color(0xFFF2F2F7)
val AppBackgroundBottom = Color(0xFFF2F2F7)
val AppBackground = Color(0xFFF2F2F7)

/**
 * Тёмная тема — та же мятно-небесная палитра, а не отдельный «брутальный» бренд.
 *
 * Фон не чисто чёрный: холодный тёмно-бирюзовый оттенок сохраняет характер мяты
 * и неба, на нём мятные акценты читаются как продолжение светлой темы, а не как
 * другое приложение.
 */
val AppBackgroundTopDark = Color(0xFF07161A)
val AppBackgroundBottomDark = Color(0xFF0A1D22)
val AppBackgroundDark = Color(0xFF07161A)

val AppSurface = Color(0xFFFFFFFF)
val AppSurfaceSoft = Color(0xFFFAFAFA)

val AppSurfaceDark = Color(0xFF10262C)
val AppSurfaceSoftDark = Color(0xFF17323A)

/** Осветлённые мята и небо: на тёмном фоне исходные тона теряют контраст. */
val MintOnDark = Color(0xFF4FE3BE)
val MintOnDarkSoft = Color(0xFF2BA98D)
val SkyOnDark = Color(0xFF7CC0FF)
val SkyOnDarkSoft = Color(0xFF3E82C4)

val TextPrimary = Color(0xFF1C1C1E)
val TextSecondary = Color(0xFF8E8E93)
val TextHint = Color(0xFFC7C7CC)

val TextPrimaryDark = Color(0xFFEAF6F4)
val TextSecondaryDark = Color(0xFFA7C2C4)

val BorderSoft = Color(0x14000000)
val DividerSoft = Color(0x0F000000)

/** Обводки в тёмной теме слегка мятные — тот же приём, что и на светлых карточках. */
val BorderSoftDark = Color(0x334FE3BE)
val DividerSoftDark = Color(0x1F7CC0FF)

val ErrorColor = Color(0xFFFF3B30)
val SuccessColor = Color(0xFF34C759)
val WarningColor = Color(0xFFFF9500)

val CardBlue = Color(0xFFEAF5FF)
val CardMint = Color(0xFFEBFFF5)
val CardLavender = Color(0xFFF4F0FF)

/** Подложки карточек в тёмной теме: те же три акцента, но приглушённые. */
val CardBlueDark = Color(0xFF16323F)
val CardMintDark = Color(0xFF12332F)
val CardLavenderDark = Color(0xFF232541)