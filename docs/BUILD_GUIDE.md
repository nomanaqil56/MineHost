# MineHost — Build & Installation Guide

## 1. Prerequisites

- Windows 10/11, macOS, or Linux.
- JDK 17.
- Android Studio / Android SDK Platform 34 and compatible Build Tools.
- Git, curl, tar, sha256sum, and readelf (binutils).
- Git Bash or WSL on Windows for the runtime preparation script.
- ARM64 Android phone for actual server-runtime testing.

## 2. Clone the repository

```bash
git clone https://github.com/nomanaqil56/MineHost.git
cd MineHost
```

## 3. Prepare the Android ARM64 PHP runtime

The Bedrock adapter expects `libphp.so` in Android's extracted native-library directory. The build-time bootstrap downloads a pinned PMMP PHP-Binaries Android ARM64 static build, verifies the published archive SHA-256, ELF architecture and static linkage, then places it in the ignored `app/src/main/jniLibs/arm64-v8a` directory.

Run on Linux/macOS or Git Bash/WSL:

```bash
bash tools/prepare_android_php_runtime.sh
```

The binary is generated locally and is not committed to Git. This script validates the archive and ELF metadata only; it does **not** prove that the executable works on a physical Android device. The PHP-Binaries upstream project and all bundled dependency licences must be reviewed before any public release.

## 4. Build the debug APK

Linux/macOS:

```bash
chmod +x gradlew
./gradlew --no-daemon assembleDebug
./gradlew --no-daemon testDebugUnitTest
```

Windows PowerShell (after running the Bash preparation script):

```powershell
.\gradlew.bat --no-daemon assembleDebug
.\gradlew.bat --no-daemon testDebugUnitTest
```

Debug APK output:

`app/build/outputs/apk/debug/app-debug.apk`

A successful build only proves that the Android project compiles. It does **not** prove that the PHP executable runs on-device, PocketMine starts, or Minecraft clients can connect.

## 5. GitHub Actions

Workflow: `.github/workflows/android-ci.yml`

The workflow downloads and verifies the pinned PHP runtime, packages it into the debug APK, runs Gradle build and unit tests, and uploads the APK and test reports on success. Inspect the run in the repository's Actions tab.

Do not confuse a CI build with a physical-device acceptance test.

## 6. Install the debug build

With a device connected and USB debugging enabled:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Grant only permissions required by the app. Android 13+ notification permission is needed for the ongoing foreground-service notification.

## 7. Current runtime limitations

The PHP runtime packaging path is a candidate and still requires physical-device validation. No validated Java VM is present. Therefore:
- downloading the PocketMine PHAR alone does not make Bedrock hosting runnable;
- downloading the Paper JAR alone does not make Java hosting runnable;
- do not advertise either engine as working until the runtime launches and real Minecraft clients pass the acceptance tests.

Use Diagnostics to inspect the current runtime state. Missing or incompatible runtimes must produce a clear error, never a false ONLINE status.

## 8. Device acceptance

Follow [DEVICE_TESTING.md](DEVICE_TESTING.md). Record device model, Android version, ABI, app commit, runtime/engine versions, Minecraft client version, network type, and exact results.

Public internet testing must use a MineHost-owned relay acceptance environment. The Playit adapter is experimental and is not proof of the final independent-network architecture.
