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

The repository contains a native Android application built with Kotlin, Jetpack Compose, Room, and Hilt. The Bedrock path currently includes:

- Axolotl-PM 5.51.1, pinned by release tag, asset URL and SHA-256.
- PHP 8.4.27 ZTS Android ARM64 CLI, bundled in the APK and checked using `php -n`.
- Runtime verification for all 17 required compiled-in PHP extensions.
- Generated server configuration for modern Bedrock NetherNet: TCP signaling and multiplexed UDP WebRTC traffic on port 19132.
- Process supervision and a protocol readiness probe for NetherNet HTTP signaling, with legacy RakNet fallback.
- Server dashboard, creation/detail, console, player, diagnostics, and backup screens.
- World backup/restore validation, foreground hosting service, wake-lock, and thermal-monitoring code.

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

1. Run the current Android CI after the NetherNet/configuration changes and fix any build or test failures.
2. Re-run server startup through the MineHost UI on the Vivo Android 16 device.
3. Join from a second Bedrock 1.26.60+ client on the same LAN; verify gameplay, reconnect and world persistence.
4. Validate TCP/UDP port behavior and Android WebRTC networking.
5. Implement MineHost-owned public connectivity after LAN play is proven.
6. Add Java hosting and cross-play only after a complete Android-compatible JVM and process launch are proven.

## Licensing and Minecraft notice

The project-level licence must be confirmed: the current main-branch audit did not find a root `LICENSE` file. Add the intended licence text before public distribution. Each bundled or downloaded dependency has its own licence and redistribution requirements, which must be reviewed separately.

Minecraft is a trademark of Mojang AB and Microsoft. MineHost is an independent third-party project and is not affiliated with, endorsed by, or associated with Mojang or Microsoft.
