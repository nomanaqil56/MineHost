# MineHost — Physical Device Test Results

**Report updated:** 2026-11-10  
**Device recorded in retained evidence:** Vivo I2221 / iQOO Z7 Pro 5G  
**Android:** 16 · API 36 · ARM64  
**Package in captured evidence:** `com.minehost.app.debug`  
**Source commit actually tested:** `74fce0c34ebde5fd388e8fd8ecb19c26713d01e3`

> **Important:** This is a historical device report. It predates the latest configured-NetherNet-port correction and subsequent Phase 1 cleanup work. The latest branch APK has not yet been installed and retested on the physical phone. Do not represent the historical results as acceptance of the latest source.

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

| Evidence file | Purpose |
|---|---|
| `evidence/RAW_ADB_LOGS/startup.log` | Original ADB startup/runtime log |
| `evidence/SCREENSHOTS/01_dashboard.png` | Dashboard screenshot |
| `evidence/SCREENSHOTS/02_server_online.png` | Server ONLINE screenshot |
| `evidence/TEST_RESULTS/lifecycle_cycles.txt` | Three start/stop cycle record |
| `evidence/TEST_RESULTS/ui_dashboard.xml` | Dashboard UI hierarchy |
| `evidence/TEST_RESULTS/ui_online.xml` | Online-screen UI hierarchy |

These are retained source artifacts, not regenerated summaries. No evidence should be edited to make a failed or missing test appear successful.

## 4. Required retest on the latest Phase 1 APK

Record the exact APK SHA-256, source commit, device model, Android version, test date/time, and result for every retest.

| # | Test | Required procedure | Result |
|---:|---|---|---|
| 1 | Build identity | Build APK from final Phase 1 commit; record commit and APK SHA-256 | PENDING |
| 2 | Clean install | Install/update exact APK on target ARM64 device; launch MineHost | PENDING |
| 3 | PHP runtime | Verify PHP 8.4.27, ZTS, and all 17 required modules in diagnostics | PENDING |
| 4 | Engine install | Verify pinned Axolotl-PM PHAR and checksum | PENDING |
| 5 | Default port | Start server on 19132; confirm ONLINE and generated `port-range: [19132, 19132]` | PENDING |
| 6 | Custom port | Configure 19140; confirm generated `port-range: [19140, 19140]`, TCP signaling, and companion IPv6 port | PENDING |
| 7 | LAN address | Confirm MineHost advertises the actual Wi-Fi/hotspot LAN address, not cellular/VPN | PENDING |
| 8 | Real client join | From a second Bedrock client on the same LAN, connect to the host address and signaling port; complete any trust prompt | PENDING |
| 9 | Gameplay | Confirm world entry, chunks, movement, block placement, and block breaking | PENDING |
| 10 | Rejoin | Disconnect, reconnect, and confirm player can re-enter the same world | PENDING |
| 11 | World persistence | Stop cleanly, restart, and confirm world/player changes persist | PENDING |
| 12 | Offline backup/restore | Stop server, create backup, restore it, and verify world contents | PENDING |
| 13 | Online backup | Test separately; do not mark consistent unless server flush completion is acknowledged or the server is stopped | PENDING |
| 14 | Lifecycle | Perform at least three start/stop cycles; verify process exit and no stale ONLINE state | PENDING |
| 15 | Background/screen-off | Keep server running with screen off; record duration, battery settings, and unexpected termination | PENDING |

## 5. Final acceptance decision

**Latest-APK device acceptance: NOT RUN.**  
**Real second-client gameplay: BLOCKED / NOT VERIFIED.**  
**Backup/restore acceptance: PENDING.**  
**Phase 1: NOT COMPLETE.**

Only change a PENDING/BLOCKED result to PASS after performing the test on the physical device and adding supporting evidence to the evidence archive. Keep failures and limitations visible in this report.
