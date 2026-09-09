# Remote (Android) — build reference

This is the build/run reference for this sub-project only. The user-facing setup guide
(pairing walkthrough, adding shortcuts on the Host, etc.) lives elsewhere.

## Prerequisites

- Android Studio Ladybug (2024.2) or newer — bundles JDK 17, which this project targets.
- JDK 17 (use the one bundled with Android Studio; don't mix in JDK 21+).
- Android SDK Platform 34 and Build-Tools 34.x, installed via the SDK Manager.
- A device or emulator running API 26 (Android 8.0) or newer, with a camera if you want
  to test QR scanning (the emulator's virtual camera works for this).

## Gradle wrapper note

This project was hand-authored outside Android Studio, so it ships
`gradle/wrapper/gradle-wrapper.properties` (pinned to Gradle 8.7) but **not** the binary
`gradle-wrapper.jar`, nor the `gradlew` / `gradlew.bat` scripts — those are binary/
generated artifacts. Before building from the command line, generate them once:

```
gradle wrapper --gradle-version 8.7
```

(requires a local Gradle install just for this one command), or simply **open the
`android/` folder in Android Studio first** — it will detect the wrapper properties,
offer to sync, and regenerate the missing wrapper files automatically. After that,
`./gradlew` works normally.

## Open and sync

1. Android Studio → Open → select the `android/` folder (the one containing this file).
2. Let Gradle sync. It will download AGP, Kotlin, and all dependencies listed below on
   first sync — this needs network access.
3. Run the `app` configuration on a device/emulator, or build a debug APK (below).

## Building a debug APK

```
./gradlew assembleDebug
```

Output: `android/app/build/outputs/apk/debug/app-debug.apk`

## Dependency versions used

- Android Gradle Plugin 8.5.2, Kotlin 1.9.24, Gradle 8.7
- Compose BOM 2024.09.00, Compose compiler extension 1.5.14
- Jetpack Navigation Compose 2.7.7
- Lifecycle (runtime-ktx / runtime-compose) 2.8.4, Activity Compose 1.9.2, Core KTX 1.13.1
- OkHttp 4.12.0
- kotlinx-serialization-json 1.6.3
- DataStore Preferences 1.1.1
- CameraX (core / camera2 / lifecycle / view) 1.3.4
- ML Kit barcode-scanning 17.3.0

`compileSdk` / `targetSdk` 34, `minSdk` 26, `applicationId` `com.remotehost.remote`.

## Known constraints of this build

- No Gradle/JDK/Android SDK was available in the sandbox this project was authored in,
  so none of this has been compiled. Everything was written against current, stable
  library APIs and cross-checked by hand; a first sync/build in Android Studio may
  still surface small issues (e.g. a version bump Google has since made mandatory).
- No bundled font files: Plus Jakarta Sans / IBM Plex Mono from THEME.md fall back to
  `FontFamily.Default` / `FontFamily.Monospace`. Drop real `.ttf` files under
  `app/src/main/res/font` and point `ui/theme/Type.kt` at them to use the real faces.
- Pairing data (including the token) is stored in plain DataStore Preferences, not
  Android Keystore-encrypted storage — acceptable for a v1 LAN-only tool per the build
  spec, but worth knowing.
