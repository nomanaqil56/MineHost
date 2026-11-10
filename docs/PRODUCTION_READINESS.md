# MineHost: Production Readiness

_Audit date: 2026-11-09. Based on reading the source of the `main` branch (commit `915ff66`) and the changes listed below._

## Verdict

**NOT READY: CRITICAL BLOCKERS REMAIN.**

MineHost has a well-organised app shell (UI, database, services, backups), but **no Minecraft server has ever been shown to run through it**, and several blockers mean it cannot run one today. Nothing here has been built or run on a device in this audit (see "What was and was not verified").

## Blockers (why a real server cannot start today)

| # | Blocker | Evidence | Needed to resolve |
|---|---|---|---|
| 1 | **No PHP runtime exists.** The Bedrock engine runs `libphp.so` from `nativeLibraryDir`, but the repo has no `app/src/main/jniLibs/` directory and no code that obtains a PHP binary. `getPhpExecutable()` returns `null`, so Bedrock start fails with a clear error. | `RuntimeManager.getPhpExecutable()`, `BedrockEngine.start()`, no `jniLibs/` | An Android-compatible ARM64 PHP build (as `lib*.so` in `jniLibs`, or another validated execution method), tested on a device. |
| 2 | **No Java runtime exists.** `JavaEngine` needs `libjava.so`; none is bundled. | `JavaEngine.start()`, no `jniLibs/` | An ARM64 JRE that runs on Android Bionic, or a PRoot userspace. Java is therefore **BLOCKED**. |
| 3 | **Android 10+ W^X.** Binaries downloaded into `filesDir` (the Playit agent, a downloaded PHP) cannot be executed on targetSdk 29+. Only files extracted from the APK's `jniLibs` can. | `RuntimeManager.downloadPlayitAgent()` writes to `filesDir` and calls `setExecutable` | Package binaries in `jniLibs` or use an approved loader. Not testable without a device. |
| 4 | **Playit agent download URL is almost certainly wrong.** The app requests `playit-cli-linux-aarch64` / `playit-cli-linux-x86_64`. The official Linux asset naming is `playit-linux-*` (e.g. `playit-linux-amd64`, per playit.gg/support/run-on-linux). The correct arm64 asset name was not confirmed. | `RuntimeManager.downloadPlayitAgent()` | Confirm the real asset name and whether it runs on Bionic. |
| 5 | **Downloads are not integrity-checked.** SHA-256 is _computed_ and stored but never compared with a trusted value; PocketMine and the Playit agent use `releases/latest`. | `RuntimeManager.downloadFileWithProgress()` | Pin versions and verify against published checksums or signatures. |
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
| Bedrock engine (PocketMine-MP) | **BLOCKED** | Real code, but no PHP runtime (blocker 1). Fake shell loops and `\|\| true` checks from older versions are already gone. |
| Java engine (Paper) | **BLOCKED** | No JRE (blocker 2). |
| EULA consent | **NOT IMPLEMENTED** | `eula.txt` is written as `eula=false` (the server will refuse to start rather than silently accepting). There is no consent screen or stored consent. |
| Cross-play (Paper + Geyser) | **NOT IMPLEMENTED** | Only a Geyser config generator exists. Now correctly reported as unsupported. |
| Playit tunnel | PARTIALLY IMPLEMENTED, **BROKEN download** | Real process launch and output parsing, but blockers 3 and 4. It reports CONNECTED when its output regex matches a hostname, not after an end-to-end check. |
| Connection monitoring | PARTIALLY IMPLEMENTED | Uses a TCP connect, which is **wrong for Bedrock (UDP)**. Not protocol-aware. |
| Tunnel state model | PARTIALLY IMPLEMENTED | One status enum; agent/auth/tunnel/local/remote health are not separated. |
| Backups: create | IMPLEMENTED BUT UNVERIFIED | Backing up while the server runs can capture an inconsistent world (no `save-off`/`save-all`). |
| Backups: restore | IMPROVED, UNVERIFIED | See changes above. |
| Players screen | IMPLEMENTED BUT UNVERIFIED | Sends console commands and updates the local DB regardless of whether the server accepted them. Whether the list reflects real joins was not traced. |
| Diagnostics | PARTIALLY IMPLEMENTED | Install flags and device info are real; download failures are swallowed (results ignored) so the user sees nothing on error. |
| Foreground service / wake lock / thermal | IMPLEMENTED BUT UNVERIFIED | `specialUse\|dataSync` types declared; Android 14+ behaviour untested. |
| Release signing | **BROKEN for production** | Release build uses the **debug** signing config (`app/build.gradle.kts`). |
| Credential storage | NOT IMPLEMENTED | `security-crypto` is a dependency but nothing stores Playit credentials securely. |

## What was and was not verified

- **Done:** read the source of the server, networking, world, service, build and test files; verified each audit finding against it; checked the Playit asset naming against playit.gg.
- **Not done:** no Gradle build, no unit-test run, no emulator, no physical device, no Minecraft client, no tunnel. This audit environment has no Android SDK and no network for Gradle. **Treat all edits above as unbuilt.** The previously documented "assembleDebug passes / 8 of 8 tests pass" claims were not reproduced here.
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
| Download integrity (blocker 5) | `RuntimeManager` resolves real release assets and verifies published SHA-256 when one exists. Asset names were not confirmed by a run. |
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
