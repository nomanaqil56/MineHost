# MineHost — Testing Status

This document distinguishes source-level test coverage from executed evidence. **A test class existing in the repository does not mean it passed.** The current PR branch has not yet had a verified Gradle/CI run.

## Test levels

| Level | What | Current status |
|---|---|---|
| 1. Build | Gradle `assembleDebug`, lint, resources | **PENDING CI**. Workflow added; inspect the actual run result. |
| 2. Unit tests | Protocol framing, parsers, checksum, backup swap, tunnel policy | **SOURCE PRESENT; current branch not rerun**. |
| 3. Instrumented/device | Room migration, foreground service, Compose UI, native PHP execution | **NOT TESTED** |
| 4. Real server | Axolotl-PM / Paper starts and answers | **NOT TESTED** |
| 5. Real client | Bedrock/Java client joins LAN/public endpoint | **NOT TESTED** |
| 6. Public connectivity | MineHost-owned TCP edge, HTTPS signalling, TURN | **NOT IMPLEMENTED / NOT TESTED** |

## Unit-test source inventory

| Test class | Source test count | Covers | Evidence limitation |
|---|---:|---|---|
| `ReadinessProbeTest` | 10 | NetherNet HTTP capability response, legacy RakNet framing, Java status VarInt and loopback responders | Fake responders do not prove Axolotl-PM/Paper behaviour or WebRTC gameplay. |
| `CommandSanitizerTest` | 4 | Rejects blank, over-long and control-character commands | Does not prove shell/process isolation beyond the tested helper. |
| `ZipUtilsAndValidationTest` | 6 | Content checksum and tamper detection | Does not prove real LevelDB/world safety. |
| `WorldSwapTest` | 4 | Replace-with-rollback logic on temporary directories | Not tested against an active Minecraft world. |
| `TunnelSupportTest` | 10 | Reachability tracker, reconnect backoff, log redaction and Playit output parser | Parser assumptions are not validated against a live provider. |
| `PlayerTrackerTest` | 6 | Join/leave parsing | Formats are not captured from the selected engine version. |
| `LocalNetworkTest` | 4 | LAN address selection | Does not prove real LAN discovery/client join. |

A previous project note reported 40 of 40 pure-Kotlin tests passing through a standalone compiler/JUnit run. That historical result has not been reproduced on the current PR branch; two NetherNet probe tests and two ZIP identity-metadata tests have since been added. Do not report a current passing count until CI or a reproducible local run records it.

## Required real-world tests

- PHP 8.4 Android ARM64 executable launches from APK `nativeLibraryDir`.
- PHP `-m` lists `webrtc` (ext-webrtc).
- Axolotl-PM 5.51.1 starts with `transport.name: nethernet`.
- Bedrock 1.26.60 client joins on the same Wi-Fi.
- NetherNet UDP ICE/DTLS/SCTP gameplay succeeds, not just HTTP `GET /v1/join`.
- World persists after graceful stop/restart; `nethernet.key` persists and is backed up.
- Paper/JVM and Geyser cross-play on physical Android (separate milestone).
- MineHost-owned TCP reverse tunnel exposes Java TCP and Bedrock HTTPS signalling through CGNAT.
- MineHost-owned TURN provides Bedrock UDP fallback when direct ICE fails.
- Wi-Fi-to-cellular transition triggers endpoint/tunnel/ICE recovery and honest player reconnection.
- Foreground service survives screen-off and sustained workloads on multiple OEM devices.
- Thermal throttling, memory pressure, storage exhaustion, crash recovery and backup restore.
- International client joins with p50/p95 RTT, jitter, loss, TPS/MSPT and relay bandwidth recorded.

See [DEVICE_TESTING.md](DEVICE_TESTING.md) for the exact acceptance procedure and report template.
