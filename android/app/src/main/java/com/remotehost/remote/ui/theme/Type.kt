package com.remotehost.remote.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Plus Jakarta Sans / IBM Plex Mono .ttf files aren't available in this sandbox; these
// fall back to the platform defaults per THEME.md's own fallback guidance. Drop the
// real files into res/font and point these FontFamily values at them later.
val AppFontFamily = FontFamily.Default
val MonoFontFamily = FontFamily.Monospace

val AppTypography = Typography(
    headlineSmall = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold, fontSize = 19.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    bodyLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodyMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp),
    bodySmall = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 11.5.sp),
    labelSmall = TextStyle(fontFamily = MonoFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 10.5.sp),
    labelMedium = TextStyle(fontFamily = MonoFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
)
