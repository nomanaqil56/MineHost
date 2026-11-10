# MineHost — Server Engine Evaluation and Compatibility

**Status:** Engine adapters and configuration scaffolding exist, but no engine is currently marked physically verified. This document is an evaluation guide, not a list of production-supported engines.

An engine may be shown as **supported** in the app only after its exact engine/runtime/version combination passes on physical Android ARM64 hardware with a real Minecraft client.

## 1. Bedrock candidates

### PocketMine-MP
- Existing source adapter ID: `bedrock-pocketmine-v5`.
- Server distribution: PocketMine-MP PHAR.
- Runtime requirement: a compatible PHP CLI runtime and required extensions compiled for Android ARM64/Bionic (or a proven compatible userspace).
- Existing code expects a PHP process path. The current runtime path is not proven launchable.
- Important gameplay differences: PocketMine is not a drop-in implementation of every vanilla Bedrock mechanic. Validate entities, redstone, dimensions, blocks, plugins and client protocol support against the intended use case.
- **Status:** adapter source present; runtime and physical gameplay BLOCKED/UNVERIFIED.

Before selection, verify upstream maintenance, supported PHP version, supported Bedrock client protocols and actual transport implementation. Do not assume an old RakNet-only probe proves compatibility with all current Bedrock versions.

### PowerNukkitX
- Java-based Bedrock server candidate.
- Requires the same complete Android-compatible ARM64 JVM problem as Java Edition hosting.
- Potentially higher vanilla parity, but measure actual RAM, CPU, battery and thermal load.
- **Status:** evaluation candidate only; no operational MineHost adapter/runtime verified.

### Official Bedrock Dedicated Server
- Do not assume an official ARM64 Android build exists.
- Do not make x86 emulation the default mobile path; benchmark its CPU, battery and stability cost before considering it.
- **Status:** not selected.

## 2. Java candidates

### PaperMC
- Existing source adapter ID: `java-paper`.
- Requires a complete Java 17/21-compatible JVM distribution and launcher that actually run on supported Android ARM64 devices.
- Android ART does not execute desktop Java server JARs as a substitute for a desktop JVM.
- A file named `libjava.so` is not automatically a valid `bin/java` launcher or a complete JRE.
- Measure memory, GC pauses, tick time, chunk generation, plugin compatibility and thermal load on real phones.
- **Status:** adapter and downloader source present; complete JVM and real gameplay BLOCKED/UNVERIFIED.

Other server software (Purpur, Fabric, Vanilla, etc.) is future evaluation only. Do not expand the engine matrix until Paper's runtime and lifecycle are proven.

## 3. Java/Bedrock cross-play

Candidate stack:
- PaperMC as the Java server.
- GeyserMC as the Bedrock-to-Java protocol bridge.
- Floodgate where the intended authentication model requires it and the relevant terms/configuration are followed.

Requirements:
- Complete Android-compatible Java runtime.
- Paper starts and passes Java Server List Ping and real Java client join.
- Geyser starts and binds its Bedrock-facing endpoint.
- The selected Geyser version supports the current Bedrock client transport.
- Floodgate authentication and key management are configured securely.
- Real Bedrock and Java clients can join the same world and play expected features.

**Status:** Geyser configuration generator exists; operational cross-play is NOT IMPLEMENTED/VERIFIED. Keep the UI option disabled until acceptance tests pass.

## 4. Runtime execution is a first-class dependency

For each engine/runtime combination, record:
- CPU ABI and Android API levels tested.
- ELF type, interpreter and libc dependencies for native components.
- Exact runtime version, source URL and licence.
- Trusted checksum/signature and verification result.
- Launch method and executable location.
- Startup logs and protocol readiness response.
- Stop/restart behaviour.
- RAM, CPU, battery and thermal measurements.
- Client protocol versions tested.

A downloaded archive, PHAR or JAR alone is not a runnable server. A file existing on disk is not proof that Android can execute it.

## 5. Compatibility matrix

Keep a versioned matrix in the repository and in the app's engine metadata. Example columns:

| Engine | Engine version | Runtime | Android API/ABI | Client versions | Transport | LAN join | Remote join | Status |
|---|---|---|---|---|---|---|---|---|
| PocketMine | Exact tested release | Exact PHP build | Physical test data | Exact Bedrock versions | Engine-specific | Pending | Pending | BLOCKED |
| Paper | Exact tested build | Exact JVM build | Physical test data | Exact Java versions | TCP | Pending | Pending | BLOCKED |
| Paper + Geyser | Exact tested versions | Exact JVM build | Physical test data | Java + Bedrock matrix | TCP + Bedrock transport | Pending | Pending | NOT IMPLEMENTED |

Never fill this table with guessed or aspirational compatibility.

## 6. Selection principle

Choose the smallest maintained engine set that can meet the product promise:
1. One Bedrock engine with a proven native Android ARM64 runtime.
2. One Java engine with a proven Android ARM64 JVM.
3. Geyser/Floodgate only after both underlying server and Bedrock transport paths work.

Prioritize stability, low resource use, protocol support and maintainability over the number of engine logos in the UI.
