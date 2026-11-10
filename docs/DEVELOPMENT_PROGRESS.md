# MineHost — Development Progress

> **Last reconciled:** 2026-11-10
> **Overall status:** Active development. Not production-ready.

This tracker distinguishes source implementation from verified operation. Update a row only when the stated evidence exists.

## Verification levels

- **SOURCE ONLY:** Code or UI exists; runtime behavior not proven.
- **UNIT TESTED:** Deterministic tests pass.
- **ANDROID BUILD VERIFIED:** Clean Gradle build and tests pass in CI.
- **EMULATOR VERIFIED:** Android framework behavior verified on an emulator.
- **PHYSICAL DEVICE VERIFIED:** Tested on real ARM64 Android hardware.
- **REAL CLIENT VERIFIED:** A real Minecraft client joins and gameplay is exercised.
- **EXTERNAL NETWORK VERIFIED:** A client joins from another network/country.
- **SUSTAINED LOAD VERIFIED:** Long-duration testing with resource/thermal metrics.

## Current inventory

| Subsystem | Current source status | Verification | Notes |
|---|---|---|---|
| Kotlin / Compose app foundation | Present | Source only; latest build pending CI | UI, navigation, view models and feature screens exist. |
| Room / Hilt / DataStore | Present | Source only; migration/device test pending | Includes server metadata and preferences. |
| Server lifecycle/process supervision | Present | Unit/helper tests only | Must verify lifecycle and process ownership on device. |
| Bedrock engine adapter | Present but blocked | Not device verified | PocketMine launcher requires a PHP runtime that is not supplied. |
| PHP ARM64 runtime | Missing | Blocked | No proven install/bundle strategy. |
| Java Paper adapter | Present but blocked | Not device verified | JavaEngine expects libjava.so; no usable JVM supplied. |
| Java ARM64 JVM | Missing | Blocked | Must prove Android/Bionic compatibility. |
| Geyser/Floodgate | Configuration groundwork | Not runtime verified | Cross-play is unavailable until Java runtime and bridge work. |
| Runtime downloads | Present | Unit/source review only | Version and integrity policy needs hardening for executable assets. |
| Playit provider | Present, experimental | Not external-network verified | Third-party dependency; not intended production architecture. |
| MineHost control plane | Not implemented | Not tested | Host registration/session coordination/relay allocation absent. |
| MineHost-owned TCP/UDP relays | Not implemented | Not tested | Separate Java TCP and Bedrock transport implementation required. |
| Direct public connectivity/NAT traversal | Not implemented | Not tested | Requires protocol-specific validation. |
| LAN discovery/address display | Source groundwork | Not physical-device verified | Test across real Wi-Fi networks. |
| World storage | Present | Source/helper tests | Verify with actual server world data and app upgrades. |
| Backup / restore | Present | Pure-Kotlin tests; real-world test pending | Validate save pause, LevelDB/Java world integrity, rollback. |
| Console / command dispatch | Present | Parser/helper tests | Verify commands and process stdin against real engines. |
| Player tracking/admin | Present | Parser/helper tests | Join/leave formats and command acceptance need real server tests. |
| Foreground service / wake lock | Present | Not device verified | Validate API 34+ special-use behavior and Play policy. |
| Thermal monitoring | Present | Source only | Validate on supported devices under sustained load. |
| Release signing | Configured, no production key in repo | Not release verified | Release intentionally unsigned without secrets. |
| Google Play target API | Migration in progress | Not release verified | Main is API 34. The current PR moves the development branch to API 36 and upgrades AGP/Kotlin/Gradle/KSP; CI must pass before this is marked verified. |
| International connectivity | Not implemented | Not tested | No MineHost-owned relay or real international join. |

## Existing safety work in source

- Fake sleep-loop server processes and random public endpoints were removed.
- Server startup requires a protocol readiness response instead of merely a live process or open port.
- Cross-play is disabled rather than falsely mapped to a Bedrock-only engine.
- Failed engine configuration aborts server startup.
- Restore uses checksum validation, temporary extraction, a safety snapshot, and rollback-oriented replacement.
- EULA consent is explicit and persisted; Java start is blocked until accepted.
- Runtime downloads use temporary files and atomic replacement; published SHA-256 is checked when available.
- Tunnel status separates authentication/start/reconnect/error states and redacts sensitive log content.
- Player counts are derived from parsed server logs rather than hard-coded.
- Release builds do not silently fall back to the debug signing key.

These safeguards still need Android build, device, and real-server verification.

## Test evidence

The latest testing record in docs/TESTING.md reports 40 pure-Kotlin helper tests run outside Gradle with 0 failures. The latest Android Gradle build has not yet been reproduced by the current audit. The Android CI workflow has been added on the development branch; check its pull request run before changing the build status to verified.

## Next execution order

1. Get CI green.
2. Prove or replace the Android PHP runtime strategy.
3. Prove or replace the Android JVM strategy.
4. Run the first real local server/client acceptance test.
5. Implement MineHost-owned relay and control-plane proof of concept.
6. Prove external CGNAT connectivity.
7. Validate Bedrock transport and cross-play compatibility by version.
8. Upgrade target SDK/toolchain to API 36 and complete release compliance.
9. Complete sustained performance, security, licensing, and Play Store verification.

See [Master Roadmap](MINEHOST_MASTER_ROADMAP.md), [Engine Evaluation](ENGINE_EVALUATION.md), and [Production Readiness](PRODUCTION_READINESS.md).
