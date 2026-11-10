# MineHost — Production Readiness Audit

_Audit date: 2026-11-10. Source review of the current development branch and recent main-branch changes. This is a code/documentation audit, not a physical-device certification._

## Verdict

**NOT PRODUCTION-READY. Do not publish or advertise the planned hosting capabilities as working yet.**

The repository contains a meaningful Android application foundation and several real implementation components. However, the core product promise—running a real Minecraft server on an Android phone and allowing real players to join—has not been proven on a physical device. The two principal runtime paths are currently blocked.

## Critical blockers

| Priority | Blocker | Source evidence / consequence | Required proof |
|---|---|---|---|
| P0 | Android PHP runtime absent | RuntimeManager can locate a PHP executable, but no PHP runtime installer or bundled ARM64 runtime is present. BedrockEngine returns a clear failure when it cannot find PHP. | Reproducible Android-compatible ARM64 PHP runtime; launch PocketMine on a physical phone and join with a real Bedrock client. |
| P0 | Android JVM absent | JavaEngine expects libjava.so in nativeLibraryDir; no verified JVM distribution is bundled. Paper cannot launch. | Reproducible Android-compatible ARM64 JVM and a real Paper launch/client join on device. |
| P0 | No end-to-end server proof | The current test evidence is helper/unit logic only. No physical-device server/client acceptance evidence is recorded. | Real device, real server process, real client, persistence and sustained-session test. |
| P0 | MineHost-owned public connectivity absent | The existing provider is Playit-specific. The MineHost control plane, direct path negotiation and MineHost-operated relays are not implemented. | Public join behind CGNAT using a MineHost-operated relay, with a normal Minecraft client. |
| P0 | Playit executable path incompatible/unverified | RuntimeManager downloads the agent into app-private files and attempts setExecutable/ProcessBuilder. Android 10+ executable-memory/W^X restrictions make this path unsuitable without a validated loader or packaged native executable. | Prove a permitted Android execution model on API 29+ devices, or remove the provider from production. |
| P1 | Java/Bedrock cross-play unavailable | Geyser configuration groundwork exists, but Java runtime execution and bridge execution are not available. | Real Paper + Geyser + Bedrock-client interoperability tests for a declared version matrix. |
| P1 | Bedrock transport/version compatibility uncertain | Existing readiness and networking notes contain legacy RakNet assumptions. Current Bedrock server/client transport support must be revalidated before selecting the engine. | Versioned protocol matrix and real client tests for each supported transport. |
| P1 | Google Play target SDK migration pending verification | Main currently uses compileSdk/targetSdk 34. This PR migrates the development branch to API 36 with AGP 8.13, Gradle 8.13, Kotlin 2.3 and matching Compose/KSP plugins. The migration has not yet passed CI. | Get the API 36 toolchain build green, run tests, then complete Android 16 behavior/policy migration and Play Console review. |
| P1 | Foreground service policy review pending | The server service uses specialUse on Android 14+, with a subtype description. This use case and declaration require Play Console review and a user-visible notification. | Test on Android 14+ and submit accurate special-use declaration and video evidence in Play Console. |
| P1 | Release distribution not verified | Release signing is intentionally unsigned unless credentials are configured. No signed release artifact or Play App Bundle validation is recorded. | Secure production signing, AAB build, Play pre-launch report, and store policy review. |

## What is implemented in source (not equivalent to production verification)

- Kotlin / Jetpack Compose application foundation, Room, Hilt, navigation, and feature screens.
- Server engine abstractions and server lifecycle controller.
- PocketMine configuration and process-launch path, but no usable PHP runtime.
- Paper download/configuration and EULA consent groundwork, but no usable JVM.
- Server readiness probes for legacy Bedrock RakNet and Java Server List Ping; real engine compatibility remains unverified.
- World storage, ZIP backup, checksum validation, safe extraction, and rollback-oriented restore logic.
- Live backup save-pause logic and restore refusal while server is active.
- Console command sanitization and player join/leave parsing; parser formats require real server verification.
- Playit tunnel provider, output parser, claim flow and restart supervision; third-party and not end-to-end verified.
- Separate tunnel/agent/local/public health state groundwork and protocol-aware probes.
- Release signing configuration that avoids silently signing release builds with the debug key.
- Runtime architecture check corrected on the development branch to report only ARM64 as supported by the current runtime plan.

## Verification evidence and limits

The latest testing notes record 40 pure-Kotlin helper tests executed outside the Android Gradle build, with no failures. Those tests cover protocol framing helpers, parsers, checksum/backup logic, and other deterministic code. They do not prove Android framework integration, native runtime execution, real Minecraft server behavior, tunnel reachability, or gameplay.

No current audit evidence establishes:
- a clean Android Gradle build from the latest branch;
- a physical Android device starting PocketMine or Paper;
- a real Bedrock or Java client joining;
- a real external/international join;
- stable screen-off hosting under sustained load;
- production relay operation or measured end-to-end latency.

The new Android CI workflow is intended to provide reproducible build and unit-test evidence. Its result must be inspected before treating the Android build as verified.

## Important source-level risks to investigate

1. RuntimeManager.getPhpExecutable() only searches expected locations; it does not install a PHP runtime.
2. JavaEngine requires libjava.so but no JVM package or installer is present.
3. Runtime downloads must be reviewed against Android W^X restrictions and Google Play's dynamically loaded code policy. A successful download and chmod do not prove execution is permitted.
4. RuntimeManager accepts downloads when the upstream release provides no checksum, recording the file as not verified. Production policy should pin versions and require trusted integrity evidence for executable/runtime assets.
5. The Playit agent is stored in app-private files and launched through ProcessBuilder; this must not be treated as a proven Android-compatible execution path.
6. Playit output endpoint parsing is not an end-to-end Minecraft connectivity test.
7. A tunnel process or assigned public hostname does not prove remote protocol reachability.
8. Server readiness probes must match the selected server engine and exact Minecraft transport/version.
9. Backup consistency must be verified against real Bedrock LevelDB and Java worlds, not only temporary files.
10. The current target SDK is API 34; new Google Play submissions now require API 36.
11. Foreground-service special-use eligibility is reviewed by Google Play and is not guaranteed by adding a manifest declaration.
12. No measured network or server performance benchmark exists yet.

## Immediate order of work

1. Get Android CI green on the current branch and fix all compile/test failures.
2. Create a PHP-on-Android feasibility spike. Confirm whether a reproducible Bionic ARM64 build can run safely under Android restrictions and Play policy. If not, choose a different maintained Bedrock engine/runtime strategy before expanding features.
3. Create a JVM-on-Android feasibility spike. Do not expose Java hosting or cross-play until Paper launches on a physical phone.
4. Complete a local LAN acceptance test for the first viable engine.
5. Build the first MineHost-operated relay proof of concept, separately for Java TCP and the supported Bedrock transport.
6. Prove a remote client can join through CGNAT using MineHost infrastructure.
7. Measure latency, jitter, packet loss, throughput, TPS/MSPT, battery and temperature under a documented test load.
8. Verify the in-progress API 36 toolchain migration in CI, then complete Android 16 behavior/Google Play policy work.
9. Complete licensing/SBOM, privacy, security, release signing, AAB and Play Console review.
10. Expand to additional engines and relay regions only after the core hosting proof succeeds.

## Release decision

**Current decision: NO-GO.**

A release candidate requires the release gates in [MINEHOST_MASTER_ROADMAP.md](MINEHOST_MASTER_ROADMAP.md). A successful Gradle build alone is not enough.
