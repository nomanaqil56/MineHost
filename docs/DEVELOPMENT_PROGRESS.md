# MineHost — Development Progress

Last source review: 2026-11-10  
Current authoritative plan: [MINEHOST_MASTER_ROADMAP.md](MINEHOST_MASTER_ROADMAP.md)  
Release gate: [PRODUCTION_READINESS.md](PRODUCTION_READINESS.md)

## Status meanings

- **SOURCE PRESENT:** code exists; this does not prove it builds or works.
- **UNIT TESTED:** a reproducible automated test ran and passed.
- **DEVICE VERIFIED:** tested on a physical Android device.
- **CLIENT VERIFIED:** a real Minecraft client connected and played.
- **EXTERNAL VERIFIED:** a real client connected from another ISP/public network.
- **BLOCKED:** a known missing runtime, platform capability, service, or other dependency prevents completion.
- **NOT STARTED:** no implementation/evidence yet.

Never promote a feature to a higher status without recording the command, commit, device/OS, server/client versions, network conditions, and result.

## Source inventory (reviewed 2026-11-10)

| Area | Source status | Current evidence / blocker |
|---|---|---|
| Kotlin / Compose app shell | SOURCE PRESENT | UI, navigation and feature screens exist; latest build not reproduced in this review. |
| Room / Hilt / repositories | SOURCE PRESENT | Data and DI layers exist; instrumented migration tests not recorded here. |
| Server engine abstraction | SOURCE PRESENT | Interface, registry and lifecycle classes exist. |
| Axolotl-PM Bedrock adapter | SOURCE PRESENT / BLOCKED | Engine now targets Axolotl-PM 5.51.1 / Bedrock 1.26.60, generates NetherNet config, and checks for PHP ext-webrtc. Build-time script pins Axolotl-PM PHP 8.4 Android ARM64 asset; CI packaging and physical-device launch are not yet verified. |
| Paper adapter | SOURCE PRESENT / BLOCKED | Requires a compatible JVM; adapter expects `libjava.so`, but no validated runtime is present. |
| Geyser config | SOURCE PRESENT / BLOCKED | Configuration generation exists; Java/Paper and bridge execution are not proven. |
| Runtime downloads | SOURCE PRESENT | PHAR/JAR/Playit download logic exists; runtime execution, pinning and trust model need further work. |
| Playit provider | SOURCE PRESENT / EXPERIMENTAL | Third-party provider implementation exists; not MineHost's intended final connectivity architecture and not device-verified. |
| MineHost-owned coordinator | NOT STARTED | Required for independent public connectivity. |
| MineHost-owned TCP edge / reverse tunnel | NOT STARTED | Required for Java TCP and Bedrock HTTPS signalling behind CGNAT. |\n| MineHost-owned TURN | NOT STARTED | Required as Bedrock WebRTC UDP fallback behind CGNAT/restrictive NAT. |
| Direct-first public routing | NOT STARTED | LAN helpers exist; public direct/NAT traversal not proven. |
| Wi-Fi/cellular recovery | PARTIAL SOURCE | Network observation exists; full tunnel recovery and player reconnection are not proven. |
| Local server readiness | SOURCE PRESENT | Protocol probe code exists; real engine responses are not verified. |
| World backup/restore | SOURCE PRESENT | Safety logic exists; test against real active worlds and engine save barriers is still required. |
| Foreground service / wake locks | SOURCE PRESENT | Must be validated across Android versions and OEM battery policies. |
| Thermal monitoring | SOURCE PRESENT | Listener exists; protective policy and sustained device behaviour need validation. |
| Automated Android build | NOT VERIFIED | No current CI result was available during this review. |
| Physical device testing | NOT DONE / NOT EVIDENCED | No current accepted device report. |
| Real Minecraft client testing | NOT DONE / NOT EVIDENCED | No current accepted client report. |
| External/international connectivity | NOT DONE / NOT EVIDENCED | No MineHost-owned relay acceptance report. |

## Verified vs unverified

The source review confirms the presence of implementation files, not successful runtime behaviour. The repository's Python verification harness constructs synthetic fixtures and simulated state transitions; those checks must not be described as end-to-end Minecraft server tests.

The earlier report of 40 pure-Kotlin tests is historical information from the prior development notes. It has not been independently rerun as part of this review. The Gradle build, Gradle unit tests, Android instrumentation, device tests, real server launches, and external joins must be rerun and logged.

## Immediate next tasks

1. [ ] Add CI for `assembleDebug` and `testDebugUnitTest`.
2. [ ] Make the synthetic Python harness label its results as simulations, not functional integration.
3. [ ] Run CI packaging and prove the bundled PHP executable and required extensions on physical ARM64 Android.
4. [ ] Prove a supported Java VM on ARM64 Android (separate milestone).\n5. [ ] Implement MineHost-owned TCP reverse tunnel and TURN infrastructure; do not use managed providers as the production path.
5. [ ] Complete physical-device local Bedrock acceptance test.
6. [ ] Implement and test one MineHost-owned public relay.
7. [ ] Test host on Wi-Fi and cellular behind CGNAT.
8. [ ] Validate current Bedrock transport and supported client matrix.
9. [ ] Complete security, dependency/licence, Play policy and release signing reviews.

Detailed acceptance criteria and phase exit gates are in [MINEHOST_MASTER_ROADMAP.md](MINEHOST_MASTER_ROADMAP.md).
