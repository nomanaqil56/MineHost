# MineHost

MineHost is an Android application for running a Minecraft Bedrock server locally on an Android device.

## Current project status

- [Project Progress & Phase 1 Status](docs/PROJECT_STATUS.md) — authoritative implementation status and roadmap.
- [Physical Device Test Results](device-evidence/DEVICE_TEST_RESULTS.md) — consolidated device results and retained evidence.

## Build

Requires JDK 17 and the Android SDK.

Windows:
```powershell
.\gradlew.bat testDebugUnitTest lint assembleDebug
```

macOS/Linux:
```bash
./gradlew testDebugUnitTest lint assembleDebug
```

## Scope

- Phase 1: local Android Bedrock hosting, runtime verification, world persistence, and LAN gameplay.
- Phase 2: MineHost-owned internet connectivity, NAT traversal, and relay infrastructure.
- Java hosting and cross-play are separate deferred work.

## Repository structure

- `app/src/main`: Android application and server implementation
- `app/src/test`: JVM unit tests
- `docs/PROJECT_STATUS.md`: single project progress report
- `device-evidence/DEVICE_TEST_RESULTS.md`: consolidated device test result
- `device-evidence/evidence/`: retained raw evidence and screenshots
