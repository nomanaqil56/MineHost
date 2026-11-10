# MineHost — Technical Research & Feasibility

> This document records engineering constraints and candidate approaches. It is not proof that a candidate works on Android. Current delivery priorities and evidence requirements are in [MINEHOST_MASTER_ROADMAP.md](MINEHOST_MASTER_ROADMAP.md).

## 1. Product and system constraints

MineHost runs the actual Minecraft server process on the user's Android phone. The phone stores the world and supplies CPU, memory, storage, battery/charging and upstream bandwidth. MineHost's backend coordinates remote access and provides self-operated relay fallback; it does not host the Minecraft world.

This creates three independent performance budgets:
1. **Game server:** CPU tick time, memory, storage I/O, plugin/mod load, thermal throttling.
2. **Host network:** upload bandwidth, carrier/router NAT, packet loss, jitter, route stability.
3. **Remote path:** direct route or player-to-relay-to-host route, relay capacity and peering.

A low ping does not prove healthy TPS. A 20 TPS server does not prove a low-latency network. Measure them separately.

## 2. Bedrock engine candidates

### PocketMine-MP
- PHP-based Bedrock server implementation with a PHAR distribution.
- Potential advantage: comparatively lightweight server architecture and plugin extensibility.
- Trade-off: not full vanilla Bedrock parity; feature differences must be clearly disclosed.
- Critical feasibility work: source a maintained Android ARM64 PHP CLI/runtime with required extensions; validate process launch and dynamic libraries on physical devices; review licences; test current Bedrock client transport compatibility.
- The repository currently downloads the PHAR but does not contain a validated PHP runtime installer/binary.

### PowerNukkitX
- Java-based Bedrock server implementation.
- Potential advantage: Java plugin ecosystem and a different feature/parity profile.
- Critical feasibility work: requires the same unresolved Android JVM problem as Java Edition; measure memory, CPU, battery and thermal load.
- Do not claim resource use or vanilla parity without version-specific tests.

### Official Bedrock Dedicated Server
- Check Mojang's current release artifacts and supported architectures before considering it.
- Do not assume an official Android/ARM64 build exists.
- x86 translation/emulation is not the preferred path for a performance-first phone host.

**Decision gate:** select one maintained engine that can actually run on Android ARM64 and supports the current target Bedrock client transport. A PHAR/JAR download alone does not satisfy this gate.

## 3. Java engine candidate

### Paper
- A performance-oriented Java server JAR with a mature plugin ecosystem.
- Android ART cannot directly execute desktop Java JARs. MineHost needs a real, compatible JVM and native libraries.
- The current adapter expects `libjava.so`; no validated Android JVM is present in the repository.
- Feasibility checks: Bionic/dynamic-linker compatibility, JVM native libraries, JIT behaviour, filesystem permissions, signal/process handling, memory mapping, supported Java version, sustained thermal behaviour.
- Test realistic memory allocation on physical phones before setting minimum device specifications.

## 4. Cross-play

Geyser translates compatible Bedrock client traffic for a Java server. Floodgate supports a Bedrock identity/authentication flow when configured with Geyser. This requires a working Java server and a compatible current Geyser version.

Do not advertise cross-play until Paper + Geyser (+ Floodgate where needed) execute on the same phone and real Java and Bedrock clients can join the same world.

## 5. Android process execution and packaging

- Android ART is not a desktop OpenJDK runtime.
- A server runtime must target the device ABI and compatible Android userspace/dynamic linker.
- A Linux binary compiled for glibc is not automatically executable on Android/Bionic.
- Downloading a file into app-private writable storage and setting its executable bit is not proof that Android will allow it to execute.
- Packaging native code in `jniLibs` has ABI, loader, library naming, linker and Play policy implications; a shared library is not automatically a standalone CLI executable.
- PRoot or a Linux rootfs may improve compatibility for some tools but adds storage, performance, security and maintenance costs. It must be justified by measurements.
- Pin every runtime and engine version; record source, ABI, licence, dependency set and trusted digest/signature.

## 5A. Android PHP runtime candidate (research update 2026-11-10)

The official `pmmp/PHP-Binaries` repository documents an `android-aarch64` cross-compile target using an `aarch64-linux-musl` toolchain. Its PM5 PHP 8.4 release exposes an asset named `PHP-8.4-Android-arm64-PM5.tar.gz`; the GitHub Releases API currently reports SHA-256:

`d8867966340121f821591b9bb29c80a58ad77abd6cc8e0e44d1cbbb3aaedd70c`

The upstream Android build test checks that the PHP executable has no dynamic section. This is promising because a static ARM64 executable can avoid relying on a desktop glibc loader. The MineHost repository now has `tools/prepare_android_php_runtime.sh`, which verifies the archive digest, checks the ELF architecture/static linkage, and copies the executable as `libphp.so` under `jniLibs/arm64-v8a` so Android packages/extracts it with native libraries.

**This is only a packaging spike, not a validated runtime.** The upstream PHP-Binaries repository is archived, and the full PHP/dependency licence inventory and update strategy must be reviewed before release. CI packaging, APK inspection, execution on current physical Android versions, required PHP extensions, PocketMine startup, and real Bedrock client join are still required. Do not mark Bedrock as supported based on the script alone.

Sources:
- [PMMP PHP-Binaries build documentation](https://github.com/pmmp/PHP-Binaries)
- [PMMP PHP-Binaries PM5 PHP 8.4 release](https://github.com/pmmp/PHP-Binaries/releases/tag/pm5-php-8.4-latest)
- [PMMP PHP-Binaries Android build workflow](https://github.com/pmmp/PHP-Binaries/blob/stable/.github/workflows/main-pm-matrix.yml)

## 6. Networking feasibility

### NAT and CGNAT
Mobile carriers and residential ISPs often prevent inbound connections. A local server socket is therefore not necessarily reachable from the public internet.

### Protocol separation
- Java gameplay uses TCP.
- Bedrock transport must match the selected server/client versions; do not hard-code an obsolete RakNet-only assumption.
- Java TCP needs a TCP-compatible direct/tunnel strategy.
- Bedrock requires a relay that preserves the current transport's TCP/UDP behaviour and packet semantics.
- UDP NAT traversal is not a universal solution for Java TCP or all NAT types.

### MineHost-owned architecture
- Direct LAN/public IPv6 where reachable.
- Direct NAT traversal where the protocol and both networks support it.
- MineHost-owned coordinator for registration, rendezvous and short-lived connection tickets.
- MineHost-operated TCP/UDP relay fallback.
- Regional route selection based on measured RTT, jitter, loss, capacity and peering.
- Coordinator outage must not stop a locally running server.

A relay adds a network leg; it cannot eliminate physical distance or fix a slow host upload connection. Start with one self-managed relay and expand based on measured demand and bandwidth economics.

## 7. Android background and device constraints

- A foreground service is necessary for a user-visible long-running host workload, but it does not guarantee process survival.
- Select an Android foreground-service type that accurately matches the work and current Android/Play policies; do not use a time-limited service type as a workaround for permanent hosting.
- OEM battery managers and low-memory killers can still terminate a process.
- Wake locks and high-performance Wi-Fi locks increase battery use.
- Thermal throttling can reduce CPU performance during sustained sessions.
- MineHost should recommend charging and stable Wi-Fi for dedicated hosting, while still supporting cellular when the user chooses it.
- Implement bounded restart/recovery and clearly report when the OS terminates a server.

## 8. Security and operational feasibility

- Self-hosting a relay means MineHost is responsible for patching, monitoring, capacity, abuse handling, DDoS response and incident response.
- Use authenticated encrypted host tunnels and short-lived scoped tickets.
- Rate-limit control-plane requests and relay sessions.
- Do not expose arbitrary port forwarding or open proxy functionality.
- Minimize telemetry and document retention.
- Budget relay bandwidth per active server-hour before promising unlimited free relaying.
- Use established open-source components where suitable, after licence and threat-model review.

## 9. Open research questions

- Which maintained Bedrock server has the best current protocol compatibility and can run efficiently on Android ARM64?
- Which PHP runtime and extension set can be distributed and executed lawfully and reliably on supported Android versions?
- Is there a maintainable Android JVM distribution suitable for Paper and Geyser?
- What transport adapter is needed for each supported Bedrock protocol generation?
- Which NAT traversal method works with the target mobile carriers, and what percentage still requires relay?
- What relay bandwidth and operating cost does a realistic Minecraft workload produce?
- Which foreground-service declaration and Play distribution approach is compliant for the exact host workload?

No candidate should be selected based on marketing claims or desktop benchmarks alone. Validate it on target devices with real clients.
