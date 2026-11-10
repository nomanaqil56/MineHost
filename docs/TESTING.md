# MineHost — Testing Status

This file separates source code, automated test evidence, and real-device verification.

## Test levels

| Level | Meaning | Current status |
|---|---|---|
| Source review | Code exists and has been reviewed | Partial |
| Pure Kotlin unit tests | Deterministic logic tests | 40 tests were previously run outside Gradle; current Gradle test task is being established |
| Android Gradle build | Clean debug build against API 36 | Pending current CI result |
| Emulator/instrumented | Android framework, Room, service and UI | Not verified |
| Physical device | Real ARM64 phone | Not verified |
| Real Minecraft client | Real server/client gameplay | Not verified; blocked by runtime/engine choice |
| External network | Public join from another network/country | Not verified; MineHost relay not implemented |
| Sustained load | Thermal, battery, TPS/MSPT over hours | Not verified |

## Previous pure-Kotlin test evidence

The previous audit recorded 40 pure-Kotlin helper tests run outside Gradle with 0 failures. These covered:
- protocol framing/parsing helpers;
- command sanitization;
- backup checksum/tamper detection;
- world swap/rollback helpers;
- tunnel state/reconnect/log parsing;
- player join/leave parsing;
- local network address selection.

This is not proof of Android framework integration, a real Minecraft server, or public connectivity. Some protocol/parser assumptions have not been captured from real server logs.

## Current automated CI

The development branch adds [.github/workflows/android-ci.yml](../.github/workflows/android-ci.yml), which:
1. sets up JDK 17 and Android SDK 36;
2. runs testDebugUnitTest;
3. assembles a debug APK;
4. uploads test reports and APK artifacts.

Check the latest workflow run and fix failures before marking the build verified. CI has not yet established physical-device behavior.

## Not verified

- PHP runtime and Bedrock engine launch on Android.
- JVM and Paper launch on Android.
- Geyser/Floodgate cross-play.
- Current Bedrock transport/version compatibility.
- Real Minecraft client join on LAN.
- MineHost-owned relay, CGNAT traversal, or international join.
- Wi-Fi/mobile-data handover and recovery.
- Foreground service survival, Doze/OEM restrictions, thermal/battery behavior.
- Backup/restore against real Bedrock LevelDB and Java world files.
- Release signing, AAB, Play pre-launch report, licensing and privacy/security audit.

See [Device Testing](DEVICE_TESTING.md), [Production Readiness](PRODUCTION_READINESS.md), and [Master Roadmap](MINEHOST_MASTER_ROADMAP.md).
