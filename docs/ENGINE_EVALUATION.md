# MineHost — Minecraft Server Engine Evaluation

> **Decision status:** No Bedrock engine is approved as the long-term production default yet.
> **Reason:** PocketMine-MP and its official PHP-Binaries repository were archived in 2026. The current app also lacks a working Android PHP/JVM runtime. Engine selection must be reopened before we invest in more adapters.

## Product constraints

A candidate must:
1. Run on ARM64 Android without requiring the user to install Termux, PRoot, or another app.
2. Be legally distributable or downloadable under a reviewed licence and supply-chain policy.
3. Support current Bedrock or Java clients and the required transport for the version matrix we publish.
4. Start and run in the Android app's process/runtime model without violating Android executable-memory restrictions or Google Play policy.
5. Persist worlds correctly in app-private storage and expose a safe lifecycle API.
6. Be measurable and performant on mid-range phones under sustained thermal load.
7. Have an active upstream or a credible MineHost-maintained fork strategy.
8. Provide a practical plugin/mod ecosystem and a supportable update path.

## Candidate comparison

| Candidate | Language/runtime | Strengths | Risks / missing proof | Current disposition |
|---|---|---|---|---|
| PocketMine-MP | PHP | Mature Bedrock custom server; existing adapter/configuration work; official PHP-Binaries releases include Android ARM64 archives. | Official PocketMine-MP repository was archived July 9, 2026; official PHP-Binaries repository was archived August 8, 2026. Existing PHP assets do not prove Android 10+ app-private execution or current transport compatibility. | Do not make the archived upstream the default without a maintained fork and physical-device proof. |
| PowerNukkitX | Java 21+ | Active Bedrock server project; current release notes advertise modern Bedrock versions; richer vanilla feature set than some lightweight engines. | Requires Java 21+ and official installation guidance recommends 2 GB minimum / 4 GB+ RAM. A usable Android JVM and sustained phone performance are not proven. | Strong comparison candidate, but JVM feasibility is a P0 spike. |
| Dragonfly | Go 1.26+ | Asynchronous architecture; can be embedded as a library; Go has an Android ARM64 target, so a native integration may avoid a separate PHP/JVM runtime. | Not a drop-in binary-first server; requires a MineHost-owned Go wrapper/integration. Vanilla parity, plugins, Android build mode, storage behavior, and current transport must be verified. | Highest-priority native-runtime feasibility spike, not yet approved as a production engine. |
| Chorus (bedrock-crustaceans) | Rust | Native systems-language implementation; Apache-2.0 licence; potential Android ARM64 compilation path. | Project maturity, vanilla parity, current client compatibility, plugin ecosystem, and Android networking/storage are unverified. | Experimental candidate only. |

## Source references

- [PocketMine-MP repository](https://github.com/pmmp/PocketMine-MP) — upstream repository is archived.
- [PocketMine PHP-Binaries](https://github.com/pmmp/PHP-Binaries) — Android ARM64 PHP assets/build scripts exist, but repository is archived.
- [PowerNukkitX releases](https://github.com/PowerNukkitX/PowerNukkitX/releases) and [official installation requirements](https://powernukkitx.org/install) — Java 21+; current release compatibility must be rechecked at implementation time.
- [Dragonfly](https://github.com/df-mc/dragonfly) — Go Bedrock server generally used as an extensible library; currently requires Go 1.26+.
- [Chorus](https://github.com/bedrock-crustaceans/Chorus) — Rust Bedrock server, Apache-2.0.

## Required technical spikes

### Spike A — Dragonfly native Android viability (first priority)

- [ ] Pin an upstream Dragonfly commit; do not use an unpinned latest branch in release builds.
- [ ] Check its Go toolchain and dependency requirements.
- [ ] Build a minimal Android ARM64 wrapper with Go and the Android NDK.
- [ ] Evaluate supported build modes (shared library/JNI entrypoint versus packaged native executable).
- [ ] Verify Android linker compatibility, network socket binding, filesystem paths, signals, and graceful stop.
- [ ] Prove LevelDB/world persistence in app-private storage.
- [ ] Verify whether the required Bedrock transport is supported by that pinned commit.
- [ ] Join with a real current Bedrock client on LAN, then through a remote network.
- [ ] Benchmark CPU, memory, TPS, MSPT, thermal behavior, and battery.
- [ ] Audit MIT and all transitive dependency licences.

**Go/no-go:** If native Android integration is not robust, or server fidelity/transport is inadequate, do not force Dragonfly into production.

### Spike B — PowerNukkitX on Android

- [ ] Select a pinned PowerNukkitX build and exact Java 21 runtime requirement.
- [ ] Prototype an Android-compatible JVM with JNI and packaged native libraries.
- [ ] Validate the JAR/classpath/module setup and required JDK APIs.
- [ ] Run a real server on a physical device.
- [ ] Measure RAM baseline, startup time, CPU, TPS/MSPT, and sustained thermal behavior.
- [ ] Verify world storage and plugins.
- [ ] Verify current Bedrock transport and real-client join.
- [ ] Audit LGPL-3.0 and bundled dependencies.

**Go/no-go:** Reject if the minimum viable JVM/server footprint is unsuitable for realistic phones or cannot be legally/technically packaged.

### Spike C — PocketMine fork stewardship

- [ ] Identify an actively maintained fork, not merely an old Android binary mirror.
- [ ] Confirm security patch ownership and update cadence.
- [ ] Verify current PHP version and extensions required by the fork.
- [ ] Rebuild the PHP runtime reproducibly from source with a maintained Android toolchain.
- [ ] Package or load it in a policy-compliant way.
- [ ] Verify current Bedrock transport and real-client gameplay.
- [ ] Document LGPL obligations and every bundled dependency.

**Go/no-go:** Only continue if MineHost can maintain the fork or has a credible upstream to follow.

### Spike D — Chorus maturity check

- [ ] Check recent commits/releases, supported protocol, and vanilla feature completeness.
- [ ] Build for Android ARM64.
- [ ] Validate UDP transport and world persistence.
- [ ] Test real clients and profile performance.
- [ ] Audit dependencies and licence.

## Decision rule

Do not choose based only on language, download size, or a successful compile. The selected engine must pass a real Android physical-device join, world persistence, protocol/version compatibility, and sustained-load test. Prefer the engine that minimizes total runtime overhead while offering adequate vanilla fidelity and a maintainable update path.

The engine adapter API should remain modular so MineHost can support more than one engine later without making every engine a first-release requirement.
