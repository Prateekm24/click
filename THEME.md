# Visual theme — shared by Android app and Windows Host UI (v2)

**This supersedes the original violet/magenta theme.** The design direction changed to
a neutral, near-black palette with a single solid red accent reserved for live state and
primary actions — no more gradients. Source mockups (read these for exact values before
touching UI code):

- `C:\Users\prateek.mishra\Downloads\RemotePhonePro.dc.html` — phone "Now controlling"
  dial screen + media/keys bottom sheet.
- `C:\Users\prateek.mishra\Downloads\HostWindowQuiet.dc.html` — Host window (Overview /
  Traffic / Launchables / Pairing panes, ticker, stat strip).

## Colors

| Token              | Hex / value                          | Usage |
|---------------------|--------------------------------------|-------|
| `bg`                | `#08080a`                             | Outermost background |
| `bgDeep`            | `#0a0a0b`                             | Phone screen background |
| `surface`           | `#0f0f11` / `#101012`                 | Cards, feature rows, tiles |
| `surfaceAlt`        | `#0e0e10`                             | Host sidebar / ticker bar background |
| `titlebar`          | `#131315`                             | Host window titlebar |
| `hostShell`         | `#0b0b0c`                             | Host window outer shell |
| `onSurface`         | `#eeeeef`                             | Primary text |
| `onSurfaceMuted28` … `85` | `rgba(238,238,239, .28 .. .85)` | Secondary/muted text — pick the exact opacity used at each spot in the mock rather than reusing one value everywhere |
| `border`            | `rgba(255,255,255,.06)` / `.07`       | Card/row borders |
| `borderStrong`      | `rgba(255,255,255,.09)` / `.1`        | Emphasized borders (pairing panel, toggles) |
| `accent`            | `#e8283f`                             | The one accent color: primary buttons, active nav rule, live/good ping state, dial fill, rejected/subnet-lock text alternate |
| `accentLight`       | `#ff8593`                             | Links, "remove" labels, hover states — the lighter red |
| `warn`              | `#c9a227`                             | Medium-latency ping state, warn-level log rows |
| `neutralLost`       | `rgba(238,238,239,.3)`                | "Lost"/no-signal ping state |

There is **no gradient** anywhere in v2 — every accent fill is the flat `#e8283f`. Red
specifically means "live / active / primary action," not error — e.g. the fastest ping
band is red, not the slowest (amber is the warn band, "LOST" is neutral gray).

## Typography

Same as before: **Plus Jakarta Sans** for UI text/headings, **IBM Plex Mono** for
numerals, addresses, log lines, ticker text. Same fallback rule: Android falls back to
`FontFamily.Default`/`FontFamily.Monospace` if no bundled `.ttf`; WPF references
`"Plus Jakarta Sans, Segoe UI, sans-serif"` / `"IBM Plex Mono, Consolas, monospace"`.

## Shape

Noticeably tighter/quieter than v1: cards and rows use **10–16px** corner radius (not
18–26px), Host sidebar nav items **7px**, pills still full round. Borders are subtler
(`.05`–`.09` opacity) and there's more reliance on plain flat dark surfaces than on
tinted/gradient cards.

## Screens (read the source mocks for exact structure)

- **Phone home ("Now controlling")**: a big radial dial (SVG arc, ~270° sweep) is the
  primary volume control — drag anywhere on it to set volume by angle from center, not a
  linear slider. Center shows a large mono volume readout. Below: Mute + "Media & keys"
  buttons opening a bottom sheet. A "NIGHT" row is the brightness/dim control — a thin
  linear slider, and as it increases a full-screen black overlay (opacity = dim/160)
  actually dims the phone's own display to simulate the room going dark. A latency pill
  top-right shows live ping state (color-coded per the ping bands above).
- **Media & keys bottom sheet**: a 5-across "Playback" row (−10s/prev/play-pause/
  next/+10s) and an 8-across "Keys" row (esc/tab/↑/space/←/↓/→/enter).
- **Host window**: titlebar → a horizontally auto-scrolling "LIVE" ticker strip → a
  4-column stat row → sidebar nav (**Overview / Traffic / Launchables / Pairing**) +
  main pane. Overview is descriptive/marketing content (three alternating feature rows,
  left/right mirrored). Traffic is the raw message log, newest first, with a
  right-aligned timing column. Launchables is the app-shortcut CRUD (same as v1's "App
  shortcuts", renamed). Pairing is the QR + address/port/token panel, same shape as v1.

## What's real vs. what was a design-review convenience in the source mocks

The source `.dc.html` files include some behavior that exists only to make the design
reviewable in isolation — don't reproduce it literally:

- The Host mock's stat strip auto-alternates between two fake datasets every 2.6s, and
  the phone's ping pill cycles through 3 fake states on tap. **Replace both with one
  real, live-measured view** — see each project's build notes for what to measure.
- The phone mock's quick-launch tiles are hardcoded to fictional "Browser / Macro 1 /
  Sleep PC" entries. **Use the user's actual configured Host shortcuts instead.**
- The bottom sheet's "Keys" row implies raw keystroke simulation, which is a Phase 2
  feature not in `PROTOCOL.md`. Keep it visually, but it must not silently pretend to
  work — see each project's build notes.
