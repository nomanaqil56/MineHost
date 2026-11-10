# MineHost

> **Your device. Your world. Your network.**

MineHost is an Android-first project to run Minecraft server software locally on a user's own phone, keep worlds under the user's control, and make servers reachable by friends on local and remote networks.

## Product direction

MineHost is built around four principles:

- **Phone-hosted:** The Minecraft server process and world files should run on the Android host device. MineHost is not a cloud game-server rental panel.
- **Free local hosting:** Local hosting and same-LAN play should not require a hosted service or relay.
- **Direct-first connectivity:** Prefer LAN and viable direct public paths; use a MineHost-operated relay only when direct connectivity is unavailable.
- **Ownership and portability:** Users should be able to manage, back up, restore, and export their worlds and server files.

The long-term goal is low-latency multiplayer over Wi-Fi and cellular, Java/Bedrock compatibility, and MineHost-operated control and relay infrastructure rather than a permanent dependency on a managed tunnel provider.

**These are product goals, not claims that every capability is implemented today.**

## Current implementation

The repository contains a native Android application foundation built with Kotlin, Jetpack Compose, Room, and Hilt. Source currently includes:

- Server dashboard, creation/detail, console, player, diagnostics, and backup screens.
- Server-engine abstractions and Bedrock/Paper adapter scaffolding.
- Runtime and server-artifact download management.
- Process supervision and protocol-readiness probes.
- Local-network discovery and a tunnel-provider abstraction.
- A Playit tunnel adapter for optional experiments; it is not MineHost's final networking architecture.
- World backup/restore validation and safety mechanisms.
- Foreground hosting service, wake-lock, and thermal-monitoring code.

## Current limitations

**MineHost is not production-ready.** Source code existing in the repository is not proof that it runs successfully on a device.

- No physical Android device has been verified in the latest repository audit.
- No real Minecraft client has been shown joining a server launched by MineHost.
- Earlier development notes report 40 pure-Kotlin tests passing outside Gradle. This does not prove Android integration or gameplay.
- The latest main-branch audit did not reproduce a Gradle Android build; Android-dependent changes must be compiled and tested.
- A compatible Android ARM64 PHP runtime and a proven launch path are missing. Bedrock hosting is therefore blocked.
- A complete Android-compatible ARM64 Java runtime and launch path are missing. Paper hosting is therefore blocked.
- Geyser/Floodgate cross-play is not operational and must remain unavailable until the Java runtime works.
- The Playit integration has not been validated with a real Android agent session and external Minecraft client.
- MineHost-owned relay/control-plane infrastructure and direct NAT traversal are not implemented.
- Bedrock protocol compatibility must be validated against the exact server and client versions selected.

See [Production Readiness](docs/PRODUCTION_READINESS.md), [Known Issues](docs/KNOWN_ISSUES.md), and [Development Progress](docs/DEVELOPMENT_PROGRESS.md).

## Architecture and documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Server engines](docs/SERVER_ENGINES.md)
- [Networking](docs/NETWORKING.md)
- [Storage](docs/STORAGE.md)
- [Technical research](docs/TECHNICAL_RESEARCH.md)
- [Testing](docs/TESTING.md)
- [Physical-device test guide](docs/DEVICE_TESTING.md)
- [Build guide](docs/BUILD_GUIDE.md)

## Build

See [docs/BUILD_GUIDE.md](docs/BUILD_GUIDE.md) for prerequisites and setup.

From the repository root:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

On Windows:

```powershell
.\\gradlew.bat assembleDebug
.\\gradlew.bat testDebugUnitTest
```

A successful build is only a build check. It does not prove a server engine, native runtime, tunnel, or real Minecraft client works.

## Immediate development priorities

1. Reproduce a clean Android build and Gradle unit-test baseline on `main`.
2. Select and validate one maintained Bedrock engine and a compatible Android ARM64 PHP runtime/launcher.
3. Start that real server on a physical device and prove same-LAN gameplay and world persistence.
4. Design and implement MineHost-owned public connectivity, with LAN working independently of the backend.
5. Add Java hosting and cross-play only after a complete Android-compatible JVM and server process are proven.
6. Complete security, legal, performance, device, and release audits.

## Licensing and Minecraft notice

The project-level licence must be confirmed: the current main-branch audit did not find a root `LICENSE` file. Add the intended licence text before public distribution. Each bundled or downloaded dependency has its own licence and redistribution requirements, which must be reviewed separately.

Minecraft is a trademark of Mojang AB and Microsoft. MineHost is an independent third-party project and is not affiliated with, endorsed by, or associated with Mojang or Microsoft.
