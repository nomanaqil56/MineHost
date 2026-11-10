# MineHost: Production Readiness

_Original audit: 2026-11-09. Updated for the main branch on 2026-11-10; older findings below are historical where superseded by the current-main correction note._

## Verdict

**NOT READY: CRITICAL BLOCKERS REMAIN.**

MineHost has a well-organised app shell (UI, database, services, backups), but **no Minecraft server has ever been shown to run through it**, and several blockers mean it cannot run one today. Nothing here has been built or run on a device in this audit (see "What was and was not verified").

## Current main-branch correction (2026-11-10)

The first Vivo I2221 Android 16 device attempt exposed a PHP configuration mismatch: the app used the upstream php.ini via `-c`, while CI verified static CLI modules using `-n`. On-device PHP consequently reported sockets/yaml/zip/zlib/webrtc missing. The fix now uses `-n` for version checks, module validation and actual PHAR startup, and no longer packages the external ini. The previous committed `PHYSICAL_DEVICE_TEST_REPORT.txt` overstated what its separate manual ADB script proved and is withdrawn.


The original audit below identified several false-positive runtime checks. Follow-up commits on `main` have hardened those checks:

- `RuntimeManager.getPhpExecutable()` accepts only the pinned static ARM64 PHP CLI packaged by Gradle into `ApplicationInfo.nativeLibraryDir/libphp.so`; it rejects writable app-data candidates and validates the ELF header.
- `RuntimeManager.getJavaExecutable()` now requires an actual `bin/java`-style CLI launcher. `libjava.so` alone is no longer accepted.
- `BedrockEngine` and `JavaEngine` now return explicit runtime-blocked errors instead of attempting to execute those shared libraries.
- Playit detection no longer treats `libplayit.so` as a standalone agent executable.
- The README now describes these features as goals/scaffolding, not working production capabilities.
- Bedrock release source now targets Axolotl-PM 5.51.1 for Bedrock 1.26.60. The original PMMP upstream was archived; see [Decision 0001](decisions/0001-bedrock-engine-and-php-runtime.md).
- The matching Axolotl PHP-Binaries Android ARM64 PHP 8.4 archive is SHA-256 verified and has passed a QEMU ARM64 smoke test: PHP 8.4.27 ZTS, required PocketMine/Axolotl extensions (including `webrtc`), and bundled `php.ini` load successfully.
- Gradle downloads and verifies that archive at build time and packages the static CLI in the APK `arm64-v8a` native-library directory. MineHost uses `-n` consistently for the compiled-in modules; no external php.ini is packaged.
- The Axolotl-PM PHAR downloader is pinned to release 5.51.1 and a hard-coded SHA-256; launch re-verifies the installed PHAR.
- An Android CI workflow has been added for unit tests, lint and debug APK assembly.

**Verification status (2026-11-10):** the first real-device attempt exposed the php.ini versus `-n` mismatch. Source now aligns module validation and server launch, and CI has been strengthened to exercise the exact no-ini mode and pinned PHAR. The corrected build still requires CI completion and another device test. No in-app server start or real Minecraft client join has been verified. MineHost remains **NOT PRODUCTION READY**.



## Blockers (why a real server cannot start today)

| # | Blocker | Evidence | Needed to resolve |
|---|---|---|---|
| 1 | **PHP CLI is packaged but not device-verified.** The pinned static ARM64 PHP CLI is bundled into the APK at build time and passes QEMU smoke tests; Android may still reject its launch on a particular OS/OEM build. | `app/build.gradle.kts`, `RuntimeManager.getPhpExecutable()`, `BedrockEngine.start()` | Install on a physical ARM64 device, prove `php -v`, launch Axolotl-PM, and join with a real Bedrock client. |
| 2 | **No Java CLI runtime is available.** A complete Android-compatible ARM64 JVM and executable `bin/java` launcher are not present. A shared library alone is not accepted. | `RuntimeManager.getJavaExecutable()`, `JavaEngine.start()` | Validate a complete JVM distribution and process-launch strategy on Android. Java is therefore **BLOCKED**. |
| 3 | **Android native-library execution path is not yet device-verified.** The PHP CLI is now embedded in the APK's extracted native-library directory rather than downloaded to writable app storage. | `jniLibs/arm64-v8a/libphp.so`, `nativeLibraryDir` | Verify `execve` succeeds on supported Android API levels and OEM devices. |
| 4 | **Playit Android execution remains unverified.** The current downloader resolves release assets from metadata and attempts an execution check, but Android/Bionic compatibility and W^X policy have not been proven on a device. | `RuntimeManager.downloadPlayitAgent()` | Validate the actual ARM64 release asset and launch path; keep Playit optional and temporary, not the final MineHost network. |
| 5 | **Download supply-chain policy is partially hardened.** Axolotl-PM release tag, asset name, canonical URL and published SHA-256 are pinned in source; the downloaded PHAR and every launch-time PHAR are checked against the pinned digest. Other downloads (including Playit and Paper) still accept optional upstream digests and need separate policy review. | `RuntimeManager.downloadFileWithProgress()`, `BedrockRuntimeContract` | Extend the same pinned integrity policy to every executable/runtime component; assess signed provenance. |
| 6 | **No physical-device or real-client test has been done**, so every end-to-end claim is unproven. | n/a | Run the Bedrock acceptance test in the brief (Part 5.5). |

## Changes made in this audit (not yet built or run)

| File | Previous problem | Change | Verification |
|---|---|---|---|
| `server/ServerProcess.kt` | After a 60 s port probe, **any live process was reported ONLINE** (`if (isAlive) listener.onStarted()`). The Bedrock "probe" treated "cannot bind the UDP port" as "server ready", which is also true for any other app using that port. A deliberate stop was reported as a **crash** (signal exit code). | Readiness now requires a real protocol response (RakNet pong / Java Server List Ping). If the process is alive but silent for the timeout (default 180 s), startup is reported as an **error** and the process is killed. A requested stop is reported as a clean exit. | Logic unit-tested in `ReadinessProbeTest` (not run here). Needs a real server to prove. |
| `server/ReadinessProbe.kt` (new) | n/a | RakNet unconnected ping/pong and Java Server List Ping implementations plus VarInt helpers. | `ReadinessProbeTest`: hand-built packets plus loopback fake responders (not run here). Fake responders do **not** prove a real PocketMine/Paper server answers. |
| `server/ServerEngineRegistry.kt`, `ServerController.kt`, `CreateServerScreen.kt` | `CROSSPLAY` silently mapped to the **Bedrock-only** engine, so "cross-play" servers were not cross-play. | Cross-play now returns "unsupported" with an explanation, and the option on the create screen is disabled and labelled unavailable. | Code review only. |
| `server/ServerController.kt` | The result of `engine.configure()` was ignored, so a failed config write still started the server. | A failed configuration now aborts startup with an error. | Code review only. |
| `world/WorldBackupManager.kt` | **Restore deleted the live world first**, then extracted straight into it. A failed or corrupt extraction left the user with no world and no automatic rollback. No checksum check before restore. | Restore now: verifies the archive checksum, extracts to a temp directory, checks it is non-empty, makes a safety snapshot (aborting if that fails), swaps by rename with rollback, and only then deletes the old world. | `ZipUtilsAndValidationTest` covers checksum/tamper detection (not run here). The swap logic is **untested**. |
| `core/util/ZipUtils.kt` | No way to verify an archive against its recorded checksum. | Added `computeContentChecksum` matching how backups are hashed. | `ZipUtilsAndValidationTest` (not run here). |
| `features/worlds/BackupsViewModel.kt` | Restore was allowed while the server was running. | Restore is refused while that server is starting, running or stopping. | Code review only. |

## Feature status (source-verified; none device-verified)

| Feature | Actual status | Notes |
|---|---|---|
| App shell, navigation, Compose UI, Room, Hilt | IMPLEMENTED BUT UNVERIFIED | Not built in this audit. |
| Bedrock engine (Axolotl-PM) | **BLOCKED PENDING DEVICE TEST** | Static PHP 8.4 CLI is packaged in ARM64 APK builds and passes QEMU module checks; the PHAR must still be installed and the process launched on a real device. |
| Java engine (Paper) | **BLOCKED** | No JRE (blocker 2). |
| EULA consent | IMPLEMENTED BUT UNVERIFIED | Consent UI and persisted consent were added; verify migration and user flow on device. |
| Cross-play (Paper + Geyser) | **NOT IMPLEMENTED** | Only a Geyser config generator exists. Now correctly reported as unsupported. |
| Playit tunnel | PARTIALLY IMPLEMENTED, UNVERIFIED | Release metadata lookup and an executable check exist, but Android/Bionic execution and external join are unverified. Endpoint parsing is not proof of end-to-end reachability. |
| Connection monitoring | PARTIALLY IMPLEMENTED | Protocol-aware reachability work exists, but must be validated with real Java TCP and Bedrock UDP endpoints. |
| Tunnel state model | PARTIALLY IMPLEMENTED | One status enum; agent/auth/tunnel/local/remote health are not separated. |
| Backups: create | IMPLEMENTED BUT UNVERIFIED | The code requests save pause/flush before snapshotting; engine acknowledgement and consistency on real worlds remain unverified. |
| Backups: restore | IMPROVED, UNVERIFIED | See changes above. |
| Players screen | IMPLEMENTED BUT UNVERIFIED | Sends console commands and updates the local DB regardless of whether the server accepted them. Whether the list reflects real joins was not traced. |
| Diagnostics | PARTIALLY IMPLEMENTED | Runtime/download diagnostics are present and error surfacing has been improved; verify every failure path on device. |
| Foreground service / wake lock / thermal | IMPLEMENTED BUT UNVERIFIED | `specialUse\|dataSync` types declared; Android 14+ behaviour untested. |
| Release signing | CONFIGURED BUT UNVERIFIED | Release signing reads owner-controlled environment variables or `local.properties`; without them the release is unsigned. A production signing key is not committed. |
| Credential storage | NOT IMPLEMENTED | `security-crypto` is a dependency but nothing stores Playit credentials securely. |

## What was and was not verified

- **Done:** read the source of the server, networking, world, service, build and test files; verified each audit finding against it; checked the Playit asset naming against playit.gg.
- **CI verified:** the pinned Android PHP archive checksum, static AArch64 ELF shape, PHP 8.4.27 execution under QEMU, required module list (including `webrtc`), and bundled `php.ini` loading.
- **Still not verified:** physical Android launch, real Axolotl-PM process readiness, Bedrock LAN client join, persistence, Java runtime, cross-play and external connectivity.
- All real-Minecraft tests (local Bedrock/Java join, external join, persistence, cross-play) are **NOT TESTED**.

## Remaining work, in priority order

1. Build the project and run the unit tests (including the new ones) to confirm the edits compile and pass.
2. Resolve blocker 1 (PHP on Android) and run the Bedrock acceptance test from the original brief on a physical device with a real Bedrock client.
3. Pin and verify downloads (blocker 5); fix the Playit URL and execution model (blockers 3, 4).
4. EULA consent screen plus persisted consent (needs a Room migration).
5. Protocol-aware tunnel/connection health with separate agent/tunnel/local/remote states; secure credential storage.
6. Safe backups of a live server (`save-off`/`save-all`); surface download errors in Diagnostics.
7. Real release signing via environment variables or local properties.
8. Java runtime investigation, then Geyser cross-play.


## Update (later in the same audit): status after further changes

Still **NOT READY**. Blockers 1 to 3 (PHP, Java, executing binaries on Android 10+) are unchanged. All items below are **implemented but uncompiled and untested on a device** unless a unit test is named.

| Area | Now |
|---|---|
| Download integrity (blocker 5) | Axolotl-PM PHAR tag, asset name, canonical URL and SHA-256 are hard-pinned; launch verifies the on-disk PHAR. PHP module presence is checked at startup against the Axolotl-PM required set. |
| EULA | Consent dialog, stored consent (Room migration 1 to 2), Java never starts without it. |
| Tunnel state model | Separate states and health facts, protocol-aware probes (UDP/TCP), supervised restart with backoff, redacted logs. Playit output formats are assumed (`TunnelSupportTest`). |
| Players | Online list from parsed join/leave lines (`PlayerTrackerTest`, formats assumed); commands need an ONLINE server and a successful send. |
| Server detail | Shows LAN address and an internet-check line; no hard-coded player count. |
| Diagnostics | Download failures are shown; concurrent downloads blocked. |
| Backups | Live backups pause world saving (`save-off`/`save-all`). |
| Release signing | Reads env vars or `local.properties`; the release build is unsigned (not debug-signed) if absent. |
| Stale state | On app start, servers recorded ONLINE/STARTING are marked CRASHED. |
| Service/backup rules | `specialUse` type, `START_NOT_STICKY`; tunnel dir excluded from backups. |
| Credential storage | Still **NOT IMPLEMENTED**: the Playit agent keeps its secret in an app-private, owner-only directory; no Keystore encryption. |

Not done: dependency/license inventory, security and performance reviews beyond this table, any build or device run. Test status is in `docs/TESTING.md`.
