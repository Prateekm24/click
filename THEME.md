# Visual theme — shared by Android app and Windows Host UI

Extracted from the approved mockups (`Remote Control - App Mocks.dc.html`). Dark,
high-contrast, violet→magenta accent gradient. Reproduce these values as closely as
each platform's UI toolkit allows — do not invent a different palette.

## Colors

| Token              | Hex / value                          | Usage |
|---------------------|--------------------------------------|-------|
| `bg`                | `#0a0a0f`                             | App/screen background |
| `bgDeep`            | `#07070a`                             | Outermost background (Host titlebar area) |
| `surface`           | `#121219`                             | Cards, list rows |
| `surfaceAlt`        | `#0f0f15`                             | Sidebar / secondary panel background |
| `surfaceSunken`     | `#0d0d13`                             | Bottom nav bar background |
| `titlebar`          | `#15151c`                             | Host window titlebar / tray menu bg |
| `onSurface`         | `#ece9f2`                             | Primary text (near-white, slightly violet) |
| `onSurfaceMuted38`  | `rgba(236,233,242,0.38)`              | Section labels / eyebrow text |
| `onSurfaceMuted45`  | `rgba(236,233,242,0.45)`              | Body/secondary text |
| `onSurfaceMuted60`  | `rgba(236,233,242,0.6)`               | Inactive nav / muted labels |
| `border`            | `rgba(255,255,255,0.07)`              | Card borders |
| `borderStrong`      | `rgba(255,255,255,0.1)`               | Emphasized borders, dividers |
| `accentStart`       | `#8b5cf6` (violet)                    | Gradient start |
| `accentEnd`         | `#d946ef` (magenta)                   | Gradient end |
| `accentGradient`    | `linear-gradient(140deg, #8b5cf6, #d946ef)` | Primary buttons, active slider fill, play button, FAB-like accents |
| `accentSoft`        | `rgba(139,92,246,0.16)`               | Selected nav-item background, hero-card tint |
| `accentText`        | `#c4b5fd`                             | Accent-colored text (active tab, numbers) |
| `accentTextBright`  | `#ddd4ff`                             | Emphasized accent text (button labels) |
| `success`           | `#7ee787`                             | Connected dot |
| `warning`           | `#f5c451`                             | Reconnecting dot / warn log lines |
| `danger`            | `#f0708f`                             | Offline / error dot |
| `dangerSoft`        | `#f0a8c0`                             | Disconnect button text |

## Typography

- UI / headings / labels: **Plus Jakarta Sans** (weights 400/500/600/700).
  - Android: bundle as a Compose `FontFamily` if the .ttf files are available;
    otherwise fall back to the platform default sans-serif (`FontFamily.Default`) —
    do not block the build on missing font files.
  - Windows: reference "Plus Jakarta Sans" in XAML with a `FontFamily` fallback chain
    `"Plus Jakarta Sans, Segoe UI, sans-serif"` so it degrades gracefully if the font
    isn't installed on the machine.
- Numeric readouts / mono labels / addresses / log lines: **IBM Plex Mono**. Same
  fallback approach — degrade to `FontFamily.Monospace` (Android) or `"IBM Plex Mono,
  Consolas, monospace"` (Windows) if unavailable.

## Shape & spacing

- Large rounded corners throughout: 18–20px on cards/screens, 26px on the biggest
  containers, full pill (999px / 50%) on sliders, toggle tracks, and round dots.
- Card padding ~16–18px. Section gaps ~14px.
- Sliders: 8px tall pill track, 22px circular thumb, soft glow ring around the thumb
  (`box-shadow` equivalent — use elevation/shadow with the accent color at low opacity).
- Toggles: 42×24px pill track, 18px circular white knob that slides between the two
  ends; track color is `accentGradient` when on, `rgba(255,255,255,.14)` when off.

## Key screen patterns to reproduce

- **Volume card**: gradient-tinted hero card (`accentSoft` → transparent, radial/linear),
  big mono readout top-right, full-width slider, row of [Mute | − | +] below.
- **Brightness card**: plain `surface` card, same slider treatment but white/neutral
  fill instead of the accent gradient.
- **Now playing card**: square "art" placeholder tile, title + source text, thin
  progress bar, 5-across control row (−10s / prev / play·pause / next / +10s) where the
  center play button is the accent gradient pill and the rest are flat `surface` tiles.
- **Connection pill** (top-right of the phone header): small status dot (color per
  state) + mono status text, pill-shaped, tap to reconnect/cycle in dev builds.
- **Bottom nav**: 4 equal columns (Dashboard / Apps / Touchpad / Settings), simple line
  icons, active tab colored `accentText`, inactive `onSurfaceMuted38`.
- **Host window**: fixed left sidebar (~212px) with nav items (Status / App shortcuts /
  Pairing / General) plus a connection status card pinned to the bottom of the sidebar;
  main content area on the right per-section.

Don't chase pixel-perfect parity with the HTML mock — match the *character*: dark,
violet/magenta gradient accent, generous rounding, mono numerals, high contrast text
on near-black surfaces.
