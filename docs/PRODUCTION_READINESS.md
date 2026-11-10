# MineHost — Production Readiness Audit

_Audit date: 2026-11-10. Source inspection of repository `main` at commit `6db300fe1265eccbd7cbe855d88a128964ec10fe`, plus the current working branch's documentation changes. This is a source review only; it is not a build or device test._

## Verdict

# NOT PRODUCTION READY

The repository has a meaningful Android application foundation and substantial integration code. However, the core product promise — a real Minecraft server running on Android and reachable by external players — has not yet been demonstrated. Critical runtime and networking dependencies remain unresolved.

Do not publish claims that MineHost currently provides production-grade hosting, worldwide connectivity, or cross-play.

## Critical blockers

| # | Blocker | Source evidence / consequence | Required proof |
|---|---|---|---|
| 1 | No validated PHP runtime | `BedrockEngine` calls `RuntimeManager.getPhpExecutable()`; no `jniLibs` directory or PHP runtime installer is present in the repository tree. The PocketMine PHAR download alone cannot run the engine. | Select and legally distribute a compatible ARM64 Android PHP runtime and prove its CLI/extensions/process model on a physical device. |
| 2 | No validated Java VM | `JavaEngine` expects `libjava.so` in `nativeLibraryDir`; the repository has no bundled Java runtime or Java runtime installer. Android ART does not execute desktop Paper JARs. | Prove a compatible JVM and native dependency set on target Android devices; run Paper and a real Java client. |
| 3 | Android executable packaging model is unproven | Runtime manager has paths for downloaded executables in app-private writable storage, including the Playit agent. Modern Android execution restrictions mean `setExecutable(true)` is not proof that a downloaded ELF can execute. | Validate packaging/loader strategy against current Android versions and Play policy. Never assume desktop Linux assets run on Android/Bionic. |
| 4 | No MineHost-owned public relay | Current provider implementation is Playit-specific. The product goal requires MineHost-operated coordinator and relay services. | Deploy and test MineHost's own authenticated TCP and protocol-appropriate UDP relay from a CGNAT phone to a client on another ISP. |
| 5 | No proven direct-first path | STUN/ICE/direct route negotiation is not yet implemented as an end-to-end feature. Java TCP and Bedrock transport need separate adapters. | Demonstrate LAN, IPv6/direct, supported NAT traversal, and relay fallback with real clients. |
| 6 | Bedrock transport compatibility is not established | Existing docs/code describe legacy RakNet assumptions and PocketMine versions. Current client/server transport compatibility must be checked against the selected maintained engine. | Publish a versioned engine/client/transport matrix and pass real-client tests for each supported version. |
| 7 | No end-to-end evidence | No confirmed physical Android device, real Minecraft client, external ISP, or international connection test is recorded in this audit. | Execute the test plan in `DEVICE_TESTING.md` and record device, OS, engine version, client version, network, RTT/loss, TPS/MSPT and results. |
| 8 | Release signing is conditional | Release signing is read from local properties/environment; absent credentials leave the release unsigned. | Configure a protected release key and verify signed artifact and upgrade continuity. |
| 9 | CI/build reproducibility not established here | This audit environment did not run Gradle or Android SDK tasks. | Add/execute CI for Gradle build and tests; record actual workflow results. |

## Source-reviewed capabilities (not device verified)

| Area | Source-level state | Evidence boundary |
|---|---|---|
| Android UI and navigation | Implemented foundation | Not built in this audit. |
| Room/Hilt and data/domain layers | Present | Migrations and app behaviour not instrumented here. |
| Server engine abstractions | Present | Does not prove runtimes exist. |
| PocketMine engine adapter | Present | Blocked without PHP runtime. |
| Paper engine adapter | Present | Blocked without Java VM. |
| Geyser configuration generation | Present | Does not prove Geyser execution or cross-play. |
| Runtime manager | Downloads PHAR/JAR/Playit agent and records components | No PHP/Java runtime installer; binary execution and trusted integrity need validation. |
| Playit provider | Present as experimental provider | Third-party managed service; not the target independent architecture; Android execution and real external joins unproven. |
| Local network helpers | Present | Real device/LAN acceptance not established. |
| Connection monitor | Present | Ensure each probe matches the transport; a TCP connect is not a Bedrock UDP gameplay health test. |
| Foreground service, wake lock, thermal monitor | Present | Must be tested on current Android versions and OEM devices; foreground-service policy compliance is not inferred from source alone. |
| World backup/restore | Significant implementation present | Must be tested against real worlds and engine save-completion semantics. |
| EULA consent | Consent flow is documented as implemented in the latest progress notes | Verify persisted state and exact release behaviour in an instrumented/device test. |
| Unit tests | Test sources exist | Do not count them as passing until CI or a reproducible local run records the result. |

## Immediate next actions

1. Add reproducible Android CI and run `assembleDebug` plus `testDebugUnitTest`.
2. Remove misleading "supported" claims for engines whose runtime is missing.
3. Decide whether PocketMine or another maintained Bedrock implementation is viable for current Bedrock transport and Android ARM64.
4. Build or adopt a properly licensed Android-compatible PHP runtime; test it independently before integrating.
5. Investigate Java VM feasibility separately; do not let Paper/Geyser block a real Bedrock milestone.
6. Stop treating Playit as the production networking architecture. Keep it isolated as an optional experimental adapter, while specifying and implementing MineHost's own control/data planes.
7. Prove a real local Bedrock server and client join on physical Android hardware.
8. Build one MineHost-owned relay and prove an external join from a different ISP/CGNAT network.
9. Validate safe backups, crash recovery, Android foreground-service behaviour, thermal limits, and sustained hosting.
10. Complete dependency/licence, security, privacy, Play policy, and release-signing reviews.

## Verification record

This review inspected the repository tree, README, engine adapters, runtime manager, networking providers, manifest, Gradle configuration, tests, and project documentation.

**Not run in this audit:**
- Gradle build or Android lint;
- Gradle unit tests or instrumentation tests;
- emulator or physical-device tests;
- PHP or Java runtime launch;
- PocketMine/Paper server launch;
- real Minecraft client join;
- Playit claim or external tunnel test;
- MineHost-owned relay test;
- international routing or latency benchmark.

No unrun test should be reported as passed.

## Release decision

**Release status: BLOCKED.**

A release candidate can be considered only after all critical blockers have evidence-backed resolution and the production definition in `MINEHOST_MASTER_ROADMAP.md` is met. A successful APK build alone is insufficient.
