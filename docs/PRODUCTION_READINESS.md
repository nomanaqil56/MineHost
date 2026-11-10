# MineHost — Production Readiness Audit

**Audit date:** 2026-11-10  
**Source reviewed:** `work/production-readiness` at the latest branch head on 2026-11-10. The latest successful full CI baseline is `ef96eaa04ed96d6f55e5d10a53ba331a98733391`; subsequent diagnostics and shutdown-reporting fixes are awaiting their own CI run.  
**Verdict: NOT PRODUCTION READY.**

This document deliberately distinguishes source code, pure-logic tests, Android builds, physical-device tests and real multiplayer tests. Code existing in the repository is not evidence that it works on an Android device.

## Executive summary

The project has a substantial Android application foundation: Kotlin/Compose UI, Room/Hilt, server-engine abstractions, runtime/download management, process supervision, local-network helpers, backup/restore safeguards, thermal/wake-lock services and a Playit tunnel adapter.

However, the central product promise has not yet been demonstrated end to end. The production-readiness branch now passes GitHub Actions unit tests, Android lint and debug APK assembly (run 42754538024). This verifies the Android build, not native runtime execution, physical-device behaviour or Minecraft gameplay. No physical device or real Minecraft client has yet been tested.

The current source still contains a Playit-specific provider. MineHost-owned public relay infrastructure, its control plane, direct NAT traversal and international gameplay tests are not implemented. Java runtime execution and Geyser cross-play are not verified.

Do not describe MineHost as production-grade or claim that it already provides seamless worldwide hosting.

## Evidence and verification levels

| Verification level | Current evidence | Status |
|---|---|---|
| Source inspection | App, engine, runtime, network, storage and service code present | Inspected |
| Unit tests | Gradle `testDebugUnitTest` passed in CI run 42753556848; earlier notes also report 40/40 standalone pure-Kotlin tests | CI VERIFIED (unit tests) |
| Gradle Android build | API 36 debug APK assembled in CI run 42753556848 | ANDROID BUILD VERIFIED (debug only) |
| Android lint | Passed in CI run 42753556848 | VERIFIED |
| Android instrumented tests | No device run recorded | NOT VERIFIED |
| Physical Android device | No device acceptance run recorded | NOT VERIFIED |
| Real Minecraft client | No local or remote client join recorded | NOT VERIFIED |
| Public tunnel | No live provider claim and external join recorded | NOT VERIFIED |
| International gameplay | No cross-country play test recorded | NOT VERIFIED |
| Play Store compliance | No final policy/legal audit recorded | NOT VERIFIED |

## Critical blockers

### 1. Bedrock PHP runtime and executable launch path

The Bedrock engine expects a PHP executable from `RuntimeManager.getPhpExecutable()`. The repository tree has no `app/src/main/jniLibs/` directory and no committed Android PHP runtime artifact. The code searches for `libphp.so` in `nativeLibraryDir` or a PHP executable in private app storage.

A file named `libphp.so` is not automatically a valid command-line executable. A shared object may need a dedicated launcher, compatible ELF interpreter and Android/Bionic dependencies. A binary in app-writable storage may also be blocked by Android execution policy. This must be proven with a real ARM64 build and physical-device process launch; do not treat a successful download or file-exists check as runtime readiness.

**Required:** select a maintained Bedrock engine and compatible runtime/launcher; verify provenance and checksums; run it on supported Android versions; prove start, protocol readiness, command input, graceful stop and persistence.

### 2. Java ARM64 runtime

`JavaEngine` now requires a complete runtime under `files/minehost/runtimes/java`, including an executable `bin/java` launcher and library layout. No validated Android ARM64 JRE is currently present in the repository tree. A launcher path alone is not proof of Bionic compatibility or headless server execution.

**Required:** validate a complete ARM64 JVM strategy on Android (including launcher, libraries, modules, libc and process execution). Until then, Java hosting is BLOCKED.

### 3. Android execution policy and process model

Downloaded executable files under app-private writable directories cannot be assumed executable on modern Android. `setExecutable(true)` alone is not proof that the kernel/SELinux policy will allow execution. The same concern applies to the downloaded Playit agent.

**Required:** document and test a supported packaging/launcher strategy on each supported Android API level. Never ship an execution path based only on a desktop Linux binary or file permission bit.

### 4. Connectivity does not meet the product independence goal

The source contains `PlayitTunnelProvider`, and `TunnelManager` is wired to a single provider. This is a useful integration/test adapter, but it is a third-party managed tunnel and does not meet the final MineHost independence requirement.

Not implemented in the repository:
- MineHost-owned control plane and registration API.
- MineHost-operated TCP/UDP relay server.
- Android host-side client for MineHost relay protocol.
- Relay authentication, authorization, quotas and abuse controls.
- Direct public path selection and NAT traversal.
- Relay region selection based on measured end-to-end quality.
- International real-client acceptance tests.

**Required:** preserve a provider abstraction, but build and operate MineHost's own control and data planes. Playit must not be a mandatory production dependency. Local LAN hosting must work with the backend unavailable.

### 5. Bedrock protocol and engine compatibility

The current source includes RakNet-oriented readiness probing and a PocketMine adapter with a small, old-looking advertised version list. Bedrock transport and protocol support are version-dependent and evolve. A generic UDP port check or an old RakNet probe cannot establish compatibility with every current Bedrock client/server.

**Required:** choose the exact engine and version, verify its supported client protocol and transport, implement the matching readiness/health probe, and test with real current clients. Keep the supported-version matrix explicit.

### 6. Cross-play is not operational

A Geyser configuration generator exists, but the Java runtime and Geyser execution path are not proven. The application must not advertise cross-play as available until Paper, Java runtime, Geyser, Floodgate and the Bedrock-facing transport all pass real-client tests.

### 7. No end-to-end device evidence

There is no recorded evidence of a real server starting through MineHost, a second device joining on LAN, a world surviving restart, or a remote player joining through a public relay.

**Required:** follow `docs/DEVICE_TESTING.md` and record device model, Android version, ABI, runtime/engine versions, client version, network type, logs and outcome.

### 8. Google Play target API requirement

The baseline project used targetSdk 34 / compileSdk 34. The production-readiness branch has been migrated to target/compile API 36 with AGP 8.13.2, Gradle 8.13, Kotlin 2.3.21, Compose BOM 2026.04.01 and JDK 17; the debug build passes CI. Android 16 physical-device compatibility and the Play Console specialUse foreground-service declaration remain release blockers.

Source: https://support.google.com/googleplay/android-developer/answer/11926878

The app also uses a specialUse foreground service for user-initiated long-running server hosting. Its manifest subtype must accurately explain the hosting use case, and the foreground-service declaration and supporting demonstration must be reviewed in Play Console. Declaring specialUse is not automatic approval.

Source: https://developer.android.com/develop/background-work/services/fgs/service-types

## Important source-level risks to verify/fix

These are code-review findings, not device-confirmed failures:

- `ServerController` now serializes start/stop with a lifecycle mutex and reconciles a missing process handle instead of silently reporting success. The change compiles in CI, but start/stop races and service recovery still need physical-device testing.
- `RuntimeManager.getPlayitExecutable()` now requires a regular executable file and no longer attempts to bypass Android execution restrictions with `setExecutable(true)`. A real Android process-launch test is still required.
- `RuntimeManager.getPhpExecutable()` still accepts a candidate packaged `libphp.so` only when it is marked executable; validate its ELF type, interpreter and actual CLI launch before calling the runtime ready.
- `RuntimeManager.isPaperJavaInstalled()` now requires both the Paper JAR and a `bin/java` launcher. Diagnostics distinguishes downloaded server archives from a runnable engine.
- Live backup uses a fixed settle delay after save commands; it does not have a server-side acknowledgement that the flush completed. Large worlds need a stronger completion strategy or a clearly documented best-effort status.
- Player tracking and Playit output parsing depend on expected log formats. Capture and test against real engine/agent output.
- Credential handling for tunnel secrets needs an explicit Android Keystore-backed design and redaction review.
- The manifest declares foreground-service types and the service uses `specialUse` on newer Android. Verify required declarations, Play policy eligibility and API 36 behaviour before release.
- Release signing now avoids the debug key and reads owner-supplied configuration. A production key still needs to be created, stored securely and used in a release build.
- Android CI now exists and passes unit tests, lint and debug APK assembly on the production-readiness branch. Keep it green as native runtimes and relay code are added.
- The inspected root tree did not contain a `LICENSE` file even though README previously claimed Apache-2.0. Confirm the intended project licence and add the corresponding licence text before distribution.

## Positive source changes already present

The latest code/docs describe several useful safeguards:
- Real protocol readiness probes instead of treating any live process or open port as ONLINE.
- Explicit EULA consent and a Room migration.
- Server configuration failures abort startup.
- Backup restore verifies checksum, extracts to a temporary directory and attempts rollback-safe swapping.
- Console commands reject control characters.
- Tunnel status distinguishes claim/authentication, endpoint assignment and reachability.
- Diagnostics surface download failures and prevent concurrent installs.
- Release builds do not silently fall back to debug signing.
- Stale ONLINE/STARTING state is reconciled after app restart.
- Pure-Kotlin tests cover readiness framing, command sanitization, archive validation, restore swapping, tunnel helpers, player parsing and LAN address selection.

These are meaningful improvements, but they do not resolve the runtime, Android build, device or network blockers.

## Release gate

MineHost must not be called production-ready until all of the following have evidence:

1. Reproducible Gradle release build, unit tests, lint and CI.
2. A real Bedrock engine starts and accepts a compatible real client on a physical ARM64 Android device.
3. World persistence, backup and restore pass on a real world.
4. Screen-off and sustained-load tests pass on documented supported devices.
5. Java hosting passes with a real Android-compatible JVM and Java client, if Java is advertised.
6. Geyser cross-play passes with real Java and Bedrock clients, if cross-play is advertised.
7. Wi-Fi and cellular tests pass, including a carrier/home CGNAT case.
8. MineHost-operated relay path passes real external and international gameplay tests without a managed third-party tunnel.
9. Direct/relay path latency, jitter, packet loss, reconnect behaviour and relay bandwidth cost are measured.
10. Security, dependency licence, privacy, Minecraft branding and Google Play policy reviews are complete.
12. targetSdkVersion and compileSdkVersion are at least API 36 and Android 16 behaviour is tested.
11. Production signing, monitoring, support and rollback/recovery procedures are ready.

See [MINEHOST_MASTER_ROADMAP.md](../MINEHOST_MASTER_ROADMAP.md) for the phased plan and acceptance criteria. See [DEVICE_TESTING.md](DEVICE_TESTING.md) for the physical-device test procedure.


## CI verification update — 2026-11-10

The production-readiness branch now has a successful Android CI run:
- [GitHub Actions run 42754538024](https://github.com/nomanaqil56/MineHost/actions/runs/42753556848)
- Unit tests: PASS.
- Android lint: PASS.
- Debug APK assembly: PASS.
- Debug APK artifact: `minehost-debug-apk` (approximately 18.3 MiB (debug APK artifact ZIP)).

This closes the initial reproducible Android debug-build gate only. It does **not** close the PHP/JVM runtime, Android executable policy, physical-device, current NetherNet, Geyser, MineHost relay, international gameplay, licensing or Play Store release gates. The branch is not production-ready and has not been merged to `main`.
