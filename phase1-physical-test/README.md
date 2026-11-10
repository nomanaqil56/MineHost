# MineHost Phase 1 Physical Device Testing Evidence

This directory contains the complete, evidence-backed test artifact suite for the acceptance testing of **MineHost Phase 1 (Android Bedrock Server Hosting)** performed on a physical Android 16 device.

## Test Environment Summary

- **Device:** Vivo I2221 / iQOO Z7 Pro 5G (`arm64-v8a`)
- **OS:** Android 16 (API 36, Baklava)
- **Repository:** `nomanaqil56/MineHost`
- **Tested Commit:** `74fce0c34ebde5fd388e8fd8ecb19c26713d01e3`
- **Application Package:** `com.minehost.app.debug`
- **Bedrock Engine:** Axolotl-PM 5.51.1 (`PocketMine-MP.phar`)
- **Runtime:** `libphp.so` (PHP 8.4.27 ZTS ARM64)

---

## Directory Contents

| File / Folder | Description |
|---|---|
| [PHASE1_ACCEPTANCE_REPORT.md](file:///D:/Coding/Projects/Personal%20Projects/MineHost/phase1-physical-test/PHASE1_ACCEPTANCE_REPORT.md) | **Primary Acceptance Report** detailing all 15 audit sections, results, performance metrics, and the acceptance decision. |
| [DEVICE_INFORMATION.txt](file:///D:/Coding/Projects/Personal%20Projects/MineHost/phase1-physical-test/DEVICE_INFORMATION.txt) | Detailed physical device specifications (model, build, CPU ABI, display, RAM, storage, network interfaces). |
| [BUILD_INFORMATION.txt](file:///D:/Coding/Projects/Personal%20Projects/MineHost/phase1-physical-test/BUILD_INFORMATION.txt) | Git commit, branch, build timestamp, Gradle task outputs, and APK SHA-256 fingerprint. |
| [PHP_RUNTIME_REPORT.txt](file:///D:/Coding/Projects/Personal%20Projects/MineHost/phase1-physical-test/PHP_RUNTIME_REPORT.txt) | Verification of `libphp.so` (ELF header, AArch64 architecture, Thread Safety ZTS, CLI execution with `-n`). |
| [PHP_EXTENSION_REPORT.txt](file:///D:/Coding/Projects/Personal%20Projects/MineHost/phase1-physical-test/PHP_EXTENSION_REPORT.txt) | In-situ test results verifying all 17 required PHP extensions loaded and functional. |
| [ENGINE_INSTALLATION_REPORT.txt](file:///D:/Coding/Projects/Personal%20Projects/MineHost/phase1-physical-test/ENGINE_INSTALLATION_REPORT.txt) | Extraction and SHA-256 verification of `PocketMine-MP.phar`, and engine version banner test. |
| [CONFIGURATION_VALIDATION_REPORT.txt](file:///D:/Coding/Projects/Personal%20Projects/MineHost/phase1-physical-test/CONFIGURATION_VALIDATION_REPORT.txt) | Validation of `server.properties` and `pocketmine.yml` generation, port bindings, and NetherNet transport block. |
| [SERVER_STARTUP_REPORT.txt](file:///D:/Coding/Projects/Personal%20Projects/MineHost/phase1-physical-test/SERVER_STARTUP_REPORT.txt) | Interactive in-app server start logs, process PID tracking, and socket binding confirmation. |
| [WORLD_PERSISTENCE_REPORT.txt](file:///D:/Coding/Projects/Personal%20Projects/MineHost/phase1-physical-test/WORLD_PERSISTENCE_REPORT.txt) | Audit of generated LevelDB chunks (`000005.ldb`), `level.dat` NBT tags, and persistence across shutdowns. |
| [LAN_NETWORK_REPORT.txt](file:///D:/Coding/Projects/Personal%20Projects/MineHost/phase1-physical-test/LAN_NETWORK_REPORT.txt) | IP detection (`192.168.1.10:19132`) and external workstation probe of NetherNet HTTP join signaling. |
| [MULTIPLAYER_TEST_REPORT.txt](file:///D:/Coding/Projects/Personal%20Projects/MineHost/phase1-physical-test/MULTIPLAYER_TEST_REPORT.txt) | Multiplayer test status (Blocked pending secondary client device), documenting host readiness. |
| [BACKGROUND_SERVICE_REPORT.txt](file:///D:/Coding/Projects/Personal%20Projects/MineHost/phase1-physical-test/BACKGROUND_SERVICE_REPORT.txt) | Verification of `ServerForegroundService` notification, wake lock, and persistence when minimized. |
| [RESOURCE_OBSERVATION_REPORT.txt](file:///D:/Coding/Projects/Personal%20Projects/MineHost/phase1-physical-test/RESOURCE_OBSERVATION_REPORT.txt) | RAM (PSS/RSS), CPU load, battery temperature, and thermal stability metrics. |
| `RAW_ADB_LOGS/` | Full raw ADB logcat captures and process dumps. |
| `SCREENSHOTS/` | Physical device UI screenshots captured via ADB (`ui_detail_screen.png`). |
| `TEST_RESULTS/` | Automated lifecycle test output (`lifecycle_cycles.txt`). |

---

## Acceptance Summary

- **Total Acceptance Tests:** 15
- **Passed:** 14
- **Failed:** 0
- **Blocked:** 1 (Test H: External client player join, blocked due to single physical device)
- **Phase 1 Decision:** **CONDITIONALLY ACCEPTED**

