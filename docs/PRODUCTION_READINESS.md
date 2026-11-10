# MineHost — Production Readiness Audit

_Audit date: 2026-11-10. The original baseline review inspected `main` at `6db300fe1265eccbd7cbe855d88a128964ec10fe`. The current PR branch adds the Axolotl-PM/NetherNet and runtime-packaging changes described below. This is source inspection only; no Gradle build or physical-device test has been completed in this review._

## Verdict

# NOT PRODUCTION READY

MineHost has an Android application foundation and server/network integration code, but the core promise — a real Minecraft server running on Android and reachable by external players — has not yet been demonstrated.

Do not publish claims of production-grade hosting, worldwide connectivity, or cross-play until the release gates below pass.

## Critical blockers

| # | Blocker | Source evidence / consequence | Required proof |
|---|---|---|---|
| 1 | PHP runtime not device-validated | Build-time script pins the Axolotl-PM PHP 8.4 Android ARM64 asset, verifies archive SHA-256 and static AArch64 ELF properties, and packages it as `jniLibs/arm64-v8a/libphp.so`. The app checks for PHP and the `webrtc` module. Neither packaging nor execution has yet been verified on a physical device. | CI must package it; inspect APK/nativeLibraryDir; run `libphp.so -v` and `-m` on physical ARM64 Android; verify ext-webrtc; start Axolotl-PM and join with a real client. |
| 2 | PHP/native dependency licensing | The PHP runtime includes PHP and native dependencies (including libdatachannel/ext-webrtc). The full dependency licence inventory and redistribution rights are not yet documented. | Produce SBOM/licence notices and confirm redistribution rights before public release. |
| 3 | No validated Java VM | `JavaEngine` expects `libjava.so`; no validated Android JVM distribution or install path is present. Android ART does not execute desktop Paper JARs. | Prove a compatible JVM/native dependency set on physical Android devices; run Paper and a real Java client. |
| 4 | Android executable packaging model unproven on device | The PHP script packages an executable in `jniLibs` to avoid running a downloaded ELF from writable app storage. This packaging strategy still needs Android installer/execution proof and Play policy review. Playit agent download/execution remains experimental. | Inspect APK extraction and execute the packaged binary on supported Android versions. Review Google Play executable-code and native-code rules. |
| 5 | No MineHost-owned public edge or TURN | Current provider code includes experimental Playit integration. The production target requires a MineHost-operated TCP edge/reverse tunnel for Java and Bedrock HTTPS signalling, plus MineHost-operated TURN for NetherNet UDP gameplay fallback. | Deploy and test the MineHost coordinator, TCP edge/tunnel, and TURN from a CGNAT phone to real clients on another ISP. |
| 6 | NetherNet gameplay not proven | Branch code targets Axolotl-PM 5.51.1 / Bedrock 1.26.60, configures `transport.name: nethernet`, checks ext-webrtc, and probes HTTP(S) `GET /v1/join`. A signalling 2xx does not prove WebRTC ICE/DTLS/SCTP gameplay connectivity. | Join with real Bedrock 1.26.60 clients over LAN, direct WAN, and MineHost TURN fallback. Record the version matrix and results. |
| 7 | No end-to-end evidence | No confirmed physical Android device, real Minecraft client, external ISP, or international connection test is recorded. | Execute `DEVICE_TESTING.md` and record device, OS, engine/client versions, network, RTT/loss, TPS/MSPT and outcome. |
| 8 | Release signing is conditional | Release signing is read from local properties/environment; absent credentials leave the release unsigned. | Configure protected release signing and verify signed artifact/update continuity. |
| 9 | CI/build reproducibility not established | Android CI workflow has been added, but its run result must be inspected; no passing result is recorded here. | Confirm PHP runtime bootstrap, `assembleDebug`, and `testDebugUnitTest` pass; retain APK and test reports. |

## Source-reviewed capabilities (not device verified)

| Area | Source-level state | Evidence boundary |
|---|---|---|
| Android UI/navigation and Room/Hilt | SOURCE PRESENT | No build/instrumented behaviour verified in this audit. |
| Server engine abstraction | SOURCE PRESENT | Does not prove a runtime exists. |
| Axolotl-PM adapter | SOURCE PRESENT | Targets 5.51.1 / Bedrock 1.26.60; runtime and actual client join unverified. |
| PHP runtime bootstrap | SOURCE PRESENT | Pinned Android ARM64 asset and packaging checks added; no physical-device run. |
| NetherNet config | SOURCE PRESENT | Generates NetherNet transport, persistent key path and UDP port range; TURN credentials and public HTTPS edge are not implemented. |
| NetherNet readiness probe | SOURCE PRESENT | HTTP(S) capability probe plus legacy RakNet fallback; signalling only, not UDP gameplay. |
| Paper adapter | SOURCE PRESENT / BLOCKED | No validated Java VM. |
| Geyser/Floodgate | SOURCE PRESENT / BLOCKED | Configuration exists; Java/Paper and current NetherNet cross-play are unverified. |
| Playit provider | SOURCE PRESENT / EXPERIMENTAL | Third-party managed service; not the target architecture. Dual TCP/UDP is explicitly rejected by this adapter. |
| MineHost TCP edge / reverse tunnel | NOT STARTED | Required for public Java TCP and Bedrock signalling behind CGNAT. |
| MineHost TURN | NOT STARTED | Required for Bedrock WebRTC relay fallback. |
| Foreground service / wake lock / thermal monitor | SOURCE PRESENT | Requires current Android/OEM sustained-device testing and Play policy review. |
| World backup/restore | SOURCE PRESENT | Must preserve real worlds and Axolotl-PM's `nethernet.key` identity file. |
| Unit tests | TEST SOURCES PRESENT | Must not be counted as passing until CI records results. |

## Immediate next actions

1. Inspect and fix the Android CI result; confirm PHP asset verification, APK packaging, `assembleDebug`, and `testDebugUnitTest`.
2. Verify the packaged PHP executable and ext-webrtc on a physical ARM64 Android phone.
3. Complete PHP/native dependency licence review.
4. Start Axolotl-PM 5.51.1 and join with a real Bedrock 1.26.60 client on LAN.
5. Preserve `nethernet.key` in backup/export/restore and server migration.
6. Prove a Java VM separately; then validate Paper and Geyser.
7. Implement MineHost-owned TCP reverse tunnel/HTTPS edge and TURN service.
8. Test Wi-Fi, cellular, CGNAT, external ISP and international paths.
9. Complete security, privacy, Play policy, release signing, capacity planning and incident-response reviews.

## Verification record

Reviewed: repository tree, README, engine adapters, runtime manager, networking providers, manifest, Gradle configuration, test sources, and project documentation.

**Not run in this audit:**
- Gradle build, Android lint, or unit tests;
- emulator or physical-device tests;
- PHP runtime launch or ext-webrtc module verification;
- Axolotl-PM server launch;
- real Bedrock client join or NetherNet ICE/DTLS gameplay;
- Java/Paper/Geyser launch or cross-play;
- Playit claim or external tunnel test;
- MineHost-owned TCP edge or TURN test;
- international latency benchmark.

No unrun test should be reported as passed.

## Release decision

**Release status: BLOCKED.**

A release candidate can be considered only after all critical blockers have evidence-backed resolution and the production definition in `MINEHOST_MASTER_ROADMAP.md` is met. A successful APK build alone is insufficient.
