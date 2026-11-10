# MineHost — Server Engine Status

> **No Minecraft engine is currently certified as production-ready on Android by this repository.** The existing adapters are source implementations, not proof of a working server.

## Current adapters

| Engine | Edition | Source state | Runtime blocker | Release status |
|---|---|---|---|---|
| PocketMine-MP v5 adapter | Bedrock | Adapter/configuration code exists | No verified Android PHP runtime; upstream PocketMine-MP and official PHP-Binaries repositories were archived in 2026 | Blocked; do not advertise as supported |
| PaperMC adapter | Java | JAR download/configuration code exists | No verified Android ARM64 JVM | Blocked; do not advertise as supported |
| Geyser + Floodgate | Cross-play bridge | Configuration groundwork exists | Requires a working Java server/JVM and tested protocol compatibility | Unavailable |
| PowerNukkitX | Bedrock | Research candidate only | Java 21+ Android JVM and phone resource profile unproven | Not integrated |
| Dragonfly | Bedrock | Research candidate only | Requires a MineHost Go wrapper/native Android integration; feature and protocol parity unproven | Not integrated |
| Chorus (Rust) | Bedrock | Research candidate only | Maturity, protocol support, Android integration and vanilla parity unproven | Not integrated |

## Engine selection policy

A server engine is supported only after it:
- runs on a supported physical Android ARM64 device without requiring users to install another app;
- has a maintainable, licensed, reproducible runtime/distribution path;
- supports the declared Minecraft client versions and transport;
- accepts a real client connection;
- persists worlds correctly across stop/restart;
- survives sustained-load and thermal testing.

Do not select an engine solely because it compiles or has a low idle-memory estimate. Compare runtime overhead, vanilla parity, plugin ecosystem, protocol support, Android execution model, licensing, and upstream maintenance.

See [Engine Evaluation](ENGINE_EVALUATION.md) for candidate analysis and technical spikes, and [Master Roadmap](MINEHOST_MASTER_ROADMAP.md) for the release gates.
