# MineHost — Complete Project Progress Report

**Last updated:** 2026-11-10  
**Repository:** `nomanaqil56/MineHost`  
**Active workstream:** Phase 1 — Bedrock local-hosting completion and acceptance  
**Authoritative device record:** [DEVICE_TEST_RESULTS.md](../device-evidence/DEVICE_TEST_RESULTS.md)  
**Evidence archive:** [device-evidence/evidence/](../device-evidence/evidence/)

> This report is the single source of truth for implementation status, phase scope, known limitations, and remaining work. Source-code presence, unit tests, CI, and physical-device acceptance are tracked separately; none is used as a substitute for another.

## 1. Executive status

MineHost is an Android application that runs a Minecraft Bedrock-compatible server locally on an ARM64 Android device. The current implementation uses Axolotl-PM 5.51.1 and a pinned PHP 8.4.27 ZTS runtime packaged in the APK.

The latest recorded `main` commit at the start of this workstream was `14d99eca3f4b569cef86d8a40e2e687d966c588b`. Its Android CI run **42817752559 passed**. Existing physical-device evidence records successful runtime, server startup, host-side signaling, world persistence, foreground operation, and three start/stop cycles on a Vivo iQOO Z7 Pro 5G running Android 16 / API 36.

**Phase 1 is not accepted as complete.** The retained device evidence is from an earlier source commit, no real second Bedrock client gameplay session is recorded, and backup/restore acceptance is incomplete. The latest configured NetherNet UDP-port change must also be installed and retested on the phone.

The current priority is to finish Phase 1. Do not begin Phase 2 public relay development, Java hosting, or cross-play while these acceptance gates remain open.

## 2. Phase 1 — Bedrock local hosting

### 2.1 Implemented in source

- Axolotl-PM 5.51.1 PHAR pinned to SHA-256 `bcfbd0deff349c2b25b1688299a7d54f1fc8a080d2f95e19884f8e341319021a`.
- PHP 8.4.27 ZTS Android ARM64 runtime pinned to SHA-256 `45b462ff5e290ee06d34eced589c325d27a2743185acab53acab291b0188e181`.
- Runtime packaged as `lib/arm64-v8a/libphp.so`; CI checks the APK payload and QEMU smoke-tests the pinned archive.
- Runtime executable, version, ZTS, and required PHP extension validation.
- Bedrock server configuration generation, input validation, Xbox authentication setting, and configurable NetherNet ICE UDP mux port range.
- Managed process launch, stdout/stderr capture, command input, shutdown, crash handling, and protocol-level readiness probes.
- Android foreground service, notification controls, wake-lock and thermal-monitoring integrations.
- LAN IPv4 address discovery and pure unit tests for address selection.
- World directory management, ZIP backup creation, SHA-256 validation, staged restore, safety snapshot, and rollback-capable world replacement.
- JVM unit tests for runtime contract, configuration, readiness packet parsing, networking selection, world swap, ZIP validation, and supporting utilities.

### 2.2 Evidence already on record

The historical device report records:
- PHP 8.4.27 ZTS runtime and 17 required modules.
- Axolotl-PM 5.51.1 installation/integrity and process startup.
- Configuration generation and server ONLINE UI state.
- World generation and persistence across recorded restarts.
- Successful HTTP `GET /v1/join` signaling probe.
- Foreground/background operation and three clean start/stop cycles.

These results are historical evidence from commit `74fce0c34ebde5fd388e8fd8ecb19c26713d01e3`, not a fresh test of the latest APK. Details and evidence references are in [DEVICE_TEST_RESULTS.md](../device-evidence/DEVICE_TEST_RESULTS.md).

### 2.3 Phase 1 acceptance gates

| Acceptance gate | Current evidence | Required to close |
|---|---|---|
| Latest Android CI | PASS on baseline commit `14d99ec` | CI must pass on the final Phase 1 branch/merge commit |
| PHP runtime and 17 modules | Historical device PASS | Re-run on APK built from final Phase 1 source |
| Axolotl-PM launch and readiness | Historical device PASS | Re-run on final APK |
| Server start/stop lifecycle | Historical device PASS; three cycles recorded | Re-run at least three cycles on final APK |
| Configurable NetherNet UDP port | Source fix and unit/CI coverage | Inspect generated config on device for default and non-default ports |
| LAN address selection | Unit tests; implementation hardened to exclude cellular/VPN/unknown interfaces | Confirm correct Wi-Fi/hotspot address on target phone |
| Real Bedrock client gameplay | **NOT TESTED / BLOCKING** | Second client joins, loads chunks, moves, places/breaks blocks, disconnects, and rejoins |
| World persistence | Historical device PASS | Re-run after real client gameplay and clean restart |
| Offline backup and restore | Implementation present; acceptance incomplete | Create backup, restore it, and verify the restored world |
| Online backup consistency | Fixed-delay implementation only; no server flush acknowledgement | Do not claim guaranteed online snapshot consistency; implement acknowledgement or use safe stopped-server backup |
| Screen-off/background hosting | Historical device PASS | Repeat on final APK and record device power settings |

**Phase 1 exit criteria:** every required gate above must have current, reproducible evidence. The real-client gameplay and backup/restore tests cannot be marked PASS based on source code, synthetic tests, a port probe, or HTTP signaling alone.

### 2.4 Known technical limitations

1. Axolotl-PM is PocketMine-derived, not Mojang's vanilla Bedrock Dedicated Server. Vanilla feature parity (including mob AI, redstone, and other behaviours) is not guaranteed.
2. `GET /v1/join` verifies the HTTP signaling endpoint only. It does not prove ICE candidate gathering, DTLS/SCTP setup, world entry, chunk streaming, or gameplay.
3. NetherNet uses TCP HTTP signaling and separate UDP WebRTC game traffic. The signaling port, ICE UDP mux port range, and LAN discovery port are distinct concepts.
4. Online backup currently sends `save-off` and `save-all`, then waits a fixed five seconds. There is no server acknowledgement handshake. This is not a guaranteed consistent snapshot for arbitrary world sizes.
5. Android OEM battery management can affect long-running background processes.
6. Local LAN availability does not imply internet reachability.

## 3. Phase 2 — Public connectivity (not started)

The application has a tunnel-provider abstraction, legacy Playit adapter code, and endpoint-monitoring foundations. The final MineHost-owned public networking system is not implemented.

Required future work includes:
- MineHost control plane and authenticated host agent.
- Direct-connect and public-address/CGNAT classification.
- NetherNet-compatible ICE coordination and NAT traversal design.
- Relay service and UDP data path, session management, monitoring, and failover.
- External network and international client acceptance testing.

**Protocol constraint:** Axolotl-PM's current ICE UDP multiplexing configuration has documented TURN limitations. Do not introduce TURN without first resolving compatibility with `ice-udp-mux: true`.

## 4. Java hosting and cross-play (deferred)

Java hosting is not operational. The repository contains Paper download/configuration and engine scaffolding, but a verified Android ARM64 JVM and a working Paper process-launch path are not available. A `libjvm.so` file alone is not a runnable JVM.

Geyser/Floodgate configuration scaffolding does not mean those services are running. Cross-play depends on operational Java hosting and a separately validated bridge.

## 5. Build and automated verification

The canonical workflow is `.github/workflows/android-ci.yml`. It runs:
- `testDebugUnitTest`
- `lintDebug`
- `assembleDebug`
- APK verification for the packaged ARM64 PHP CLI
- Pinned Android PHP archive inspection and QEMU smoke testing

Latest verified baseline before the Phase 1 cleanup branch:
- Commit: `14d99eca3f4b569cef86d8a40e2e687d966c588b`
- Workflow run: [42817752559](https://github.com/nomanaqil56/MineHost/actions/runs/42817752559)
- Result: **SUCCESS**

CI validates build and automated checks. It does not replace Android device or real Minecraft client acceptance.

## 6. Repository structure and documentation policy

Keep one canonical file for each reporting purpose:
- `docs/PROJECT_STATUS.md` — this complete project progress report and phase roadmap.
- `device-evidence/DEVICE_TEST_RESULTS.md` — one consolidated physical-device test record.
- `device-evidence/evidence/` — original raw logs, screenshots, UI hierarchy captures, and test evidence.

Keep application source, JVM tests, Gradle wrapper/configuration, the CI workflow, and directly useful build/runtime tooling. Remove stale, duplicate, machine-specific, or misleading reports/harnesses; do not delete production source or required build files merely to reduce the file count.

## 7. Immediate execution order

1. Harden LAN address selection so cellular, VPN, and unrelated private interfaces are not advertised as the local multiplayer address.
2. Run JVM unit tests, Android lint, and debug APK build on the Phase 1 work branch.
3. Install that exact APK on the target ARM64 phone.
4. Recheck PHP version/ZTS/modules, Axolotl-PM startup, default and non-default NetherNet port configuration, lifecycle, world persistence, and screen-off operation.
5. Join from a second Bedrock client and complete the full gameplay checklist.
6. Complete offline backup/restore verification; document the online-backup acknowledgement limitation accurately.
7. Add dated evidence for every final acceptance result and update the device report.
8. Mark Phase 1 complete only after all blocking physical-device tests pass.
9. Begin Phase 2 public connectivity only after Phase 1 acceptance.

**Current decision: Continue Phase 1. Do not mark it 100% until the physical-device acceptance gates are actually completed.**
