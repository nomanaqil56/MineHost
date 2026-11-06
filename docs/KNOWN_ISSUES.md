# MineHost — Known Issues & Technical Limitations

## 1. Android Platform Constraints

### 1.1 Android 10+ (API 29+) W^X Restrictions
- **Description:** Starting with Android 10, the operating system blocks executing executable binaries from application data directories (`/data/data/<package>/files`).
- **Impact:** Binaries downloaded dynamically into `filesDir` cannot be executed directly via `Runtime.exec()` or `ProcessBuilder`.
- **Mitigation:**
  - Binaries distributed with the app must be packaged inside `jniLibs/<abi>/lib<name>.so` and installed with `android:extractNativeLibs="true"`, placing them in `applicationInfo.nativeLibraryDir` with execute permissions.
  - Dynamically updated components utilize modular interpretative runtimes or shared library loading.

### 1.2 Android ART vs Desktop Java Virtual Machine
- **Description:** Android's native runtime is ART (Android Runtime), which only executes DEX bytecode. ART cannot directly execute standard Java `.class` or `.jar` files.
- **Impact:** Desktop Java server software (PaperMC, PowerNukkitX, Geyser) requires a real ARM64 OpenJDK binary distribution (`bin/java` and `libjvm.so`).
- **Mitigation:** MineHost integrates an embedded JRE 21 manager designed for ARM64 Android with custom system-call compatibility patches.

### 1.3 Memory & Aggressive OEM Battery Killers
- **Description:** Android vendors (Samsung, Xiaomi, Huawei, etc.) aggressively kill background processes even if a Foreground Service is running.
- **Impact:** A server running in the background may be terminated if free RAM drops below the low-memory killer (LMK) threshold, especially if the host plays Minecraft on the same device.
- **Mitigation:**
  - MineHost actively monitors `ComponentCallbacks2.onTrimMemory()` and Android Thermal status.
  - The app guides users through disabling battery optimization via `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
  - Recommended minimum RAM is 6 GB for running both host server and Minecraft client simultaneously.

### 1.4 Bedrock Server Engine Vanilla Parity
- **Description:** PocketMine-MP is extremely lightweight and fast, but lacks native vanilla mobs, AI, and redstone mechanics without community plugins. PowerNukkitX has full parity but higher memory requirements.
- **Impact:** Users must choose the engine matching their device capability.
- **Mitigation:** The server creation wizard clearly indicates these trade-offs and guides low-spec vs high-spec device owners.

---

## 2. Networking & Relay Constraints

### 2.1 Cellular NAT & Ping Latency
- **Description:** When hosting on mobile 4G/5G connections, network jitter and carrier routing can introduce 30ms - 80ms added latency through relay nodes.
- **Mitigation:** The connection monitor dynamically tests ping times and recommends hosting over 5GHz Wi-Fi for competitive multiplayer sessions.

