package com.remotehost.remote.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// THEME.md palette, translated 1:1.
val Bg = Color(0xFF0A0A0F)
val BgDeep = Color(0xFF07070A)
val Surface = Color(0xFF121219)
val SurfaceAlt = Color(0xFF0F0F15)
val SurfaceSunken = Color(0xFF0D0D13)
val Titlebar = Color(0xFF15151C)

val OnSurface = Color(0xFFECE9F2)
val OnSurfaceMuted38 = Color(0x61ECE9F2)
val OnSurfaceMuted45 = Color(0x73ECE9F2)
val OnSurfaceMuted60 = Color(0x99ECE9F2)

val BorderColor = Color(0x12FFFFFF)
val BorderStrong = Color(0x1AFFFFFF)

val AccentStart = Color(0xFF8B5CF6)
val AccentEnd = Color(0xFFD946EF)
val AccentSoft = Color(0x298B5CF6)
val AccentText = Color(0xFFC4B5FD)
val AccentTextBright = Color(0xFFDDD4FF)

val SuccessGreen = Color(0xFF7EE787)
val WarningYellow = Color(0xFFF5C451)
val DangerRed = Color(0xFFF0708F)
val DangerSoft = Color(0xFFF0A8C0)

fun accentGradientBrush(): Brush = Brush.linearGradient(listOf(AccentStart, AccentEnd))

fun neutralGradientBrush(): Brush = Brush.linearGradient(listOf(OnSurfaceMuted45, OnSurface))
