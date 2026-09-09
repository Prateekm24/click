package com.remotehost.remote.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// The mock is dark-only by design (no light variant in THEME.md), so this theme does
// not branch on system light/dark setting.
private val RemoteDarkColorScheme = darkColorScheme(
    primary = AccentStart,
    onPrimary = Color.White,
    secondary = AccentEnd,
    onSecondary = Color.White,
    tertiary = AccentEnd,
    onTertiary = Color.White,
    background = Bg,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceAlt,
    onSurfaceVariant = OnSurfaceMuted60,
    error = DangerRed,
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
