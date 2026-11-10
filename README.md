# MineHost 🎮📱

> **Phone-first Minecraft server hosting for Android.**  
> Your device. Your world. Your network.

MineHost is an Android application project intended to run Minecraft server software locally on a user's Android device. The phone remains the game host and stores the authoritative world. MineHost's long-term goal is to provide direct-first, international connectivity through infrastructure that MineHost operates itself.

**Project status: active development — not production-ready.** Real-device execution, Android runtime compatibility, and public-internet connectivity still have blockers. See the current evidence-based status in [Production Readiness](docs/PRODUCTION_READINESS.md) and the [Master Roadmap](docs/MINEHOST_MASTER_ROADMAP.md).

## Product principles

- **Local-first hosting:** the Minecraft server process runs on the Android host device; MineHost does not rent a cloud game server on the user's behalf.
- **Free core hosting:** basic local hosting is intended to remain free.
- **Direct-first networking:** prefer LAN and viable direct public routes; use MineHost-operated relays when NAT/CGNAT prevents direct access.
- **Infrastructure independence:** the target is to own and operate MineHost's coordinator and relay software rather than permanently depending on a managed tunnel provider.
- **World ownership:** local worlds and server files remain exportable and portable.
- **Honest status:** a process being alive or a tunnel assigning an endpoint is not proof that a real Minecraft client can join.

## Current technical foundation

- Kotlin, Jetpack Compose, Material 3
- Clean Architecture / MVVM structure
- Room persistence and Hilt dependency injection
- Server-engine abstraction and runtime manager
- Bedrock and Java engine integration work (runtime execution is not yet proven on physical Android devices)
- Tunnel-provider abstraction; the current Playit adapter is an experimental integration, not the intended permanent dependency
- Local networking and connection diagnostics
- World backup, restore, import/export utilities
- Foreground service, wake-lock and thermal-monitoring components
- Unit-test sources and a Python verification harness

These are source-code capabilities, not a claim that every feature has passed end-to-end testing.

## Build from source

### Requirements
- Android Studio / Android SDK compatible with the project Gradle configuration
- JDK 17 for the Gradle build
- Android ARM64 device for actual server-runtime testing

### Build
```bash
git clone https://github.com/nomanaqil56/MineHost.git
cd MineHost
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

On Windows:
```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

A successful APK build does **not** prove that a Minecraft engine can execute on a physical Android device. Follow the device and external-network acceptance tests before treating a server engine as supported.

## Documentation

- [Master Product & Engineering Roadmap](docs/MINEHOST_MASTER_ROADMAP.md)
- [System Architecture](docs/ARCHITECTURE.md)
- [Technical Research & Feasibility](docs/TECHNICAL_RESEARCH.md)
- [Server Engines & Compatibility](docs/SERVER_ENGINES.md)
- [Networking Architecture](docs/NETWORKING.md)
- [Storage & Backup Management](docs/STORAGE.md)
- [Build Guide](docs/BUILD_GUIDE.md)
- [Testing](docs/TESTING.md)
- [Device Testing](docs/DEVICE_TESTING.md)
- [Development Progress](docs/DEVELOPMENT_PROGRESS.md)
- [Known Issues](docs/KNOWN_ISSUES.md)
- [Production Readiness](docs/PRODUCTION_READINESS.md)

## License and Minecraft notice

**Repository licence is currently unspecified:** no root `LICENSE` file was present in the reviewed repository tree. Do not assume the source is Apache-2.0 or otherwise licensed for reuse until the project owner chooses and adds a licence. Each bundled or downloaded engine, runtime, library, and binary also remains subject to its own licence and redistribution terms; review those terms before distribution.

Minecraft is a trademark of Mojang AB and Microsoft. MineHost is an independent third-party project and is not affiliated with, endorsed by, or associated with Mojang or Microsoft. MineHost does not distribute Minecraft game assets.
