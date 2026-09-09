# Remote Control — v1

Control your Windows laptop's volume, brightness, and media playback from your phone,
and launch apps remotely — over your home WiFi, no internet or cloud involved.

Two pieces:

- **`windows-host/`** — "Remote Host", a system-tray app for the laptop (.NET 8 / WPF).
- **`android/`** — "Remote", the phone app (Kotlin / Jetpack Compose).

They talk to each other directly over your LAN via WebSocket. Nothing leaves your
network. See [`PROTOCOL.md`](PROTOCOL.md) for the wire format and
[`THEME.md`](THEME.md) for the shared visual design if you want to extend either side.

This is the v1 (MVP) scope only: volume, brightness, media control, and app shortcuts.
Touchpad, keyboard input, macros, clipboard sync, and remote lock/sleep are Phase 2 and
are **not** built yet — the Android app's Touchpad tab is a placeholder.

> **Neither project has been compiled in this environment** (no .NET SDK / Android
> SDK / Gradle available where they were written). Both were hand-written and reviewed
> carefully against known-correct library APIs, but **you must build each project
> yourself as the first step** — see below — and fix anything a real compiler flags
> before relying on this. Treat it as a complete, carefully-written first draft, not
> as pre-verified working software.

---

## 1. Set up the Windows Host

**Prerequisites:** Windows 10/11, [.NET 8 SDK](https://dotnet.microsoft.com/download/dotnet/8.0).

```bash
cd windows-host
dotnet restore
dotnet build
dotnet run --project RemoteHost
```

If that all succeeds, you won't see a window — look for the **Remote Host** icon in
your system tray (bottom-right, near the clock). That's expected; it's a background
service.

Full detail (NuGet packages, self-contained publish for distributing a standalone
`.exe`, config file location) is in [`windows-host/README-BUILD.md`](windows-host/README-BUILD.md).

### Running it every time you log in

Once the app is working, open it (left-click the tray icon) → **General** → turn on
**"Start on login"**. This adds a Startup entry so the Host is always listening when
you log into Windows — you shouldn't need to launch it manually again.

---

## 2. Set up the Android app

**Prerequisites:** Android Studio (Ladybug 2024.2+), a phone or emulator on **API 26+**,
on the **same WiFi network** as the laptop.

1. Open the `android/` folder in Android Studio and let it sync (downloads dependencies
   — needs internet the first time).
   - This project ships hand-written Gradle wrapper *properties* but not the generated
     wrapper jar/scripts — Android Studio regenerates those automatically on first
     open. If you're building from the command line instead, see the note in
     [`android/README-BUILD.md`](android/README-BUILD.md).
2. Run the `app` configuration on your device, or build an APK:
   ```bash
   ./gradlew assembleDebug
   ```
   Output lands at `android/app/build/outputs/apk/debug/app-debug.apk`.
3. Install it on your phone (if you built the APK manually rather than running from
   Android Studio, you'll need to allow installs from your build tool / "unknown
   sources" once).

Full detail (exact dependency versions, known build caveats) is in
[`android/README-BUILD.md`](android/README-BUILD.md).

---

## 3. Pair the phone with the laptop

1. On the laptop, left-click the Remote Host tray icon (or right-click → **Show
   pairing QR**). Go to the **Pairing** tab — you'll see a QR code plus the same info
   as text (address, port, token) in case scanning fails.
2. On the phone, open the Remote app. On first launch you'll land on a **"Pair with
   your laptop"** screen — tap **Scan QR** and point the camera at the laptop screen.
3. Once scanned, the phone connects immediately and the header's connection dot turns
   green ("CONNECTED"). You're paired — this only needs to happen once; the phone
   remembers the laptop's address and token.
4. **If scanning doesn't work** (no camera, bad lighting, etc.), use **"Enter address
   manually"** from the same screen and type in the address/port/token shown as text
   on the laptop's Pairing tab instead.

The pairing token stays the same across Host restarts. You only need to re-scan if you
click **"Rotate token & regenerate code"** on the laptop (e.g. if you think the token
leaked), or if you reset/reinstall the Host or the phone app.

---

## 4. Add app shortcuts

On the laptop, open the Host window → **App shortcuts**:

1. Fill in a **Name** (shown on the phone tile) and the full **Path** to the
   application's `.exe` (e.g. `C:\Program Files\Spotify\Spotify.exe`).
2. Click **Add**.

It appears immediately in the phone's **Apps** tab as a tile — tap it to launch that
app on the laptop. Use **remove** next to any row in the Host's list to delete a
shortcut; it disappears from the phone automatically, no re-pairing needed.

---

## 5. Using it day to day

- **Dashboard** tab: volume slider + mute + nudge buttons, brightness slider, and
  media transport controls (±10s / prev / play-pause / next). These reflect the
  laptop's *actual* current volume/brightness — if you change the volume with the
  laptop's own keyboard or the physical slider, the phone updates too.
- **Apps** tab: tap a tile to launch that program on the laptop.
- **Touchpad** tab: placeholder for now — Phase 2 feature, not built in v1.
- **Settings** tab: see the paired host's info, re-scan to re-pair, toggle
  auto-reconnect, or disconnect.

---

## 6. What happens when things go wrong

| Situation | What you'll see on the phone | What to do |
|---|---|---|
| Laptop's WiFi drops briefly | "Reconnecting…" — the app retries automatically every few seconds (backing off up to ~10s between tries) | Usually nothing — it recovers on its own once WiFi is back |
| Remote Host isn't running on the laptop | "Host isn't running" / host offline state | Open Remote Host on the laptop (or check it's in the tray) |
| Pairing token no longer matches (rotated, or Host was reset) | "Pairing token rejected" with a **Scan QR** prompt | Go to the laptop's Pairing tab and re-scan |
| Never paired yet | "Pair with your laptop" first-run screen | Follow the pairing steps above |
| Phone and laptop are on different networks (e.g. phone on mobile data, or different WiFi/guest network) | Connection never succeeds / stays in "Reconnecting" | Confirm the phone is on the **same WiFi** as the laptop — this is a LAN-only tool by design, it will not work over the internet |

The Host also actively **rejects connections from outside its own local subnet** — if
your phone somehow got a WebSocket connection through from a different network, the
Host would close it instantly rather than accept commands. That's a deliberate security
boundary, not a bug.

---

## Security notes (v1)

- No data leaves your local network — there is no cloud server or relay.
- A random pairing token is generated on the Host's first run; the phone must present
  it on every connection or the Host rejects it.
- The Host only accepts TCP connections whose source IP is on the same subnet as its
  own network adapter.
- The connection itself is plain `ws://` (unencrypted) — acceptable for a LAN-only
  personal tool in v1, but don't extend this to run over an untrusted network without
  adding TLS.

---

## Project layout

```
click/
  PROTOCOL.md          shared WebSocket JSON contract — read this before touching either side
  THEME.md              shared visual design tokens (colors, type, shape)
  README.md             this file
  windows-host/          .NET 8 / WPF tray app ("Remote Host")
    README-BUILD.md      build/run instructions for this sub-project
  android/               Kotlin / Jetpack Compose app ("Remote")
    README-BUILD.md      build/run instructions for this sub-project
```

## Roadmap (Phase 2, not built yet)

Touchpad mode, keyboard input, presentation remote / laser pointer, custom macro
buttons, clipboard sync, remote lock/sleep/wake, and notification mirroring — all
explicitly out of scope for this v1 pass. Build these only after the MVP above feels
solid in daily use, per the original build order.
