# MineHost

> **Your device. Your world. Your network.**
>
> MineHost is an Android-first project to run Minecraft server software locally on the user's own phone, keep worlds under the user's control, and make servers reachable by friends on local and remote networks.

## Product direction

MineHost is designed around four principles:

- **Phone-hosted:** The actual Minecraft server process and world files run on the Android host device. MineHost is not a cloud game-server rental panel.
- **Free local hosting:** Local hosting and same-LAN play should not require a hosted service or relay.
- **Direct-first connectivity:** Prefer LAN and viable direct public paths; use a MineHost-operated relay only when direct connectivity is unavailable.
- **Ownership and portability:** Users can manage, back up, restore and export their worlds and server files.

The long-term goal is low-latency international multiplayer, Wi-Fi and cellular support, and tested Java/Bedrock compatibility, with MineHost operating its own control plane and relay software rather than permanently depending on a managed tunnel provider.

**This is the product goal, not a claim that all of these capabilities are currently implemented.**

## Current implementation

The repository contains a native Android application built with Kotlin, Jetpack Compose, Room and Hilt. Source currently includes:

- Server dashboard, creation/detail, console, player, diagnostics and backup screens.
- Server-engine abstractions and Bedrock/Paper adapter scaffolding.
- Runtime and server artifact download management.
- Process supervision and protocol readiness probes.
- Local network discovery and a tunnel-provider abstraction.
- A Playit tunnel adapter (external provider; not the desired final independent production network).
- World backup/restore validation and safety mechanisms.
- Foreground hosting service, wake-lock and thermal monitoring.

## Current limitations — read before using

MineHost is **not production-ready yet**.

- No physical Android device has been verified in the latest repository test notes.
- No real Minecraft client has been shown joining a server launched by the app.
- Android-dependent code has not been compiled in the latest documented audit environment.
- The repository's latest notes report 40 pure-Kotlin tests passing outside Gradle; this does not validate Android integration or real server execution.
- The production-readiness branch now targets Android 16 / API 36 with an updated AGP/Gradle/Kotlin toolchain; CI and Android 16 device verification are still pending.
- PHP and Java runtime launch paths on Android ARM64 remain critical unverified blockers. A shared library named `libphp.so` or `libjava.so` is not automatically a valid CLI executable.
- Java hosting and Geyser cross-play are not verified operational capabilities.
- Current tunnel integration is Playit-specific. MineHost-owned relay/control-plane infrastructure and direct NAT traversal are future work.
- Bedrock protocol compatibility must be checked against the exact server and client versions supported; do not assume old RakNet-only behaviour is sufficient.

See [Production Readiness](docs/PRODUCTION_READINESS.md), [Known Issues](docs/KNOWN_ISSUES.md), and [Development Progress](docs/DEVELOPMENT_PROGRESS.md).

## Architecture and roadmap

The authoritative product scope, architecture constraints, phased milestones and acceptance gates are in [MINEHOST_MASTER_ROADMAP.md](MINEHOST_MASTER_ROADMAP.md).

Subsystem documentation:
- [Architecture](docs/ARCHITECTURE.md)
- [Server engines](docs/SERVER_ENGINES.md)
- [Networking](docs/NETWORKING.md)
- [Storage](docs/STORAGE.md)
- [Technical research](docs/TECHNICAL_RESEARCH.md)
- [Testing](docs/TESTING.md)
- [Physical-device test guide](docs/DEVICE_TESTING.md)
- [Build guide](docs/BUILD_GUIDE.md)

## Build

Requirements and setup are described in [docs/BUILD_GUIDE.md](docs/BUILD_GUIDE.md).

Typical commands:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

On Windows:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

A successful build is only a build check. It does not prove a server engine, runtime, tunnel or real Minecraft client works.

## Project status

Development is active. Work should proceed in small, testable phases:

1. Reproduce a clean Android build and tests.
2. Prove one real local server engine on a physical ARM64 Android device.
3. Prove LAN gameplay and world persistence.
4. Build and validate MineHost-operated public connectivity.
5. Add Java and cross-play only after runtime and protocol compatibility are proven.
6. Complete security, legal, performance, device and release audits.

## Licensing and Minecraft notice

The project-level licence is not confirmed in this revision: the inspected root tree does not contain a `LICENSE` file. Choose and add the intended licence before public distribution. Each bundled or downloaded dependency has its own licence and redistribution requirements, which must be reviewed separately.

Minecraft is a trademark of Mojang AB and Microsoft. MineHost is an independent third-party project and is not affiliated with, endorsed by, or associated with Mojang or Microsoft. Follow the current Minecraft usage guidelines before public distribution.
