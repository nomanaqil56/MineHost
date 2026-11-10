# MineHost — Technical Research and Feasibility

> This document records engineering constraints and open decisions. It is not a claim that the described systems are already implemented.

## 1. Product model

MineHost is phone-first, local-first Minecraft hosting:
- the user's Android phone runs the actual Minecraft server;
- world files remain on the phone unless the user exports them;
- MineHost's cloud services coordinate connectivity but do not host or simulate the game world;
- local hosting and LAN multiplayer are intended to remain free;
- direct connectivity is preferred, and MineHost-operated relays are used only when necessary;
- both Wi-Fi and mobile data are supported.

MineHost can own its control plane and relay software, but cannot eliminate upstream internet transit, electricity, DNS, Android, or open-source dependencies. The independence target is no mandatory third-party managed game-hosting or tunnel provider.

## 2. Server-engine feasibility

The previous default choice of PocketMine-MP must be reconsidered: its upstream repository and official PHP-Binaries repository were archived in 2026. PowerNukkitX requires Java 21+, which introduces a large Android JVM and memory challenge. Dragonfly is Go-based and can be embedded as a library, making native Android ARM64 integration worth investigating. Chorus is a Rust candidate but maturity and protocol parity are unverified.

Android ART is not a desktop JVM and does not run arbitrary Paper/PocketMine server packages directly. A real compatible runtime or a native/in-process server integration is required. The repository currently has neither a verified PHP runtime nor a verified JVM.

See [Engine Evaluation](ENGINE_EVALUATION.md). Do not expand server-engine UI or claim compatibility until one engine passes physical-device and real-client tests.

## 3. Android native execution and distribution

Android 10+ imposes W^X restrictions: apps targeting API 29+ cannot execute untrusted binaries directly from writable app-home directories. Native code intended for execution must be packaged appropriately in the APK (for example, ABI-specific native libraries extracted to the app's native library directory) and must be compatible with Android's Bionic linker/ABI.

Google Play separately restricts downloading executable code such as DEX, JAR, and .so files from outside Google Play, subject to the policy's virtual-machine/interpreter exception. That exception must not be assumed to cover every CLI runtime or server package. MineHost needs a specific Play policy review before choosing how PHP/JVM/server code is distributed.

Preferred engineering approach:
- pin every runtime and engine version;
- build reproducibly from source where feasible;
- package native runtime components in the APK/AAB;
- verify checksums/signatures and maintain licence notices;
- keep world/config data separate from executable code;
- fail closed if a runtime cannot be verified or launched.

## 4. Networking feasibility

Cellular and residential networks commonly use NAT/CGNAT, preventing unsolicited inbound connections. MineHost's target is direct-first connectivity:
1. LAN direct.
2. Public direct IPv6 or explicitly reachable endpoint.
3. Protocol-compatible direct traversal where it is genuinely supported.
4. MineHost-operated relay fallback.

Java gameplay uses TCP and needs a TCP forwarding strategy. Bedrock transport is version-dependent; do not assume every supported client/server version uses only legacy RakNet. The chosen server engine and exact client versions define the actual TCP/UDP transport requirements.

The control plane handles registration, authentication, connection negotiation, and relay allocation. Gameplay packets travel over the data plane, not an HTTP API. Existing local servers should continue running during control-plane outages.

See [Networking Architecture](NETWORKING.md).

## 5. Performance model

Measure separate causes of lag:
- Network: RTT median/p95, jitter, packet loss, throughput, route changes.
- Minecraft: TPS and MSPT.
- Android: CPU, memory, storage, thermal status, throttling, battery.

A relay cannot remove physical distance or compensate for a weak host upload link. Select relay routes using measured end-to-end quality and capacity, not geography alone. Never advertise zero ping or zero lag.

## 6. Android lifecycle

A server needs a correctly declared user-visible foreground service, ongoing notification, process supervision, graceful shutdown, and bounded wake-lock usage. OEM battery managers, Doze, low memory, thermal throttling, and Android version changes still affect reliability. Do not promise uninterrupted operation on every phone.

The app currently targets API 36 on the development branch; verify the AGP/Kotlin/Gradle migration in CI and test Android 16 behavior before release.

## 7. Open decisions

- Which maintained Bedrock engine is viable on Android ARM64?
- Can Dragonfly be built as a Go shared library/AAR and safely run inside the app process?
- Can a Java 21+ JVM be packaged and maintained within phone RAM/thermal limits?
- Which Bedrock transport/version matrix will be supported?
- What relay protocol and self-operated node implementation provide the best latency/cost trade-off?
- What fair-use policy keeps free relayed connectivity sustainable?
- What runtime distribution approach passes current Google Play policy?

These questions are tracked in [Master Roadmap](MINEHOST_MASTER_ROADMAP.md) and [Engine Evaluation](ENGINE_EVALUATION.md).
