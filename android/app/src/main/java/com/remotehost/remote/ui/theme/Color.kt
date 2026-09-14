package com.remotehost.remote.ui.theme

import androidx.compose.ui.graphics.Color

// THEME.md v2 palette — neutral near-black + a single solid red accent. No gradients
// anywhere in this theme; every accent fill is the flat `accent` color below.
val Bg = Color(0xFF08080A)
val BgDeep = Color(0xFF0A0A0B)
val Surface = Color(0xFF101012)
val SurfaceAlt = Color(0xFF0E0E10)
val SurfaceSunken = Color(0xFF0D0D0F)

val OnSurface = Color(0xFFEEEEEF)
val OnSurfaceMuted28 = Color(0x47EEEEEF)
val OnSurfaceMuted32 = Color(0x52EEEEEF)
val OnSurfaceMuted35 = Color(0x59EEEEEF)
val OnSurfaceMuted38 = Color(0x61EEEEEF)
val OnSurfaceMuted45 = Color(0x73EEEEEF)
val OnSurfaceMuted60 = Color(0x99EEEEEF)
val OnSurfaceMuted70 = Color(0xB3EEEEEF)
val OnSurfaceMuted80 = Color(0xCCEEEEEF)
val OnSurfaceMuted85 = Color(0xD9EEEEEF)

val BorderColor = Color(0x12FFFFFF) // ~.07
val BorderStrong = Color(0x1AFFFFFF) // ~.1

// The one accent color: primary buttons, active nav rule, live/good ping state, dial
// fill, rejected/subnet-lock text alternate. Red means "live / active / primary
// action" in this palette, not error.
val Accent = Color(0xFFE8283F)
val AccentLight = Color(0xFFFF8593) // links, "remove" labels, hover states
val AccentSoft = Color(0x29E8283F) // ~16% tint, for tinted backgrounds/indicators

// Medium-latency ping state / warn-level rows.
val Warn = Color(0xFFC9A227)

// "Lost" / no-signal / not-yet-connected state — neutral, not red.
val NeutralLost = Color(0x4DEEEEEF) // ~.3
