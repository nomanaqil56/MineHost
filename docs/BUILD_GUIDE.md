# MineHost — Build and Installation Guide

> This guide covers building the Android application. It does not claim that a Minecraft server can currently run. See the [Production Readiness Audit](PRODUCTION_READINESS.md) before testing server features.

## Prerequisites

- Windows 10/11, macOS, or Linux.
- JDK 17.
- Android SDK Platform 36 and Build Tools 36.0.0.
- Git.
- Android Studio version compatible with the project's AGP 8.13 / Gradle 8.13 toolchain.

## Build and test

Linux/macOS:
```bash
git clone https://github.com/nomanaqil56/MineHost.git
cd MineHost
chmod +x gradlew
./gradlew --no-daemon testDebugUnitTest assembleDebug
```

Windows PowerShell:
```powershell
git clone https://github.com/nomanaqil56/MineHost.git
cd MineHost
.\gradlew.bat testDebugUnitTest assembleDebug
```

Debug APK output:
```text
app/build/outputs/apk/debug/app-debug.apk
```

The repository CI workflow is [.github/workflows/android-ci.yml](../.github/workflows/android-ci.yml). CI is the reproducible build check; inspect the latest run rather than assuming it passed.

## Install a debug build on a device

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Use an ARM64 Android device for server-runtime experiments. The current app supports Android API 28+ at the manifest level, but this does not mean every Android version/device is suitable for hosting.

## Current runtime limitation

The current source does not include a verified Android PHP runtime or Android JVM. Therefore:
- PocketMine install must fail clearly until a verified PHP runtime is bundled.
- Paper install must fail clearly until a verified Java runtime is bundled.
- Cross-play must remain unavailable until Java + Geyser are actually executable.
- Playit is an experimental third-party adapter, not the production connectivity plan.
- No worldwide public join is currently verified.

Do not follow older instructions that claim downloading only a PocketMine PHAR or Paper JAR makes the engine runnable.

## Physical-device validation

1. Install the debug APK.
2. Open Diagnostics and confirm runtime availability is reported accurately.
3. Attempting to install Bedrock or Java while the runtime is missing must produce an actionable error and must not mark the engine as installed.
4. Verify app navigation, server metadata, local storage, and backup UI.
5. Do not claim server hosting works until a selected engine starts on-device and a real Minecraft client joins.
6. Follow [Device Testing](DEVICE_TESTING.md) after an engine/runtime proof exists.

## Release builds

A release signing key must be supplied through uncommitted local properties or CI secrets. Release builds intentionally do not fall back to the debug key. Before publishing, complete the API 36 migration, Play policy review, licence/SBOM review, privacy/security review, signed AAB build, and physical-device acceptance tests.
