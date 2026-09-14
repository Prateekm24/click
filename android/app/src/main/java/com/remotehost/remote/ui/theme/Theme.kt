package com.remotehost.remote.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// The mock is dark-only by design (no light variant in THEME.md), so this theme does
// not branch on system light/dark setting.
private val RemoteDarkColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = Accent,
    onSecondary = Color.White,
    tertiary = Warn,
    onTertiary = Color.Black,
    background = Bg,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceAlt,
    onSurfaceVariant = OnSurfaceMuted60,
    error = Accent,
    onError = Color.White,
    outline = BorderStrong,
)

@Composable
fun RemoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RemoteDarkColorScheme,
        typography = AppTypography,
        content = content,
    )
}
