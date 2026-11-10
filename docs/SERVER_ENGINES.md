# MineHost — Server Engine Evaluation and Compatibility

**Status:** No engine is currently physically verified on Android by MineHost. This is an evaluation matrix, not a list of production-supported engines.

An engine may be shown as supported only after its exact engine/runtime/transport combination passes on a physical Android ARM64 device with a real Minecraft client.

## 1. Bedrock Edition candidates

### VanillaMine — leading PHP candidate to investigate
- Continuation of PocketMine-MP 5, currently advertises Bedrock 1.26.60 and NetherNet.
- Its current published PHP binaries are listed for Linux x86-64 and Windows x64; its install docs say ARM builds are not yet available.
- NetherNet helper pm-nethernet is included; Android/Bionic helper compatibility is not proven.
- Requires PHP 8.4 with engine-specific native extensions.
- **MineHost status:** not integrated as a validated runtime; Android ARM64 PHP + helper build is a critical blocker.

Sources: [VanillaMine](https://vanillamine.com/), [Install](https://docs.vanillamine.com/server/install), [Networking](https://docs.vanillamine.com/server/network).

### Altay — secondary PHP candidate
- Current downloads page advertises Altay 5.45.1 for Bedrock 1.26.60 and provides its own PHP binaries/extensions.
- Android ARM64 support and NetherNet behaviour must be verified from its exact release/build system.
- **MineHost status:** evaluation only.

Sources: [Altay](https://getaltay.org/), [Downloads](https://getaltay.org/downloads).

### Original PocketMine-MP
- Original upstream and its PHP-Binaries repository are archived; its last upstream release is reported as Bedrock 1.26.30.
- Do not treat the old pmmp/PocketMine-MP latest-release downloader as the long-term current Bedrock engine.
- **MineHost status:** existing adapter is legacy scaffolding; do not advertise as current Bedrock support.

Sources: [PocketMine docs](https://doc.pmmp.io/en/rtfd/), [Archived PHP-Binaries](https://github.com/pmmp/PHP-Binaries).

### Allay
- Java 21 Bedrock server with multi-version goals and a shaded JAR.
- Potentially shares the JVM runtime layer with Paper.
- Current public README does not prove NetherNet 26.60+ or Android device compatibility.
- **MineHost status:** evaluate only after verifying current protocol support and Android JVM performance.

Source: [Allay GitHub](https://github.com/AllayMC/Allay).

### PowerNukkitX / Cloudburst Nukkit
- Java-based Bedrock candidates.
- Require the same complete Android-compatible Java runtime as Java Edition hosting.
- Current NetherNet/client compatibility must be independently verified.
- **MineHost status:** future candidates, not selected.

### Official Bedrock Dedicated Server
- Mojang's current downloads are documented for Windows x64 and Ubuntu Linux x64, not ARM-based devices.
- Do not use x86 emulation as the default mobile path.
- **MineHost status:** not a native Android ARM64 option.

Source: [Mojang Bedrock server setup](https://help.minecraft.net/hc/en-us/articles/49914677714957).

## 2. Bedrock transport compatibility

Bedrock 26.60+ uses NetherNet; older RakNet-only probing is not sufficient. NetherNet uses WebRTC data channels with HTTP signaling and ICE. Current server connections require TCP signaling and UDP gameplay. iOS clients require HTTPS signaling with a trusted certificate.

Requirements for each engine:
- Correct GET /v1/join capability and metadata response.
- Correct POST /v1/join/{networkId} SDP exchange.
- Valid server identity assertion and stable operator identity key.
- Reachable ICE candidate from the player's network.
- Working DTLS/SCTP data channels and engine bridge.
- TCP and UDP reachability.
- Real clients from Android, Windows and iOS where advertised.

The existing MineHost RakNet UDP probe must only be used for an engine/client combination that actually supports RakNet. Implement a separate NetherNet-aware readiness test; an HTTP endpoint alone is not enough to prove a data channel can carry gameplay.

Source: [Mojang NetherNet onboarding guide](https://mojang.github.io/bedrock-protocol-docs/guides/nether-net-onboarding-guide/).

## 3. Java Edition candidate — PaperMC

- Existing source adapter ID: java-paper.
- Requires a complete Java 17/21-compatible JVM distribution and launcher for Android ARM64.
- Android ART does not execute desktop Java server JARs as a substitute for a desktop JVM.
- Amethyst-Android (the maintained successor to discontinued PojavLauncher) documents mobile OpenJDK 17/21 ARM64 ports; this is a runtime research lead, not proof that the runtime can execute headless Paper.
- Validate TLS, modules, native libraries, threads, filesystem, signal handling and headless server operation.
- Measure RAM, GC pauses, tick time, chunk generation and thermal load on real phones.
- **MineHost status:** Paper adapter/downloader source present; JVM and gameplay BLOCKED/UNVERIFIED.

Sources: [Amethyst-Android](https://github.com/AngelAuraMC/Amethyst-Android), [Amethyst runtime builds](https://github.com/AngelAuraMC/android-openjdk-build-multiarch).

## 4. Java/Bedrock cross-play

Candidate stack:
- PaperMC (or a validated Java server) as the Java server.
- GeyserMC as the Bedrock-to-Java bridge.
- Floodgate where the selected authentication model requires it.

Current Geyser builds support NetherNet (build 1237+) and Geyser build 1276 advertises Bedrock 26.40–26.63 / Java 26.3 support. Configure bedrock.transport: nethernet; the Bedrock endpoint requires TCP signaling and UDP gameplay on its port. The Bedrock port must not collide with Paper's Java TCP port.

Sources: [Geyser downloads](https://geysermc.org/download/), [Geyser NetherNet migration guide](https://geysermc.org/blog/preparing-for-nethernet/), [Geyser setup](https://geysermc.org/wiki/geyser/setup/).

**MineHost status:** configuration scaffolding only; operational cross-play NOT IMPLEMENTED/VERIFIED.

## 5. Runtime execution is a first-class dependency

For every engine/runtime combination, record:
- CPU ABI and Android API levels tested.
- Exact runtime build, source URL, licence and provenance.
- ELF type, interpreter, libc dependencies and native extensions.
- Trusted checksum/signature and verification result.
- Launcher path and executable placement.
- Startup logs and protocol readiness response.
- Stop/restart behaviour.
- RAM, CPU, battery and thermal measurements.
- Real client protocol versions tested.

A PHAR/JAR download is not a runnable server. A file named libphp.so or libjava.so is not automatically a CLI executable. Android 10+ execution from writable app data requires a proven launch strategy; setExecutable(true) alone is not enough.

## 6. Compatibility matrix

| Engine | Runtime | Android API/ABI | Bedrock/Java versions | Transport | LAN join | Remote join | Status |
|---|---|---|---|---|---|---|---|
| VanillaMine | PHP 8.4 + extensions + pm-nethernet | ARM64 Android build required | 1.26.60+ | NetherNet + legacy RakNet | Pending | Pending | EVALUATION |
| Altay | Project PHP build | ARM64 Android build required | 1.26.60 advertised | Verify | Pending | Pending | EVALUATION |
| Allay | Java 21 | Android ARM64 JVM required | Verify | Verify NetherNet | Pending | Pending | EVALUATION |
| Paper | Java 17/21 | Android ARM64 JVM required | Exact tested versions | TCP | Pending | Pending | BLOCKED |
| Paper + Geyser | Java 17/21 + Geyser | Android ARM64 JVM required | Java 26.3 / Bedrock 26.40–26.63 advertised by current Geyser | TCP + NetherNet | Pending | Pending | NOT IMPLEMENTED |

Never fill this table with guessed compatibility or untested RAM numbers.

## 7. Selection principle

Choose the smallest maintained engine set that meets the product promise:
1. One Bedrock engine with a reproducible Android ARM64 runtime and current NetherNet support.
2. One Java engine with a reproducible Android ARM64 JVM and real client join.
3. Geyser/Floodgate only after both underlying runtime and transport paths work.

Prioritize stability, low resource use, current protocol support and maintainability over the number of engine options in the UI.
