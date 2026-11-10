# MineHost — Development Progress

**Last reconciled:** 2026-11-10 (API 36 CI baseline plus latest runtime/lifecycle fixes)  
**Authoritative roadmap:** [MINEHOST_MASTER_ROADMAP.md](../MINEHOST_MASTER_ROADMAP.md)  
**Current verdict:** Active development; not production-ready.

## Verification labels

- **SOURCE PRESENT:** Implementation exists in the repository; it may still be incomplete.
- **PURE-LOGIC TESTED:** A unit/helper test has been executed. This does not prove Android integration.
- **ANDROID BUILD VERIFIED:** Gradle Android build completed successfully in CI or a documented local environment.
- **DEVICE VERIFIED:** Tested on a physical Android device.
- **REAL CLIENT VERIFIED:** A real compatible Minecraft client joined and played.
- **EXTERNAL NETWORK VERIFIED:** A client on a separate public network joined successfully.
- **NOT IMPLEMENTED / BLOCKED:** Required implementation or prerequisite is missing.

Never promote a feature to a higher verification level without a reproducible test record.

## Current implementation inventory

| Area | Source status | Verification | Notes |
|---|---|---|---|
| Kotlin/Compose application shell | SOURCE PRESENT | ANDROID BUILD VERIFIED | Navigation, screens, MVVM, Hilt and Room exist; debug APK assembled in CI; no physical-device test |
| Server dashboard/create/detail/console | SOURCE PRESENT | Not device verified | UI and ViewModels exist |
| Server engine abstraction | SOURCE PRESENT | Not end-to-end verified | Engine registry and lifecycle code exist |
| Legacy PocketMine adapter | SOURCE PRESENT, NOT CURRENT | Not device verified | Original upstream is archived; replace/retire after VanillaMine/Altay proof-of-concept selection |
| Paper adapter | SOURCE PRESENT, runtime BLOCKED | Not device verified | Test Paper 1.21.11 with an Android-compatible Java 21 runtime |
| Geyser configuration | PARTIAL SOURCE | Not operational | No proven Java runtime + Geyser process; cross-play must remain unavailable |
| Runtime downloads | SOURCE PRESENT | Pure logic only where tested | Release asset resolution and checksum validation exist; executable launchability remains unproven |
| Local network discovery | SOURCE PRESENT | 4 pure-Kotlin tests reported | No physical LAN client join recorded |
| Playit tunnel adapter | SOURCE PRESENT | 10 pure-Kotlin tests reported | Third-party dependency; live agent, claim, Android execution and external join unverified |
| MineHost-owned relay/control plane | NOT IMPLEMENTED | N/A | Required for final infrastructure-independence goal |
| Direct public connectivity/NAT traversal | NOT IMPLEMENTED | N/A | Requires protocol-specific design and tests |
| World storage | SOURCE PRESENT | No real Minecraft world test recorded | App-private storage layout exists |
| Backup/restore safeguards | SOURCE PRESENT | Pure tests reported | Real engine flush and real world restore not device verified |
| Foreground service/wake lock | SOURCE PRESENT | Not device verified | Android version, OEM and Play policy checks required |
| Thermal monitoring | SOURCE PRESENT | Not device verified | Needs sustained device tests |
| Server readiness probes | SOURCE PRESENT | 8 pure-Kotlin tests reported | Current Bedrock probe is RakNet-specific; validate selected current engine/client transport |
| Player tracking | SOURCE PRESENT | 6 pure-Kotlin tests reported | Log formats require capture from real server output |
| Server lifecycle serialization | SOURCE UPDATED | ANDROID BUILD VERIFIED | Start/stop serialized with a mutex; process-handle absence/early exit handled explicitly; CI compile passed |
| Android CI workflow | ADDED | API 36 DEBUG BASELINE VERIFIED; RELEASE MATRIX PENDING | Run 42754538024 passed unit tests, lint and debug APK; latest workflow also checks release lint, unsigned APK and AAB |
| Android 16 target API | SOURCE UPDATED | ANDROID BUILD VERIFIED | compileSdk/targetSdk 36; AGP 8.13.2, Gradle 8.13, Kotlin 2.3.21, Compose BOM 2026.04.01, KSP 2.3.9, Room 2.8.5, Hilt 2.57.2, JDK 17 |

## Test evidence currently documented

The previous development notes report **40/40 pure-Kotlin tests passing using kotlinc and JUnit outside Gradle**. This report has not been independently reproduced in this audit session.

The earlier standalone test set covers:
- Readiness probe packet framing and loopback fakes.
- Console command sanitization.
- Backup checksum and archive validation.
- Directory swap/rollback helpers.
- Tunnel parser, reconnect policy and reachability tracking.
- Player join/leave log parsing.
- Local network address selection.

In addition, the Gradle `testDebugUnitTest` task passed in CI run 42753556848. These tests do not prove that native runtimes launch or that a Minecraft client can join.

## Critical blockers

1. **Android device validation:** CI now builds the API 36 debug APK and passes unit tests/lint. Install it on physical ARM64 Android devices and validate service/runtime behaviour.
2. **Bedrock runtime:** A real Android ARM64 PHP runtime and launch mechanism must be selected and tested.
3. **Java runtime:** A complete compatible Android ARM64 JVM and launcher are not available/verified.
4. **Execution policy:** Validate native executable placement/launch on modern Android; a downloaded executable in app data and a shared object named `lib*.so` are not automatically executable.
5. **MineHost connectivity:** The only concrete provider adapter is Playit-specific. MineHost-owned relay/control-plane services are not implemented.
6. **Cross-play:** Geyser/Floodgate are not operationally integrated.
7. **Physical device:** No recorded end-to-end device acceptance test.
8. **International play:** No recorded external or international Minecraft client session.
9. **Release/legal:** Dependency licences, project licence file, privacy, Google Play policy, target API and production signing still need final review.

## Immediate next work

### P0 — Reproducible baseline (CI gate passed)
- [x] API 36 Android CI unit tests, lint and debug APK assembly pass (run 42754538024).
- [ ] Verify release lint, unsigned release APK and Android App Bundle in the expanded CI workflow.
- [x] Server lifecycle serialization compiles in the Android build.
- [x] Reconcile core roadmap, build and networking documentation.
- [ ] Keep CI green as runtime and relay work is added.

### P1 — One real local server (next priority)
- [ ] Build a current Android ARM64 PHP 8.4 runtime with the exact VanillaMine-required extensions (the available Android asset has an older chunkutils2 version).
- [ ] Cross-compile and package the Android ARM64 pm-nethernet helper.
- [ ] Validate the selected Bedrock engine/runtime/launcher on a physical device.
- [ ] Start it through MineHost on a physical device.
- [ ] Join from a second device on the same Wi-Fi.
- [ ] Verify persistence, backup/restore and screen-off stability.

### P2 — MineHost-owned public connectivity
- [ ] Specify the protocol adapters for the chosen Bedrock transport and Java TCP.
- [ ] Implement a minimal MineHost relay and Android client.
- [ ] Implement control-plane registration, short-lived credentials and relay allocation.
- [ ] Test Wi-Fi, cellular, CGNAT and international clients.
- [ ] Measure latency, jitter, loss, bandwidth and relay cost.

### P3 — Java and cross-play
- [ ] Validate Android ARM64 JVM execution.
- [ ] Prove Paper with a real Java client.
- [ ] Integrate and test Geyser/Floodgate only after the underlying runtimes and transports work.

## Change log for this branch

- Added `MINEHOST_MASTER_ROADMAP.md` as the product and engineering source of truth.
- Rewrote README claims to distinguish vision from current implementation.
- Reworked production readiness and networking docs to reflect actual implementation and missing MineHost-owned connectivity.
- Added Android CI workflow for Gradle unit tests, lint and debug APK assembly.
- Serialized server start/stop lifecycle operations and added explicit handling for missing or already-exited process handles.
- Corrected runtime readiness reporting so downloaded PHAR/JAR archives are not presented as complete runnable engines without PHP/Java launchers.
- Corrected Playit executable detection to avoid relying on chmod for app-data binaries.
- Made diagnostics download gating atomic to prevent concurrent downloads from rapid taps.
- Improved shutdown handling: wait after forced termination and do not clear the active server state while the process remains alive.


## Latest verification record

- Latest API 36 debug baseline: [run 42754538024](https://github.com/nomanaqil56/MineHost/actions/runs/42754538024) — unit tests, lint and debug APK assembly all passed.
- The expanded workflow now also runs release lint and builds the unsigned release APK and Android App Bundle; its latest run is pending for the current branch head.
- Verification level: ANDROID BUILD VERIFIED only. No physical Android runtime launch, Minecraft client join, international connection or relay test has been performed.
- The current branch is `work/production-readiness`; it has not been merged to main.
