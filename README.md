# MineHost

MineHost is an Android application for hosting a Minecraft Bedrock-compatible server locally on an ARM64 Android device.

## Project reports

- [Complete Project Progress Report](docs/PROJECT_STATUS.md) — authoritative implementation status, phase roadmap, blockers, and next actions.
- [Physical Device Test Results](device-evidence/DEVICE_TEST_RESULTS.md) — historical results and the required latest-APK acceptance checklist.
- [Raw Device Evidence](device-evidence/evidence/) — original ADB logs, screenshots, lifecycle output, and UI hierarchy captures.

## Build

Requires JDK 17 and the Android SDK.

Windows PowerShell:
```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

macOS/Linux:
```bash
chmod +x ./gradlew
./gradlew testDebugUnitTest lintDebug assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Current scope

- **Phase 1:** Complete and physically accept local Bedrock hosting, LAN gameplay, world persistence, and backup/restore.
- **Phase 2:** MineHost-owned public connectivity, NAT traversal, and relay infrastructure.
- **Deferred:** Android Java hosting and Java/Bedrock cross-play.

A passing CI build does not prove physical-device execution or real Minecraft multiplayer. See the project progress report for the current acceptance gates.
