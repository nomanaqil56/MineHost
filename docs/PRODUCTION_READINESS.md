# MineHost — Production Readiness Audit

**Audit date:** 2026-11-10  
**Source reviewed:** `main` repository tree as inspected on 2026-11-10.  
**Verdict: NOT PRODUCTION READY.**

This document deliberately distinguishes source code, pure-logic tests, Android builds, physical-device tests and real multiplayer tests. Code existing in the repository is not evidence that it works on an Android device.

## Executive summary

The project has a substantial Android application foundation: Kotlin/Compose UI, Room/Hilt, server-engine abstractions, runtime/download management, process supervision, local-network helpers, backup/restore safeguards, thermal/wake-lock services and a Playit tunnel adapter.

However, the central product promise has not yet been demonstrated end to end. The latest repository test notes report 40 pure-Kotlin tests passing outside Gradle, but also state that Android-dependent code has not been compiled in that environment and no physical device or real Minecraft client has been tested.

The current source still contains a Playit-specific provider. MineHost-owned public relay infrastructure, its control plane, direct NAT traversal and international gameplay tests are not implemented. Java runtime execution and Geyser cross-play are not verified.

Do not describe MineHost as production-grade or claim that it already provides seamless worldwide hosting.

## Evidence and verification levels

| Verification level | Current evidence | Status |
|---|---|---|
| Source inspection | App, engine, runtime, network, storage and service code present | Inspected |
| Pure Kotlin tests | Repository notes report 40/40 run with kotlinc/JUnit outside Gradle | Reported; not independently reproduced in this audit |
| Gradle Android build | Latest notes say Android-dependent code was not compiled in that environment | NOT VERIFIED |
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

`JavaEngine` expects `libjava.so` in the native library directory and invokes it as the Java executable. No JRE/JVM runtime is present in the repository tree. A JVM shared library is not necessarily the `bin/java` launcher and requires its complete runtime layout and compatible libc.

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

## Important source-level risks to verify/fix

These are code-review findings, not device-confirmed failures:

- `ServerController.stopServer()` currently returns success when `activeProcessHandle` is null. If a stop is requested while the server is still STARTING and the process handle has not yet been assigned, the request may do nothing while reporting success. The lifecycle needs an explicit cancellable start job/state transition.
- `RuntimeManager.getPlayitExecutable()` can return a binary under app-private tunnel storage and calls `setExecutable(true)`; this path needs a real Android execution test.
- `RuntimeManager.getPhpExecutable()` and `JavaEngine` use shared-object-style names as process paths. Validate actual ELF launchability rather than file presence.
- Live backup uses a fixed settle delay after save commands; it does not have a server-side acknowledgement that the flush completed. Large worlds need a stronger completion strategy or a clearly documented best-effort status.
- Player tracking and Playit output parsing depend on expected log formats. Capture and test against real engine/agent output.
- Credential handling for tunnel secrets needs an explicit Android Keystore-backed design and redaction review.
- The manifest declares foreground-service types and the service uses `specialUse` on newer Android. Verify required declarations, Play policy eligibility and target-API behaviour before release.
- Release signing now avoids the debug key and reads owner-supplied configuration. A production key still needs to be created, stored securely and used in a release build.
- The build guide references a GitHub Actions workflow, but no `.github/workflows/` file was present in the inspected repository tree. Add CI or remove that instruction.
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
11. Production signing, monitoring, support and rollback/recovery procedures are ready.

See [MINEHOST_MASTER_ROADMAP.md](../MINEHOST_MASTER_ROADMAP.md) for the phased plan and acceptance criteria. See [DEVICE_TESTING.md](DEVICE_TESTING.md) for the physical-device test procedure.
