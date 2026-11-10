# MineHost — Build & Installation Guide

## 1. Prerequisites

- Windows 10/11, macOS, or Linux.
- JDK 17.
- Android Studio / Android SDK Platform 34 and compatible Build Tools.
- Git.
- Android ARM64 phone for runtime and server acceptance testing.

## 2. Prepare the Android ARM64 PHP runtime

The Bedrock adapter expects `libphp.so` in Android's extracted native-library directory. The repository now includes a build-time bootstrap script for a checksum-pinned PocketMine PHP-Binaries Android ARM64 static build. It places the generated binary in an ignored `jniLibs` directory; the binary is not committed to Git.

On Linux/macOS (or Git Bash/WSL on Windows), run:

```bash
bash tools/prepare_android_php_runtime.sh
```

The script verifies the release archive SHA-256, ELF architecture, and static linkage before copying the executable. This is **not** proof that the binary runs on every Android version. The PHP-Binaries upstream project is an external build artifact source; complete a licence/dependency review before any public release.

Clone the repository:

```bash
git clone https://github.com/nomanaqil56/MineHost.git
cd MineHost
bash tools/prepare_android_php_runtime.sh
```

Linux/macOS:

```bash
chmod +x gradlew
./gradlew --no-daemon assembleDebug
./gradlew --no-daemon testDebugUnitTest
```

Windows PowerShell:

```powershell
.\gradlew.bat --no-daemon assembleDebug
.\gradlew.bat --no-daemon testDebugUnitTest
```

Debug APK output:

`app/build/outputs/apk/debug/app-debug.apk`

A successful build only proves that the Android project compiles. It does **not** prove that a server runtime can execute, that a Minecraft server starts, or that remote clients can connect.

## 3. GitHub Actions

The repository CI workflow is:

`.github/workflows/android-ci.yml`

Open the repository's Actions tab, select **Android CI**, and inspect the build/test run. The workflow prepares the pinned Android PHP runtime, then uploads a debug APK and unit-test reports when the jobs succeed.

Do not confuse a CI build with a physical-device acceptance test.

## 4. Install the debug build

With a device connected and USB debugging enabled:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Grant only the permissions required by the app. Android 13+ notification permission is needed for the ongoing foreground-service notification. Review battery-optimization guidance in the app if offered.

## 5. Current runtime limitation

The repository contains a build-time PHP runtime bootstrap script, but the runtime has not yet been validated on a physical Android device. No validated Java VM is present. Therefore:
- downloading the PocketMine PHAR alone does not make Bedrock hosting runnable; the build-time PHP preparation script must also have completed and the packaged runtime must pass on-device validation;
- downloading the Paper JAR alone does not make Java hosting runnable;
- do not advertise either engine as working until its runtime is validated on a physical device.

Use Diagnostics to inspect the current runtime state. A missing runtime must produce a clear, truthful error. Do not proceed as though a server is online.

## 6. Device acceptance

Follow [DEVICE_TESTING.md](DEVICE_TESTING.md). Record device model, Android version, ABI, app commit, runtime/engine versions, Minecraft client version, network type, and the exact result.

Public internet testing must use a MineHost-owned relay acceptance environment. The Playit adapter is experimental and is not proof of the final independent-network architecture.
