# MineHost — Physical Device Test Results

**Test date recorded:** 2026-11-10  
**Device:** Vivo I2221 / iQOO Z7 Pro 5G  
**Android:** 16 · API 36 · ARM64  
**Application package in captured evidence:** `com.minehost.app.debug`  
**Original tested source commit:** `74fce0c34ebde5fd388e8fd8ecb19c26713d01e3`

> The evidence predates the configured-port correction in the latest commit. It does not prove that the latest APK has been installed or tested.

## Summary

The original acceptance report records **14 PASS, 0 FAIL, 1 BLOCKED** across 15 categories.

| Test | Recorded outcome | Qualification |
|---|---|---|
| APK installation and application launch | PASS | Original report and UI captures |
| PHP 8.4.27 ZTS runtime | PASS | Runtime report and raw ADB log |
| 17 required PHP extensions | PASS | Extension report |
| Axolotl-PM 5.51.1 integrity/install | PASS | Engine report |
| Configuration generation | PASS | Configuration report |
| In-app server startup | PASS | Startup report and online UI hierarchy |
| World generation | PASS | World persistence report |
| World persistence across restarts | PASS | World report and lifecycle log |
| LAN signaling | PASS | HTTP `GET /v1/join` succeeded |
| Background service | PASS | Background report |
| Graceful shutdown | PASS | Lifecycle log |
| Three start/stop cycles | PASS | `evidence/TEST_RESULTS/lifecycle_cycles.txt` |
| Resource observation | PASS | Resource report |
| UI state | PASS | Screenshots and UI hierarchy |
| Real Bedrock client gameplay | **BLOCKED** | No second client; no real gameplay recorded |

## What the evidence establishes

The retained artifacts support the reported host-side runtime and lifecycle results on the identified device. The lifecycle log records three Axolotl-PM process starts, successful NetherNet HTTP signaling probes, and clean stops.

The HTTP signaling probe is only a host readiness check. It does **not** prove ICE candidate gathering, DTLS/SCTP setup, world entry, chunk streaming, or block interaction. Real multiplayer acceptance remains blocked.

The historical report mentions UDP 7551 for LAN discovery. That is not the gameplay UDP port. NetherNet game traffic is governed by `transport.nethernet.port-range`; signaling uses TCP `server-port`.

## Retained evidence

- `evidence/RAW_ADB_LOGS/startup.log` — original ADB log.
- `evidence/SCREENSHOTS/01_dashboard.png` — dashboard capture.
- `evidence/SCREENSHOTS/02_server_online.png` — online server capture.
- `evidence/TEST_RESULTS/lifecycle_cycles.txt` — three-cycle process/signaling record.
- `evidence/TEST_RESULTS/ui_dashboard.xml` — dashboard UI hierarchy.
- `evidence/TEST_RESULTS/ui_online.xml` — online server UI hierarchy.

Individual text reports were consolidated here. Distinct raw evidence was retained; duplicate XML snapshots and duplicate report formats were removed.

## Current-commit re-test checklist

1. Install the APK built from the current main commit on the target ARM64 phone.
2. Verify PHP version, ZTS, and all 17 required modules from MineHost diagnostics.
3. Start the Bedrock server with default port 19132 and confirm ONLINE.
4. Confirm generated `pocketmine.yml` contains `port-range: [19132, 19132]`.
5. Change the server port to a non-default value (for example 19140), start it, and confirm `port-range: [19140, 19140]`; verify TCP signaling and the IPv6 companion port.
6. From a second Bedrock client on the same LAN, add the host LAN IP and configured signaling port. Accept the trust prompt if shown.
7. Verify world entry, chunk loading, movement, block placement/breaking, disconnect, and rejoin.
8. Create a backup from a stopped server; restore it and verify the world. Test online backup separately.
9. Stop cleanly and verify process exit; repeat at least three cycles.
10. Test screen-off/background operation and record Android battery-management settings.

**Final device acceptance: PENDING.** Do not mark PASS until second-client gameplay and live backup/restore have actually completed.
