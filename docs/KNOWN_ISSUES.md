# MineHost — Known Issues & Technical Limitations

## 1. Android Platform Constraints

### 1.1 Android 10+ (API 29+) W^X Restrictions
- **Description:** Starting with Android 10, the operating system blocks executing executable binaries from application data directories (`/data/data/<package>/files`).
- **Impact:** Binaries downloaded dynamically into `filesDir` cannot be executed directly via `Runtime.exec()` or `ProcessBuilder` without execute permissions.
- **Mitigation:**
  - Pre-packaged native binaries must be placed in `jniLibs/<abi>/lib<name>.so` and installed with `android:extractNativeLibs="true"`, placing them in `applicationInfo.nativeLibraryDir` with execute permissions.
  - Dynamically updated components utilize `RuntimeManager` with appropriate file permission enforcement or validated userspace loaders.

### 1.2 Android ART vs Desktop Java Virtual Machine
- **Description:** Android's native runtime is ART (Android Runtime), which only executes DEX bytecode. ART cannot directly execute standard Java `.class` or `.jar` files.
- **Impact:** Desktop Java server software (PaperMC, Geyser) requires an actual ARM64 OpenJDK binary distribution (`bin/java` and `libjvm.so`) compiled for Android Bionic or Musl libc.
- **Mitigation:** MineHost isolates Java server execution behind `JavaEngine` and focuses the immediate milestone on native Bedrock hosting via PocketMine-MP.

### 1.3 Memory & Aggressive OEM Battery Killers
- **Description:** Android vendors (Samsung, Xiaomi, Huawei, etc.) aggressively kill background processes even if a Foreground Service is running.
- **Impact:** A server running in the background may be terminated if free RAM drops below the low-memory killer (LMK) threshold, especially if the host plays Minecraft on the same device.
- **Mitigation:**
  - MineHost integrates `ServerForegroundService` declaring `specialUse|dataSync`.
  - Prompts users to exempt MineHost from battery optimization via `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
  - Recommended minimum RAM is 4GB for Bedrock only, 6GB+ if playing on the same device.

### 1.4 Mojang Bedrock Dedicated Server (BDS) ARM64 Absence
- **Description:** Mojang only distributes x86_64 binaries for the official Bedrock Dedicated Server. There are no ARM64 Linux or Android builds.
- **Impact:** Running official BDS on Android ARM64 requires heavy x86 CPU translation (e.g. Box64), which severely impacts battery and performance.
- **Mitigation:** MineHost uses native ARM64 Bedrock server software (PocketMine-MP).

---

## 2. Networking & Relay Constraints

### 2.1 First-Time Playit Agent Claiming
- **Description:** On first launch, the Playit agent generates a unique cryptographic secret and requires a one-time claim on `https://playit.gg/claim/<code`.
- **Mitigation:** MineHost captures this URL directly from agent stdout and presents a one-click "Claim Agent on Playit.gg" button in the Server Detail screen.

---

## 5. Findings from the 2026-11-09 source audit

See [PRODUCTION_READINESS.md](PRODUCTION_READINESS.md) for the full list. Unresolved items:

- **No PHP or Java runtime is bundled.** `libphp.so` / `libjava.so` are expected in `nativeLibraryDir` but the repository has no `jniLibs/`. Bedrock and Java servers cannot start until this is solved.
- **W^X:** executables downloaded into `filesDir` (e.g. the Playit agent) cannot run on Android 10+.
- **Playit agent URL** uses an asset name (`playit-cli-linux-<arch>`) that does not match the official `playit-linux-*` naming; the download likely fails.
- **Downloads are not integrity-verified** against any trusted checksum, and use `releases/latest`.
- **EULA consent UI is missing.** `eula.txt` is written as `eula=false`, so Java servers refuse to start until consent exists.
- **Cross-play is not implemented** (now reported as unsupported instead of falling back to Bedrock-only).
- **ConnectionMonitor uses a TCP connect**, which cannot monitor a UDP (Bedrock) tunnel.
- **Backup creation while a server is running** is not made consistent (no `save-off`/`save-all`).
- **Diagnostics swallows download failures**, so install errors are invisible to the user.
- **Release builds are signed with the debug key.**
- **Stopping while STARTING:** `stopServer` returns success without effect until the process handle exists.
