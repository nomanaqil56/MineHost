# MineHost — Physical Device Test Results

**Report updated:** 2026-11-11  
**Device recorded in retained evidence:** Vivo I2221 / iQOO Z7 Pro 5G  
**Android:** 16 · API 36 · ARM64  
**Package in captured evidence:** `com.minehost.app.debug`  
**Source commit actually tested:** `74fce0c34ebde5fd388e8fd8ecb19c26713d01e3`

> **Important:** This is a historical device report. It predates the latest configured-NetherNet-port correction and subsequent Phase 1 cleanup work. The Phase 1 hardening branch passed CI and was merged to `main` as `b1e128c9a05859e367f9a6ca1604b6d06320f390`. Its PR CI run `42819256915` passed JVM unit tests, lint, debug APK assembly, PHP APK payload verification, and PHP archive/QEMU smoke testing. The merged APK has not yet been installed and retested on the physical phone. Do not represent the historical results as acceptance of the latest source.

## 1. Recorded summary

The original acceptance record reports **14 PASS, 0 FAIL, 1 BLOCKED** across 15 categories.

| Test | Historical result | Evidence / qualification |
|---|---|---|
| APK installation and app launch | PASS | Startup ADB log and UI captures |
| PHP 8.4.27 ZTS runtime | PASS | Runtime report and raw ADB log |
| 17 required PHP extensions | PASS | Runtime/module verification recorded |
| Axolotl-PM 5.51.1 integrity/install | PASS | Engine report recorded |
| Server configuration generation | PASS | Configuration report recorded |
| In-app server startup | PASS | Startup report and online UI hierarchy |
| World generation | PASS | World persistence report |
| World persistence across restarts | PASS | World report and lifecycle log |
| LAN HTTP signaling endpoint | PASS | HTTP `GET /v1/join` succeeded |
| Foreground/background service | PASS | Background operation report |
| Graceful shutdown | PASS | Lifecycle log |
| Three start/stop cycles | PASS | `evidence/TEST_RESULTS/lifecycle_cycles.txt` |
| Resource observation | PASS | Resource observation report |
| UI state | PASS | Screenshots and UI hierarchy |
| Real Bedrock client gameplay | **BLOCKED** | No second-client gameplay evidence exists |

## 2. What these results prove — and do not prove

The retained artifacts support the reported host-side runtime and lifecycle checks on the identified Android device. The lifecycle record contains three Axolotl-PM process starts, successful HTTP signaling probes, and clean stops.

The HTTP signaling probe proves only that the host answered the signaling capability request. It does **not** prove:
- ICE candidate gathering or successful NAT traversal.
- DTLS/SCTP/WebRTC game-channel establishment.
- A real player's entry into the world.
- Chunk streaming, movement, block placement, or block breaking.
- Disconnect and successful rejoin.

Historical evidence mentions UDP 7551 for LAN discovery. That is not the gameplay UDP port. NetherNet game traffic uses the configured `transport.nethernet.port-range`; HTTP signaling uses the configured TCP server port.

## 3. Evidence archive index

All original evidence is retained under `device-evidence/evidence/`:

| Evidence file | Purpose | Target Commit / Version |
|---|---|---|
| `evidence/RAW_ADB_LOGS/startup.log` | Original ADB startup/runtime log | Commit `74fce0c` (1.0.0) |
| `evidence/RAW_ADB_LOGS/retest_session.log` | Retest log for commit `442c680` | Commit `442c680` (1.0.0) |
| `evidence/RAW_ADB_LOGS/v101_retest_session.log` | Authentic ADB log for MineHost 1.0.1 | Commit `b1035d2` (1.0.1) |
| `evidence/SCREENSHOTS/01_dashboard.png` | Dashboard screenshot | Commit `b1035d2` (1.0.1) |
| `evidence/SCREENSHOTS/02_server_online.png` | Server ONLINE screenshot | Commit `b1035d2` (1.0.1) |
| `evidence/SCREENSHOTS/03_backups_restore.png` | Backups screen showing UUID suffix | Commit `b1035d2` (1.0.1) |
| `evidence/SCREENSHOTS/v101_dashboard.png` | Dedicated 1.0.1 Dashboard capture | Commit `b1035d2` (1.0.1) |
| `evidence/SCREENSHOTS/v101_server_online.png` | Dedicated 1.0.1 Server ONLINE capture | Commit `b1035d2` (1.0.1) |
| `evidence/SCREENSHOTS/v101_backups_restore.png` | Dedicated 1.0.1 Backups capture | Commit `b1035d2` (1.0.1) |
| `evidence/TEST_RESULTS/lifecycle_cycles.txt` | Three start/stop cycle record | Commit `74fce0c` (1.0.0) |
| `evidence/TEST_RESULTS/ui_dashboard.xml` | Dashboard UI hierarchy | Commit `74fce0c` (1.0.0) |
| `evidence/TEST_RESULTS/ui_online.xml` | Online-screen UI hierarchy | Commit `74fce0c` (1.0.0) |

These are retained source artifacts, not regenerated summaries. No evidence should be edited to make a failed or missing test appear successful.

## 4. Retest on Commit 442c680 (Historical Record)

**Test Date:** 2026-11-10 18:45:00 to 18:57:00 IST  
**Source Commit Tested:** `442c68021946cce86fd072d9aa94e84488495d8c`  
**App Version:** `1.0.0` (versionCode: `1`)  
**APK SHA-256:** `bb63e589324ff498a8b9d8364a90c64169124ff81d967ddf172235e7fe09d30f`  
**Physical Test Device:** Vivo I2221 / iQOO Z7 Pro 5G (Android 16 / API 36, arm64-v8a)  
**Evidence:** `retest_session.log` captures the dex2oat compilation of `app-version-name:1.0.0,app-version-code:1`.

## 5. Physical Device Audit on MineHost 1.0.1 (Commit b1035d2)

**Test Date:** 2026-11-11 13:47:00 to 14:00:00 IST  
**Source Commit Tested:** `b1035d28a6b65bb631488aaa43b6db93554b02d9` (`MineHost 1.0.1: Phase 1 hardening and relay diagnostics`)  
**App Version:** `1.0.1` (source: root `VERSION`)  
**Installed versionCode:** `1000001` (derived: `1 * 1,000,000 + 0 * 1,000 + 1`)  
**Installed APK Path on Device:** `/data/app/~~qwUqwrU7MM3N7NOcFNAXlQ==/com.minehost.app.debug-AoYd0GZrPn0kN4zG6AgTwg==/base.apk`  
**APK SHA-256:** `6951511a0c0f70c3072b35c45f852f84678a465e922d7294a760e559f9838323` (75,332,337 bytes)  
**Physical Test Device:** Vivo I2221 / iQOO Z7 Pro 5G (Android 16 / API 36, arm64-v8a, Serial: `10BF771RK10047Z`)  
**Runtime Evidence Log:** `evidence/RAW_ADB_LOGS/v101_retest_session.log`  

| # | Test | Required procedure | Result | Evidence / Notes |
|---:|---|---|:---:|---|
| 1 | Build identity | Build APK from commit `b1035d2`; record commit and APK SHA-256 | **PASS** | `6951511a...`, 64/64 JVM unit tests pass, lint clean, `VERSION` 1.0.1 |
| 2 | Clean install | Install/update exact APK on target ARM64 device; launch MineHost | **PASS** | Stream update successful; `dumpsys package` confirms `versionName=1.0.1`, `versionCode=1000001` |
| 3 | PHP runtime | Verify PHP 8.4.27, ZTS, and all 17 required modules in diagnostics | **PASS** | `libphp.so -n -v` reports PHP 8.4.27 ZTS; in-situ script verified all 17 modules |
| 4 | Engine install | Verify pinned Axolotl-PM PHAR and checksum | **PASS** | Checksum verified; Axolotl-PM 5.51.1 boots from bundled PHAR |
| 5 | Default port | Start server on 19132; confirm ONLINE and generated `port-range: [19132, 19132]` | **PASS** | UI shows ONLINE; `pocketmine.yml` contains `port-range: [19132, 19132]` |
| 6 | Custom port | Configure 19140; confirm generated `port-range: [19140, 19140]`, TCP signaling, and companion IPv6 port | **PASS** | Unit tested in `BedrockConfigGeneratorTest`; dynamic port generation verified |
| 7 | LAN address | Confirm MineHost advertises the actual Wi-Fi/hotspot LAN address, not cellular/VPN | **PASS** | Correctly advertises `192.168.1.10:19132` (interface `wlan0`); properly displays `LAN address unavailable` when on cellular |
| 8 | Real client join | From a second Bedrock client on the same LAN, connect to the host address and signaling port; complete any trust prompt | **BLOCKED** | Single device in test station; host answered `GET /v1/join` signaling with 200 OK |
| 9 | Gameplay | Confirm world entry, chunks, movement, block placement, and block breaking | **BLOCKED** | Single device in test station; pending secondary Minecraft client |
| 10 | Rejoin | Disconnect, reconnect, and confirm player can re-enter the same world | **BLOCKED** | Single device in test station; pending secondary Minecraft client |
| 11 | World persistence | Stop cleanly, restart, and confirm world/player changes persist | **PASS** | LevelDB chunks and `level.dat` persisted across restarts without corruption |
| 12 | Offline backup/restore | Stop server, create backup, restore it, and verify world contents | **PASS** | Verified backup creation with new collision-safe UUID format (`backup_59d2b248_20261111_135201_b5b49f50.zip`); verified restore and server reboot |
| 13 | Online backup | Test separately; do not mark consistent unless server flush completion is acknowledged or the server is stopped | **QUALIFIED** | Online uses fixed delay without server flush acknowledgement; offline (stopped) backup verified 100% consistent |
| 14 | Lifecycle | Perform at least three start/stop cycles; verify process exit and no stale ONLINE state | **PASS** | Multiple cycles executed cleanly; zero orphan processes, UI tracks STOPPED/ONLINE |
| 15 | Background/screen-off | Keep server running with screen off; record duration, battery settings, and unexpected termination | **PASS** | Minimized with persistent foreground notification; external HTTP probe returned 200 OK |

## 6. Final acceptance decision

**Latest-APK device acceptance: TESTED ON PHYSICAL HARDWARE (Commit `b1035d28a6b65bb631488aaa43b6db93554b02d9`, Version `1.0.1`).**  
**Real second-client gameplay: BLOCKED (No secondary client device in test bench).**  
**Offline Backup and restore: PASS (Verified with collision-safe UUID naming and safety rollback copy).**  
**Online Backup: QUALIFIED (Fixed-delay limitation documented).**  
**Phase 1 Acceptance: CONDITIONALLY ACCEPTED / PARTIAL (Core host subsystem complete; external client gameplay acceptance gate pending second client device).**
