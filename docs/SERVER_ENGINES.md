# MineHost — Server Engine Evaluation & Compatibility

> **Status: candidates under evaluation, not a list of production-supported engines.** No engine is considered supported until its exact version and runtime pass the physical-device and real-client acceptance gates in [MINEHOST_MASTER_ROADMAP.md](MINEHOST_MASTER_ROADMAP.md).

## 1. Engine architecture

Each server engine must be isolated behind the `ServerEngine` interface and provide:
- version and capability metadata;
- verified installation and runtime checks;
- configuration generation with validated inputs;
- real process launch and log capture;
- protocol-aware readiness detection;
- graceful stop and bounded force-stop;
- command delivery with truthful success/failure;
- explicit supported Minecraft client/protocol range.

An engine being listed in this document does not mean its runtime is available in the APK or can execute on Android.

## 2. Bedrock candidates

### PocketMine-MP
- Language/runtime: PHP CLI plus PocketMine-MP PHAR and native extensions.
- Why evaluate it: relatively lightweight, extensible, established plugin ecosystem.
- Important limitations: it is not a drop-in vanilla Bedrock Dedicated Server replacement; gameplay parity, entity AI, redstone, dimensions, and plugin requirements must be described honestly.
- Android blocker: MineHost currently has no validated PHP runtime. A desktop Linux PHP build is not assumed to run on Android/Bionic. The project must prove a compatible ARM64 runtime, extension set, process-launch strategy, and licence/redistribution path.
- Protocol blocker: validate the selected PocketMine release against the current Bedrock client transport. Do not assume legacy RakNet support is sufficient for all current clients.

### PowerNukkitX
- Language/runtime: Java server implementation requiring a compatible JVM.
- Why evaluate it: potentially different gameplay and plugin capabilities from PocketMine.
- Android blocker: no validated Android-compatible Java VM is currently present. Memory, CPU, battery and thermal costs must be measured on real phones.
- Do not publish estimated RAM or player-capacity claims until benchmarked on representative hardware.

### Official Bedrock Dedicated Server
- Official distributions and supported architectures must be checked against Mojang's current release documentation.
- Do not assume an official ARM64 Android binary exists.
- x86 emulation is not an acceptable primary path for a performance-first Android host unless rigorous testing proves otherwise.

## 3. Java candidate

### Paper
- Language/runtime: desktop Java server JAR requiring a full compatible JVM, not Android ART.
- Why evaluate it: mature server ecosystem and performance-oriented configuration.
- Android blocker: MineHost currently has no validated Java VM distribution that runs Paper on supported Android devices. A JAR file alone is not an executable runtime.
- Requirements: test Java version compatibility, native libraries, libc/Bionic compatibility, memory mapping, JIT behaviour, file access, signal handling, and sustained thermal performance.
- EULA: never accept the Minecraft EULA on behalf of the user. Obtain explicit consent, store it accurately, and provide the official EULA link.

Potential future alternatives include Purpur, Fabric, or Vanilla, but they should not be added until Paper is proven and the additional compatibility burden is justified.

## 4. Cross-play: Geyser and Floodgate

Geyser translates compatible Bedrock client traffic to a Java server. Floodgate can support Bedrock identity/authentication workflows with Geyser. They are not independent game-server engines.

Target composition:
- Paper runs locally on the Android host.
- Geyser runs as a plugin or supported standalone bridge connected to Paper.
- Floodgate is added only for the chosen authentication mode and configured securely.
- The Bedrock-facing transport must match the current supported Geyser/client versions.

This entire composition is blocked until the Java runtime and Paper server work on-device. Do not display cross-play as available before real Bedrock and Java clients have joined the same phone-hosted world.

## 5. Runtime and packaging constraints

- Android ART executes Android DEX; it does not directly run desktop Java server JARs.
- A PHP/Java runtime must be built for and validated on the target Android ABI and OS environment.
- Do not assume downloaded executables in app-private writable storage can be executed on modern Android.
- Native libraries packaged in `jniLibs` have Android packaging and dynamic-linker constraints; a shared object is not automatically a standalone executable.
- PRoot/rootfs approaches introduce compatibility, storage, performance, security, and maintenance costs. Use only if validated and necessary.
- Every binary/runtime must have a pinned version, source, licence, ABI, dependencies, integrity verification method, and uninstall/update procedure.

## 6. Engine support policy

A server engine may be labelled:
- **Candidate:** under research only.
- **Implemented:** integration code exists, but no complete runtime/client proof.
- **Device-verified:** launches and answers protocol probes on listed physical devices.
- **Client-verified:** real Minecraft clients join and play on a local network.
- **Externally verified:** real clients join over the public internet through MineHost connectivity.
- **Supported:** the above tests pass for a documented version/device matrix and have a maintained update path.

Never label a candidate or code-only integration as supported.
