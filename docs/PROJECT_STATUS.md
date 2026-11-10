# MineHost — Project Progress Report

**Last audited:** 2026-11-10  
**Repository:** `nomanaqil56/MineHost` · **Branch:** `main`  
**Audited baseline:** `4b6f498f24935f91f547875818f56e361357cf31`  
**Purpose:** Single authoritative project status. Physical-device evidence is recorded in [DEVICE_TEST_RESULTS.md](../device-evidence/DEVICE_TEST_RESULTS.md).

## Executive status

MineHost has an Android Bedrock server implementation built around Axolotl-PM 5.51.1 and a pinned PHP 8.4.27 ZTS ARM64 runtime. The retained physical-device report records successful runtime, engine, UI lifecycle, world persistence, LAN signaling, and background-service checks. A real second-client gameplay test is still blocked, so Phase 1 is **not yet 100% accepted**.

This change corrects NetherNet's UDP ICE-mux port to follow the selected server port rather than always using 19132. CI and a fresh device run must pass after this change before the updated code can be considered verified on-device.

## Phase 1 — Android Bedrock hosting

### Implemented

- Axolotl-PM 5.51.1 PHAR pinned to SHA-256 `bcfbd0deff349c2b25b1688299a7d54f1fc8a080d2f95e19884f8e341319021a`.
- PHP 8.4.27 ZTS Android ARM64 runtime pinned to SHA-256 `45b462ff5e290ee06d34eced589c325d27a2743185acab53acab291b0188e181`.
- PHP executable packaged as an APK native library and checked for ELF64/AArch64 executable format.
- Runtime launch validation, PHP version validation, and 17 required extension checks.
- Bedrock configuration generation, input validation, Xbox authentication property, NetherNet signaling, and ICE UDP multiplexing.
- Server process supervision, protocol readiness checks, foreground service integration, wake locks, thermal monitoring, console commands, and lifecycle state.
- LAN IPv4 address discovery.
- World directory management, ZIP backups, checksums, restore staging, safety snapshot, and rollback implementation.
- Unit tests for configuration, runtime contract, readiness packet handling, local network selection, world swaps, ZIP validation, command sanitization, and supporting utilities.

### Physical-device evidence on record

The retained evidence identifies a Vivo I2221 / iQOO Z7 Pro 5G, Android 16 / API 36, ARM64. It records PHP runtime and extension checks, Axolotl-PM startup, UI state, world persistence, LAN signaling, foreground operation, and three clean start/stop cycles. See [device test results](../device-evidence/DEVICE_TEST_RESULTS.md).

### Acceptance gates

| Gate | State | Closure requirement |
|---|---|---|
| Android build, lint, JVM unit tests | Previous main CI PASS; this change awaits CI | Latest main CI passes |
| Pinned PHP runtime and 17 extensions | PASS in retained device report | Re-run after APK/runtime changes |
| Axolotl-PM process startup and readiness | PASS in retained device report | Re-run on updated APK |
| In-app start/stop lifecycle | PASS in retained device report | Re-run on updated APK |
| World generation and persistence | PASS in retained device report | Re-run on updated APK |
| LAN signaling endpoint | PASS in retained device report | A real client join is still required |
| NetherNet UDP/WebRTC gameplay | **BLOCKED / NOT VERIFIED** | Real Bedrock client joins and plays |
| Live backup and restore | Implementation exists; device acceptance evidence incomplete | Backup, restore, rejoin, and verify world |
| Background / screen-off hosting | PASS in retained device report | Repeat on updated APK |
| Repeated lifecycle | Three cycles reported PASS | Repeat on updated APK |

**Phase 1 is not complete until second-client gameplay and live backup/restore pass.** A successful HTTP `GET /v1/join`, RakNet ping, UDP socket check, or process check is not a substitute for a real NetherNet client session.

### Technical limitations

- Axolotl-PM is PocketMine-derived, not Mojang's vanilla Bedrock Dedicated Server. Vanilla feature parity (mob AI, redstone, and other behaviours) is not guaranteed.
- NetherNet uses TCP HTTP signaling and separate UDP WebRTC game traffic. LAN discovery uses UDP 7551; that is not the gameplay UDP port.
- The app's readiness probe verifies signaling capability, not completed ICE/DTLS/SCTP negotiation or gameplay.
- Online backup pauses saving, sends a flush command, and waits a fixed five seconds. There is no server acknowledgement handshake; do not describe this as a guaranteed consistent snapshot for arbitrary world sizes.
- Android OEM battery management can affect long-running background processes.

## Phase 2 — public connectivity (deferred)

Existing code provides a tunnel abstraction, a Playit adapter, basic endpoint monitoring, and LAN address discovery. MineHost-owned control plane, host authentication, STUN/NAT classification, ICE coordination, TURN/relay fleet, secure host-to-relay client, relay failover, and external player acceptance are not implemented.

NetherNet's ICE/TURN constraints must be respected: Axolotl-PM documents that ICE UDP multiplexing cannot be combined with TURN. Do not add TURN while `ice-udp-mux: true` remains enabled.

## Java hosting and cross-play (deferred)

Java hosting is not accepted: a verified Android-compatible JVM and Paper execution path are not complete. Geyser configuration code does not mean Geyser is running. Cross-play remains unavailable.

## CI baseline

At the audited baseline, GitHub Actions run `42816003231` completed successfully on commit `4b6f498f24935f91f547875818f56e361357cf31`. It ran JVM unit tests, Android lint, debug APK assembly, APK runtime payload verification, and the pinned PHP archive/QEMU smoke test. The next commit must receive its own green CI run.

## Next actions

1. Run CI for this configured-port correction.
2. Install that exact APK on the target phone and repeat startup, signaling, stop/restart, and persistence.
3. Join from a second Bedrock client on the same LAN; verify world entry, chunks, movement, block placement/breaking, disconnect, and rejoin.
4. Verify backup and restore against the generated world.
5. Only then mark Phase 1 100% accepted.
6. Start Phase 2 after Phase 1 acceptance; keep Java/cross-play separate.

## Repository hygiene

- One project status report: this file.
- One consolidated device test result: `device-evidence/DEVICE_TEST_RESULTS.md`.
- Original evidence artifacts are retained under `device-evidence/evidence/`; duplicate summaries and redundant captures are removed.
- Source, tests, build configuration, and useful technical guides are retained.
