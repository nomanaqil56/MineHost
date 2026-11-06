# MineHost 🎮📱

> **Native Android Minecraft Server Hosting Platform**  
> Turn your Android smartphone or tablet into a high-performance, dedicated Minecraft server machine.

---

## 🌟 Overview

**MineHost** is a complete, production-grade native Android application that hosts real Minecraft servers directly on your mobile device.

Unlike third-party dashboard apps that merely connect to cloud hosting providers, **MineHost runs the server process locally on the Android device hardware**. Your phone is the host machine, your device storage holds your worlds, and incoming connections from friends around the world are bridged seamlessly via encrypted low-latency reverse tunnels.

### Core Features

- ⚡ **Genuine Local Server Execution:** Runs Minecraft server engines (PocketMine-MP, PowerNukkitX, PaperMC) directly on Android ARM64.
- 🌍 **Worldwide Multiplayer:** Automatic reverse tunneling via Playit.gg — players join from any country without port forwarding or router configuration.
- 🔄 **Cross-Play Ready:** Integrated GeyserMC bridge allows Bedrock players (iOS, Android, Windows, Consoles) and Java players (PC, Mac) to play together on the same server.
- 💾 **100% Local World Ownership:** World data never leaves your device. Features automated snapshots, `.mcworld` import/export, and safe recovery guards.
- 📱 **Background Hosting with Foreground Service:** Keeps the server alive while you minimize the app, lock your screen, or play Minecraft on the same phone.
- 💻 **Live Terminal Console:** Monospace log streaming, color-coded log levels, live player tracking, and server command administration (`say`, `kick`, `gamemode`, `save-all`).
- 🎨 **Dark Gaming UI:** Designed in Jetpack Compose and Material 3 with a custom, sleek server-management aesthetic.

---

## 🏗️ Architecture

MineHost is architected in Kotlin following **Clean Architecture** and modern Android standards:
- **Presentation:** Jetpack Compose, Material 3, Navigation Compose, Kotlin StateFlow.
- **Domain:** Pure business logic, `ServerEngine` abstraction, `TunnelProvider` interface, and lifecycle state machines.
- **Data:** Room Database (SQLite), Jetpack DataStore, and structured private file I/O (`/files/minehost/`).
- **Platform Services:** Android `ForegroundService` (`specialUse|dataSync`), `WakeLockManager`, and `ProcessBuilder` execution.

For detailed architecture diagrams and subsystem specifications, see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

---

## 🚀 Getting Started

### Prerequisites
- Android 9.0+ (API 28+), recommended Android 12+ (API 31+).
- ARM64 device (Snapdragon 7xx/8xx, Dimensity 800+, or equivalent).
- 4GB+ RAM (6GB - 8GB recommended for Java / Cross-play).
- 2GB+ free internal storage.

### Building from Source
```bash
# Clone the repository
git clone https://github.com/nomanaqil56/MineHost.git
cd MineHost

# Build the Debug APK using Gradle
./gradlew assembleDebug

# Install on a connected Android device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
For complete build instructions, see [docs/BUILD_GUIDE.md](docs/BUILD_GUIDE.md).

---

## 📚 Documentation

- [Technical Research & Feasibility](docs/TECHNICAL_RESEARCH.md)
- [System Architecture](docs/ARCHITECTURE.md)
- [Supported Server Engines](docs/SERVER_ENGINES.md)
- [Networking & Tunnel Architecture](docs/NETWORKING.md)
- [Storage & Backup Management](docs/STORAGE.md)
- [Testing & Quality Assurance](docs/TESTING.md)
- [Development Progress](docs/DEVELOPMENT_PROGRESS.md)
- [Known Issues & Limitations](docs/KNOWN_ISSUES.md)

---

## 📄 License
This project is open-source under the Apache 2.0 License.
Minecraft is a registered trademark of Mojang AB / Microsoft. MineHost is not affiliated with Mojang AB or Microsoft.
