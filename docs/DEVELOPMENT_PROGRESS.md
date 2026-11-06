# MineHost — Development Progress Tracker

## Status Classification Legend
- **IMPLEMENTED:** Source code and architecture completely implemented.
- **AUTOMATED TESTED:** Unit / Protocol / Logic verification suite passed.
- **EMULATOR TESTED:** Verified in Android Virtual Device environment.
- **PHYSICAL DEVICE TESTED:** Verified on real physical Android ARM64 hardware with physical Minecraft client.
- **EXTERNAL NETWORK TESTED:** Verified through public internet from an external network connection.
- **NOT IMPLEMENTED:** Planned feature not yet written.
- **BLOCKED:** Blocked by external hardware, missing binary, or OS constraint.

---

## Live Implementation Inventory

| Subsystem / Feature | Current Status | Verification Level | Notes & Realistic Limitations |
| :--- | :--- | :--- | :--- |
| **Android Clean Architecture Foundation** | **IMPLEMENTED** | AUTOMATED TESTED | MVVM, Room Database (5 DAOs/entities), Hilt DI, Navigation Compose |
| **UI Design System & Screens** | **IMPLEMENTED** | AUTOMATED TESTED | Jetpack Compose Material 3 dark gaming aesthetic, server wizard, console, diagnostics |
| **Runtime Manager (Downloads & Hashes)** | **IMPLEMENTED** | AUTOMATED TESTED | Streaming downloads with SHA-256 validation, atomic replace for PocketMine, Paper & Playit |
| **Bedrock PocketMine-MP Engine** | **IMPLEMENTED** | AUTOMATED TESTED | Native process execution, `server.properties` and `pocketmine.yml` generation, stdin command piping |
| **Bedrock Physical Device Gameplay** | **NOT IMPLEMENTED** | PENDING HARDWARE | Requires physical Android device test (Phase 16) |
| **World Storage & File Layout** | **IMPLEMENTED** | AUTOMATED TESTED | App-private storage `/files/minehost/servers/<id>/worlds/`, tested against restart persistence |
| **World Atomic Backup & Restore** | **IMPLEMENTED** | AUTOMATED TESTED | ZIP snapshotting, SHA-256 manifest calculation, safety pre-restore rollback snapshot |
| **World Validation (Zip Slip & Formats)** | **IMPLEMENTED** | AUTOMATED TESTED | Safe extraction with canonical path validation guarding against Zip Slip traversal |
| **Playit.gg Agent Integration** | **IMPLEMENTED** | AUTOMATED TESTED | Real agent execution (`playit-cli`), claim URL detection, stdout endpoint parser (no fake endpoints) |
| **Playit Worldwide External Join** | **NOT IMPLEMENTED** | PENDING NETWORK | Requires live claim and external player join across public internet |
| **Java PaperMC Engine** | **IMPLEMENTED** | AUTOMATED TESTED | PaperMC v2 API downloader, configuration generator, eula consent guard, JVM memory checks |
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

## Next Milestone: Physical Device Bedrock Testing
Target: Follow Part 16 sequence on physical Android ARM64 device:
1. Install APK
2. Run initial setup and download PocketMine-MP in Diagnostics
3. Start Bedrock server
4. Connect Bedrock client over local Wi-Fi to test world generation, block placement, and restart persistence.
