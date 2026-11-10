# MineHost — Known Issues & Technical Limitations

> **Main-branch update (2026-11-10):** Runtime detection has been hardened. MineHost no longer treats `libphp.so`, `libjava.so`, or `libplayit.so` as executable CLI programs. The PHP and Java runtime launch paths remain blocked until complete compatible runtimes are supplied and verified. The new Android CI workflow has not yet produced a confirmed successful run.

## 1. Android Platform Constraints

### 1.1 Android 10+ (API 29+) W^X Restrictions
- **Description:** Starting with Android 10, the operating system blocks executing executable binaries from application data directories (`/data/data/<package>/files`).
- **Impact:** Binaries downloaded dynamically into `filesDir` cannot be executed directly via `Runtime.exec()` or `ProcessBuilder` without execute permissions.
- **Mitigation:** Do not assume that `jniLibs` turns a shared library into a standalone CLI program. JNI libraries are loaded into the app process; server runtimes and helper agents need a separately validated launch strategy. A downloaded file's executable bit is not proof that Android/SELinux will permit execution. Test the exact packaging/loader approach on each supported Android API level.

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

## 5. Findings from the 2026-11-09 source audit (historical; see current-main update below)

See [PRODUCTION_READINESS.md](PRODUCTION_READINESS.md) for the full list. Unresolved items:

- **PHP and Java runtimes are not bundled.** The current main code requires executable PHP and Java CLI launchers under the managed runtime directories. The repo still has no validated Android ARM64 runtime package; Bedrock and Java remain blocked.
- **Android executable policy is unresolved.** The downloaded Playit agent may be rejected by Android/SELinux even when its executable bit is set; a real device launch test is required.
- **Playit is not the final network architecture.** Asset resolution now uses release metadata, but Android compatibility and an external client join are unverified. MineHost-owned relay/control-plane infrastructure is not implemented.
- **Download integrity and version policy need final audit.** Published SHA-256 digests are compared when available; version pinning and provenance policy still need review.
- **Cross-play is not operational.** Geyser/Floodgate runtime integration is blocked by the Java runtime.
- **ConnectionMonitor transport handling** must be verified separately for TCP Java and UDP Bedrock.
- **Backup consistency** needs real-server verification even where save/flush commands are issued.
- **Release signing** now reads owner-controlled environment variables or `local.properties`; without them, release output is unsigned. No production signing key is committed.
- **Build/device status:** the new CI workflow has not yet produced a confirmed successful run; no physical-device or real-client test is recorded.
