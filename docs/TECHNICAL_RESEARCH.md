# MineHost — Technical Research & Feasibility Analysis

> **Research update (2026-11-10):** The original PocketMine-MP and PMMP PHP-Binaries upstream repositories have been archived. Axolotl-PM is now the selected maintained PocketMine-compatible fork, with a matching Android ARM64 PHP runtime candidate. The archive has not yet been inspected or launched through MineHost; no runtime support is claimed. See [Decision 0001](decisions/0001-bedrock-engine-and-php-runtime.md).

## 1. Executive Summary

MineHost transforms an Android device (smartphones and tablets) into a dedicated, self-contained Minecraft server host. Unlike cloud-managed services where mobile apps serve only as dashboards for remote virtual machines, MineHost executes the Minecraft server process directly on Android's ARM64 hardware, stores all world and player data within local device storage, and bridges incoming traffic across cellular NAT / CGNAT via a high-performance UDP/TCP reverse tunnel.

This document presents the technical evaluation of server engines, Android runtime environments, networking protocols, background process constraints, and mitigation strategies required to deliver a reliable host.

---

## 2. Server Engine Evaluation

### 2.1 Minecraft Bedrock Edition

| Engine | Primary Language | Vanilla Parity | ARM64 Android Support | Memory Footprint | Recommendation |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Axolotl-PM (PM5 fork)** | PHP 8.4 (selected runtime baseline) | Moderate (not full vanilla parity) | Android ARM64 runtime archive exists; MineHost launch path unverified | Not measured on MineHost | **Primary Bedrock engine candidate** |
| **PowerNukkitX (PNX)** | Java 21 | High (Vanilla mobs, blocks, redstone, dimensions) | High (Requires Android ARM64 JVM) | Medium (600MB - 1.2GB) | **Primary Bedrock Engine for high parity** |
| **Bedrock Dedicated Server (BDS)** | C++ | 100% Official Vanilla | Incompatible (Only x86_64 Linux/Windows; no ARM64 binary) | Medium (500MB - 1GB) | Not viable natively without heavy x86 emulation |
| **Cloudburst Nukkit** | Java 17 | Moderate-High | High (Requires Android ARM64 JVM) | Medium (400MB - 800MB) | Supported alternative |

#### Key Technical Findings:
1. **Mojang Bedrock Dedicated Server (BDS):** Mojang only ships precompiled x86_64 Linux and Windows binaries. There are no official ARM64 Linux or Android builds. Running BDS on ARM64 requires Box64 / QEMU user-mode translation, which introduces significant CPU overhead and instability on mobile chips. It is therefore unsuitable as a primary engine.
2. **Axolotl-PM:** Maintained continuation of PocketMine-MP after the original upstream was archived in July 2026. Its matching PHP-Binaries project publishes Android ARM64 archives. The current selected candidate is Axolotl-PM 5.51.1 / Bedrock 1.26.60 with PHP 8.4. No MineHost device performance or launch test has yet been completed.
3. **PowerNukkitX:** Offers superior vanilla parity (water physics, nether/end dimensions, redstone, entity AI). Requires Java 21 runtime.

---

### 2.2 Minecraft Java Edition

| Engine | Primary Language | Compatibility & Plugins | ARM64 Android Support | Memory Footprint | Recommendation |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **PaperMC** | Java 17 / 21 | High performance, Bukkit/Spigot/Paper plugins | Requires ARM64 OpenJDK | 1.0GB - 2.5GB | **Primary Java Engine** |
| **Purpur** | Java 17 / 21 | High customization, Bukkit/Spigot/Paper | Requires ARM64 OpenJDK | 1.0GB - 2.5GB | Supported alternative |
| **Vanilla Server** | Java 17 / 21 | 100% Vanilla | Requires ARM64 OpenJDK | 1.5GB - 3.0GB | High overhead compared to Paper |
| **Fabric** | Java 17 / 21 | Mod support | Requires ARM64 OpenJDK | 1.0GB - 2.5GB | Future expansion |

#### Key Technical Findings:
- Android runs on Android Runtime (ART/Dalvik), which executes `.dex` bytecode and cannot execute Java desktop `.class` or `.jar` bytecode directly.
- Running Paper or PowerNukkitX requires an actual Java Virtual Machine (`bin/java` + `libjvm.so` + OpenJDK core libraries) compiled for `aarch64-linux-android` (or compatible musl/glibc rootfs).
- PaperMC provides the best memory optimizations (chunk loading, entity ticking, async pathfinding), which is critical for phones with 4GB - 8GB RAM.

---

### 2.3 Cross-Play Architecture (Java + Bedrock)

To enable Minecraft Bedrock players (iOS, Android, Windows, Consoles) to join a Java Edition server running on the device, MineHost integrates **GeyserMC**:
- **Geyser-Standalone / Geyser Plugin:** Translates Bedrock RakNet UDP packets and Bedrock game protocol into Java TCP packets in real-time.
- **Floodgate:** Allows Bedrock players to join Java servers without requiring a Java Edition Microsoft account purchase.
- **Resource Considerations:** Running Paper + Geyser on a single mobile device requires at least 6GB to 8GB of physical RAM.

---

## 3. Android Runtime & Execution Strategy

### 3.1 Android 10+ Executable Policy and Runtime Packaging
Android process execution is constrained by filesystem mount flags, SELinux and dynamic-linker requirements:
- Do not assume a file downloaded into app-writable `filesDir` can be executed just because `setExecutable(true)` succeeds.
- Do not assume that naming an ELF file `libphp.so` makes it a shared library or makes it runnable. Inspect its ELF type, architecture, program interpreter, dependencies and actual `execve` result.
- Packaging a binary under `jniLibs/<abi>/` may make it available in `nativeLibraryDir`, but a normal shared library is not a CLI process and a musl-linked binary may still require a matching interpreter and libraries.
- MineHost has **not yet** proven an Android PHP runtime packaging strategy. The Axolotl-PM Android ARM64 archive is a candidate pending archive inspection and physical-device launch testing.
- Until that gate passes, Bedrock runtime installation and server start must remain unavailable rather than falsely reporting a runtime as installed.

### 3.2 Process Management & Isolation
- Server engines run as child processes managed via `ProcessBuilder`.
- Standard Output and Standard Error streams are captured asynchronously on dedicated coroutine dispatchers to prevent buffer deadlocks.
- Stdin is piped to a command dispatch queue, allowing direct console administration (`say`, `kick`, `stop`, `save-all`).

---

## 4. Worldwide Networking & Tunneling Strategy

### 4.1 The NAT / CGNAT Problem
Mobile networks (4G/5G LTE) and residential Wi-Fi connections use Carrier-Grade NAT (CGNAT) or symmetric NAT without public IPv4 addresses or UPnP port-forwarding capabilities. A server listening on `0.0.0.0:19132` is unreachable from outside the local Wi-Fi subnet.

### 4.2 Protocol Requirements
- **Bedrock Edition:** Uses RakNet over **UDP** (default port `19132`).
- **Java Edition:** Uses **TCP** (default port `25565`).
- Any tunnel provider must support low-latency UDP packet forwarding for Bedrock and TCP stream multiplexing for Java.

### 4.3 Tunnel Architecture Evaluation

| Solution | UDP Support | TCP Support | Self-Hosting Required | Setup Complexity | Assessment |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Playit.gg** | Yes (Native) | Yes (Native) | No (Global Anycast edge) | Minimal (Agent API / claim code) | **Selected Primary Tunnel** |
| **Bore.pub** | TCP Only (no native UDP) | Yes | Optional | Low | Unsuitable for Bedrock |
| **Rathole / FRP** | Yes | Yes | Yes (Requires VPS) | High | Ideal for custom self-hosted tier |
| **Ngrok** | Limited (TCP only on free tier) | Yes | No | Low | Incompatible with Bedrock UDP on free tier |
| **Cloudflare Tunnel** | TCP/HTTP (No raw game UDP) | Yes | No | Medium | Incompatible with Bedrock |

#### Selected Tunnel Solution: Playit.gg
- Provides native ARM64 agent executable (`playit-cli`).
- Full support for both UDP (Bedrock RakNet) and TCP (Java).
- Dedicated gaming relay network with global edge points minimizing ping jitter.
- Provides static public hostnames (e.g. `*.joinmc.link`, `*.ply.gg`).

---

## 5. Android Background Execution & Reliability

### 5.1 Foreground Services
- In Android 8.0+ (API 26+) and Android 14+ (API 34+), background process execution is strictly capped.
- MineHost implements `ServerForegroundService` declaring:
  - `android:foregroundServiceType="specialUse|dataSync"`
  - Persistent ongoing notification with interactive controls (Stop Server, Console quick view, player count).
  - Proper notification channels with `IMPORTANCE_LOW` to avoid intrusive audio alerts while maintaining process priority.

### 5.2 Power Management & Doze Mode
- `WakeLockManager`: Acquires a `PowerManager.PARTIAL_WAKE_LOCK` with `setReferenceCounted(false)` while the server process is active.
- `WifiLock`: Acquires `WifiManager.WIFI_MODE_FULL_HIGH_PERF` to prevent the Wi-Fi chipset from entering power-saving latency throttling.
- Prompts user to exempt MineHost from OEM battery optimizations (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`).

### 5.3 Thermal & Memory Throttling
- Integrates `PowerManager.OnThermalStatusChangedListener` (API 29+).
- If device hits `THERMAL_STATUS_SEVERE` or `CRITICAL`, MineHost:
  1. Issues an in-game warning to players via RCON/stdin broadcast (`[MineHost] Device overheating, reducing tick load`).
  2. Offers an optional graceful auto-save & shutdown policy to protect the host device hardware.

---

## 6. Compatibility & System Requirements Matrix

| Component | Minimum | Recommended | Ideal |
| :--- | :--- | :--- | :--- |
| **Android Version** | Android 9.0 (Pie / API 28) | Android 12.0 (API 31) | Android 14+ (API 34) |
| **CPU Architecture** | ARM64 (aarch64 / arm64-v8a) | Snapdragon 7xx / Dimensity 800+ | Snapdragon 8 Gen 1+ |
| **RAM** | 4 GB (Bedrock only) | 6 GB - 8 GB (Bedrock + Java) | 12 GB+ (Cross-play + heavy worlds) |
| **Internal Storage** | 2 GB free | 10 GB free UFS 2.2+ | 32 GB+ free UFS 3.1+ |
| **Network** | 10 Mbps Wi-Fi / 4G LTE | 50 Mbps Wi-Fi 5 / 5G | 100 Mbps Wi-Fi 6 |

---

## 7. Development Risks & Mitigations

1. **Risk:** Android aggressive OOM killer terminating server process when user launches Minecraft client on same device.
   - **Mitigation:** Foreground service with high priority notification, low initial heap configurations (`-Xms256M -Xmx1024M`), and memory warning callbacks (`onTrimMemory`).
2. **Risk:** World corruption during abrupt app kill or battery depletion.
   - **Mitigation:** Continuous LevelDB / Anvil sync triggers, periodic auto-saves, and automatic pre-shutdown backups (`save-all flush`).
3. **Risk:** Relay network disconnection.
   - **Mitigation:** Exponential backoff reconnection in `TunnelManager` that maintains local server uptime while re-establishing public endpoint.

