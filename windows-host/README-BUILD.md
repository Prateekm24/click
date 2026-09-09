# Remote Host — build & run reference

Windows system-tray host for the Remote Control Android app. Implements
`PROTOCOL.md` (WebSocket contract) and `THEME.md` (visual style). This file
only covers building/running this sub-project — end-user setup is documented
elsewhere.

## Prerequisites

- Windows 10/11
- [.NET 8 SDK](https://dotnet.microsoft.com/download/dotnet/8.0)

## NuGet packages

Restored automatically from `RemoteHost.csproj`:

| Package            | Version | Used for |
|---------------------|---------|----------|
| `NAudio`            | 2.2.1   | CoreAudio volume/mute control + change notifications (`NAudio.CoreAudioApi`) |
| `QRCoder`           | 1.6.0   | Rendering the pairing QR code as a PNG |
| `System.Management` | 8.0.0   | WMI brightness control (`root\WMI`) |

## Restore / build / run

```powershell
cd windows-host
dotnet restore
dotnet build
dotnet run --project RemoteHost
```

The app has no visible main window on launch — look for the Remote Host icon
in the system tray. Left-click it to open the Status pane; right-click for
"Show pairing QR", "App shortcuts…", "Pause/Resume listening", and "Exit".

## Self-contained single-file publish

To produce a single `RemoteHost.exe` that end users can run without having
the .NET runtime installed:

```powershell
cd windows-host
dotnet publish RemoteHost -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true
```

The published output lands in
`windows-host\RemoteHost\bin\Release\net8.0-windows\win-x64\publish\`.
Copy the whole `publish` folder (the `.exe` plus `Assets\tray.ico`, which is
copied alongside it) when distributing — the tray icon is loaded from disk
next to the executable at runtime, not embedded in the single file.

## Configuration file

Persisted at:

```
%AppData%\RemoteHost\config.json
```

Contains the pairing token, port (default `8765`), configured app shortcuts,
and the three General-pane toggles (start on login, minimize to tray on
close, allow input simulation). A fresh cryptographically random token
(format `xxxx-xxxx`) is generated on first run if this file doesn't exist.
Delete the file to reset everything to defaults.

## Notes / known limitations (see code comments for detail)

- The WebSocket server is a hand-rolled `TcpListener` + manual HTTP Upgrade
  handshake (see `Services/WebSocketServer.cs`) rather than `HttpListener`,
  specifically to avoid Windows' URL-ACL-reservation requirement for
  non-localhost prefixes — this lets the app listen on `0.0.0.0:8765`
  without running elevated.
- Brightness control depends on the display's WMI `root\WMI` exposure
  (typically built-in laptop panels only); on unsupported displays the host
  replies `{"type":"error","message":"brightness not supported on this display"}`
  instead of crashing.
- `seek_forward` / `seek_back` are best-effort: Windows has no universal
  media-seek key, so these send `VK_RIGHT` / `VK_LEFT`, which only does
  something useful in players that bind arrow keys to seeking and only if
  that player currently has input focus.
