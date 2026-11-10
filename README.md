# MineHost

> **Phone-powered Minecraft hosting. Your device. Your world. Your network.**

MineHost is an Android application project intended to let a user run a Minecraft server on their own Android device and invite players over local or public networks. The host phone—not a MineHost cloud game server—does the actual game-server work.

## Project status

**Active development — not production-ready.**

The repository contains an Android application foundation, server-engine abstractions, world/backup code, and networking-provider groundwork. However, the latest source audit identified unresolved runtime and end-to-end verification blockers. No real Minecraft server session has yet been demonstrated on a physical Android device in the current audit.

Do not interpret the planned feature list as a claim that every feature currently works. See:
- [Production readiness audit](docs/PRODUCTION_READINESS.md)
- [Master roadmap](docs/MINEHOST_MASTER_ROADMAP.md)
- [Testing status](docs/TESTING.md)
- [Device test guide](docs/DEVICE_TESTING.md)
- [Known issues](docs/KNOWN_ISSUES.md)

## Product principles

- The Minecraft server process and world run locally on the user's Android device.
- Local hosting and LAN multiplayer are intended to remain free.
- Support Wi-Fi and mobile data.
- Prefer direct connections; use MineHost-operated relays when direct reachability is not possible.
- Do not require a managed third-party tunnel provider as a permanent production dependency.
- Keep worlds and server data under the user's control and exportable.
- Treat Java, Bedrock, and cross-play as separate compatibility commitments and advertise each only after real-client verification.
- Optimize and measure network latency separately from server TPS/MSPT and Android thermal performance.
- Never claim zero lag, universal device support, or uninterrupted connectivity.

MineHost can own and operate its coordination and relay software, while still relying on ordinary internet transit, data-centre connectivity, DNS registrars, Android, and upstream open-source projects. "Independent" means no mandatory third-party managed game-hosting or tunnel service—not independence from the internet itself.

## Technology foundation

- Kotlin and Jetpack Compose
- Clean Architecture / MVVM
- Room, DataStore, Hilt, Kotlin coroutines
- Android foreground service and process supervision
- Server-engine and tunnel-provider abstractions

The final Bedrock engine and Android-compatible PHP runtime, Java engine and Android-compatible JVM, and production networking stack remain subject to feasibility and physical-device validation. The legacy Playit adapter remains unbound source code; the app currently binds an unavailable MineHost provider and does not start a third-party tunnel.

## Build from source

Requirements: Android Studio/Android SDK, JDK 17, and a supported Android ARM64 test device for runtime validation.

```bash
git clone https://github.com/nomanaqil56/MineHost.git
cd MineHost
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

A successful APK build does **not** prove that a Minecraft engine or public tunnel works. Follow the [device testing guide](docs/DEVICE_TESTING.md) for real-device acceptance tests.

## Contributing and development

Read [docs/MINEHOST_MASTER_ROADMAP.md](docs/MINEHOST_MASTER_ROADMAP.md) before changing architecture or adding features. Update progress and testing evidence with each meaningful change. Preserve working UI and code; prefer small, testable changes over broad rewrites.

## Licensing and trademarks

See the repository's LICENSE file for MineHost's licence. Minecraft is a trademark of Mojang/Microsoft. MineHost is an independent third-party project and is not affiliated with, endorsed by, or associated with Mojang or Microsoft. Review current Minecraft usage guidelines before public distribution or monetization.
