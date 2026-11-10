# MineHost — Development Progress Tracker

> **Main-branch status update (2026-11-10):** The README and runtime guards have been corrected. Bedrock is blocked until a real Android ARM64 PHP CLI runtime is available; Java is blocked until a complete Android-compatible JVM and executable launcher are available. Shared libraries named `libphp.so`, `libjava.so`, or `libplayit.so` are not treated as CLI executables. A GitHub Actions workflow has been added, but its first run and all recent Android-dependent changes still require verification. No physical-device or real-client test has been recorded.

## Status Classification Legend
- **IMPLEMENTED:** Source code and architecture completely implemented.
- **AUTOMATED TESTED:** Unit / Protocol / Logic verification suite passed.
- **EMULATOR TESTED:** Verified in Android Virtual Device environment.
- **PHYSICAL DEVICE TESTED:** Verified on real physical Android ARM64 hardware with physical Minecraft client.
- **EXTERNAL NETWORK TESTED:** Verified through public internet from an external network connection.
- **NOT IMPLEMENTED:** Planned feature not yet written.
- **BLOCKED:** Blocked by external hardware, missing binary, or OS constraint.

---


> **Audit correction (2026-11-09):** In the table below, "AUTOMATED TESTED" means only that unit tests exist for helper logic (config generators, zip utilities, ANSI parsing). **No row has been verified on a device or with a real Minecraft client**, and the build/test results in "Build & Test Verification Milestones" were reported by the previous author and **not reproduced** in the audit. The Bedrock and Java engines are **BLOCKED** because no PHP/Java runtime is bundled. See [PRODUCTION_READINESS.md](PRODUCTION_READINESS.md) for the verified status.

---

## Live Implementation Inventory

| Subsystem / Feature | Current Status | Verification Level | Notes & Realistic Limitations |
| :--- | :--- | :--- | :--- |
| **Android Clean Architecture Foundation** | **IMPLEMENTED** | AUTOMATED TESTED | MVVM, Room Database (5 DAOs/entities), Hilt DI, Navigation Compose |
| **UI Design System & Screens** | **IMPLEMENTED** | AUTOMATED TESTED | Jetpack Compose Material 3 dark gaming aesthetic, server wizard, console, diagnostics |
| **Runtime Manager (Downloads & Hashes)** | **PARTIALLY IMPLEMENTED** | SOURCE REVIEWED | Release metadata, digest checks and staging exist; PHP/JVM runtime provisioning and Android launch are not complete. |
| **Bedrock Axolotl-PM Engine** | **BLOCKED** | SOURCE ONLY | Axolotl-PM 5.51.1 PHAR source is pinned; Android ARM64 PHP 8.4 candidate is selected but not packaged or launched. |
| **Bedrock Physical Device Gameplay** | **NOT IMPLEMENTED** | PENDING HARDWARE | Requires physical Android device test (Phase 16) |
| **World Storage & File Layout** | **IMPLEMENTED** | AUTOMATED TESTED | App-private storage `/files/minehost/servers/<id>/worlds/`, tested against restart persistence |
| **World Atomic Backup & Restore** | **IMPLEMENTED** | AUTOMATED TESTED | ZIP snapshotting, SHA-256 manifest calculation, safety pre-restore rollback snapshot |
| **World Validation (Zip Slip & Formats)** | **IMPLEMENTED** | AUTOMATED TESTED | Safe extraction with canonical path validation guarding against Zip Slip traversal |
| **Playit optional test adapter** | **PARTIALLY IMPLEMENTED** | NOT DEVICE TESTED | Release lookup and process adapter code exist; Android executable policy, Bionic compatibility and external join remain unverified. Not the final network design. |
| **Playit Worldwide External Join** | **NOT IMPLEMENTED** | PENDING NETWORK | Requires live claim and external player join across public internet |
| **Java PaperMC Engine** | **BLOCKED** | SOURCE ONLY | Paper downloader/configuration and EULA guard exist; complete Android ARM64 JVM and executable launcher are missing. |
| **Java ARM64 Runtime Execution** | **BLOCKED** | PENDING RUNTIME | Requires desktop JRE 17/21 distribution for Android bionic/musl |
| **Cross-Play (GeyserMC Bridge)** | **PARTIALLY IMPLEMENTED** | NOT IMPLEMENTED | Configuration generator written; runtime bridge execution pending Java runtime milestone |
| **Android Foreground Service & WakeLock** | **IMPLEMENTED** | AUTOMATED TESTED | `specialUse|dataSync` FGS, partial CPU wake lock, high-performance Wi-Fi lock |
| **Hardware Thermal Monitoring** | **IMPLEMENTED** | AUTOMATED TESTED | `PowerManager.OnThermalStatusChangedListener` listener reporting device thermal throttling |
| **Live Monospace Terminal Console** | **IMPLEMENTED** | AUTOMATED TESTED | Monospace log stream, ANSI code stripper, stdin server command dispatch |
| **Player Administration** | **IMPLEMENTED** | AUTOMATED TESTED | Database tracking, `/kick`, `/op`, `/ban`, `/whitelist` command dispatch |

---

## Detailed Audit of Replaced Simulations
1. **Removed Fake Bedrock Process:** Eliminated the infinite sleep shell mock (`echo ...; while true; do sleep 5; done`). The engine now strictly validates the presence of the real PHP binary and `PocketMine-MP.phar`.
2. **Removed Fake Java Process:** Eliminated the shell sleep loop. The engine now verifies real Paper JAR presence.
3. **Removed Unconditional Installation Checks:** Removed `|| true` from `BedrockEngine.isInstalled()` and `JavaEngine.isInstalled()`. Both engines now inspect actual files on disk via `RuntimeManager`.
4. **Removed Random Public Endpoints:** Removed the random hostname generator (`minehost-node-XXXX.joinmc.link`). `PlayitTunnelProvider` now only exposes endpoints parsed from genuine agent output or reports `DISCONNECTED` / `ERROR`.
5. **Added Playit Agent Claim Workflow:** The application detects the claim URL output by the agent and exposes it in the UI so the user can claim their tunnel on `playit.gg`.
6. **EULA Consent Guard:** Java server configuration no longer silently auto-accepts the Mojang EULA; user consent is explicitly checked.
7. **Port Conflict Validator:** `ServerController` now tests whether local UDP 19132 or TCP 25565 is already occupied before attempting to start a server.

---

## Build & Test Verification Status

Earlier repository notes claimed that `assembleDebug`, `testDebugUnitTest`, and a Python verification script had passed. Those results were not reproduced in the latest audit and must not be treated as the current main-branch baseline.

The latest documented standalone verification reports **40/40 pure-Kotlin tests passing with kotlinc and JUnit outside Gradle**. This does not compile Android-dependent source or verify Android integration.

A GitHub Actions workflow now runs:
1. `testDebugUnitTest`
2. `lintDebug`
3. `assembleDebug`

**Status of the new workflow:** pending its first confirmed successful run. Until then, Gradle build and Android lint are **NOT VERIFIED** for the latest main commit.

No emulator, physical Android device, native runtime launch, Minecraft client join, or external-network session has been verified.

---

## Next Milestone: Android Bedrock Runtime Packaging

1. Inspect the pinned Axolotl-PM PHP 8.4 Android ARM64 archive and verify its ELF type, interpreter and extension set.
2. Choose a packaging/loader strategy that is permitted by Android's executable policy.
3. Package or embed the runtime and prove `php -v`, required extensions and Axolotl-PM PHAR startup.
4. Only then install the app on a physical Android ARM64 device and perform local Bedrock join, world save and restart persistence tests.


## Latest session (2026-11-10)
- Added an Android CI workflow and repaired the Unix Gradle wrapper script.
- CI exposed and fixed a Kotlin DSL `Properties` import and a missing Room migration import. The latest CI run is still pending verification.
- Tightened PHP/Java/Playit runtime detection so shared-library names are not accepted as CLI executables.
- Switched the Bedrock source to Axolotl-PM 5.51.1 / Bedrock 1.26.60 and pinned the PHAR release.
- Selected the matching Axolotl-PM PHP 8.4 Android ARM64 archive; its SHA-256 and packaging constraints are documented in [Decision 0001](decisions/0001-bedrock-engine-and-php-runtime.md).
- No physical-device server launch or real Minecraft client join has been verified.
