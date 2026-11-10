# MineHost — Build and Device Test Guide

This guide covers building the Android application. **A successful APK build does not mean Minecraft hosting works.** Runtime execution and gameplay remain blocked until verified on physical Android hardware. See [PRODUCTION_READINESS.md](PRODUCTION_READINESS.md).

## 1. Prerequisites

- Windows 10/11, macOS or Linux.
- JDK 17.
- Android SDK Platform 34 and Build Tools 34.0.0 (matching the current project configuration).
- Git.
- Android ARM64 physical device and ADB for end-to-end testing.

The project currently uses Gradle Wrapper 8.4 and Android Gradle Plugin 8.3.2. Use the checked-in wrapper rather than a globally installed Gradle.

## 2. Build and test locally

### Windows PowerShell

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

### macOS / Linux

```bash
chmod +x ./gradlew
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

Debug APK output:

`app/build/outputs/apk/debug/app-debug.apk`

## 3. Continuous integration

The repository now includes `.github/workflows/android-ci.yml`. It installs JDK 17 and Android SDK Platform 34, runs unit tests and lint, assembles the debug APK, and uploads the APK as the `minehost-debug-apk` artifact.

The first workflow run must pass before treating the branch as build-verified. CI does not replace physical-device or real Minecraft-client testing.

## 4. Install on a physical device

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Grant only the permissions required by the app. Review notification, battery-optimization and foreground-service prompts. OEM battery settings differ; a foreground service does not guarantee immunity from all vendor process-killing policies.

## 5. Current runtime limitation

The Diagnostics screen may report that PHP or Java runtime components are unavailable. Do not assume downloading PocketMine-MP.phar or Paper.jar alone is sufficient:
- PocketMine requires a working Android ARM64 PHP runtime and valid launcher.
- Paper requires a complete Android-compatible ARM64 JVM and launcher.
- A desktop Linux executable or a file named `libphp.so` / `libjava.so` is not automatically runnable on Android.

Do not proceed to gameplay acceptance testing until the selected runtime is validated on the target device.

## 6. Physical-device acceptance sequence

Follow [DEVICE_TESTING.md](DEVICE_TESTING.md). At minimum:

1. Record device model, Android version, ABI, available RAM and free storage.
2. Install the debug APK and verify the app opens without crash.
3. Verify database migration by upgrading over a prior test install with test data.
4. Install the selected engine and runtime from verified sources.
5. Start the server and confirm ONLINE only after a real protocol readiness response.
6. Join from a second device on the same Wi-Fi network.
7. Place/break blocks, stop and restart; verify persistence.
8. Create and restore a backup of a real world.
9. Lock the screen and run a sustained hosting test while monitoring temperature, battery and memory.
10. Test cellular and external connectivity only after MineHost's own relay implementation is available.
11. Record logs, failure details and reproducible steps.

## 7. Release build signing

Release signing is configured through environment variables or uncommitted `local.properties` entries:

- `MINEHOST_KEYSTORE_FILE`
- `MINEHOST_KEYSTORE_PASSWORD`
- `MINEHOST_KEY_ALIAS`
- `MINEHOST_KEY_PASSWORD`

Never commit keystores or passwords. Without a configured production key, the release build is intentionally unsigned. Do not use the debug key for distribution.

## 8. Verification record

For each acceptance run, record:
- Commit SHA.
- Build variant and CI run URL.
- Device model, Android version and ABI.
- Engine/runtime version and source.
- Minecraft client version and platform.
- Network type and ISP/region (avoid recording personal IPs in public logs).
- Steps performed and pass/fail outcome.
- Relevant redacted logs and metrics (p50/p95 RTT, jitter, loss, TPS/MSPT, thermal state).

Never mark a feature as device-tested or production-ready based only on a successful compile.
