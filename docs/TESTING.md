# MineHost: Testing Status

This file separates what was **run**, what was **only written**, and what is **not tested**. Earlier versions of this
file listed many items as PASS without a reproducible run; those claims were removed.

## Test levels

| Level | What | Status |
|---|---|---|
| 1. Build | Gradle `assembleDebug`, lint, resources | **NOT TESTED** in the latest audit (no Android SDK or Gradle network in the audit environment). Every Android-dependent edit is **uncompiled**. |
| 2. Unit tests, pure Kotlin | Protocol framing, parsers, backup swap, checksum, tunnel policy | **RUN, 40 of 40 pass** (see below). Run with `kotlinc` + JUnit **outside Gradle**. |
| 3. Instrumented / device | Room migration, service, Compose UI | **NOT TESTED** |
| 4. Real server | PocketMine / Paper starting and answering | **NOT TESTED** (and blocked: no PHP/Java runtime is bundled) |
| 5. Real client | Minecraft client joining on LAN / via tunnel | **NOT TESTED** |
| 6. Tunnel | Real Playit account, claim, external join | **NOT TESTED** |

## Level 2: unit tests that were actually run

Compiled and executed with the Gradle-bundled `kotlinc` and JUnit 4.13.2 (outside Gradle), 40 tests, 0 failures.

| Test class | Tests | Covers | Limit of the evidence |
|---|---|---|---|
| `ReadinessProbeTest` | 8 | RakNet ping/pong framing, Java status VarInt parsing, loopback fake responders | Fakes prove the code, **not** that real PocketMine/Paper answer the same way |
| `CommandSanitizerTest` | 4 | Rejects blank, over-long, control-character commands | n/a |
| `ZipUtilsAndValidationTest` | 4 | Content checksum, tamper detection | n/a |
| `WorldSwapTest` | 4 | Replace-with-rollback logic on a temp directory | Not run against a real LevelDB world |
| `TunnelSupportTest` | 10 | Reachability tracker, reconnect backoff, log redaction, Playit output parser | Parser formats are **assumed**, not captured from the real agent |
| `PlayerTrackerTest` | 6 | Join/leave parsing | Line formats are **assumed**, not captured from a real server |
| `LocalNetworkTest` | 4 | LAN address selection | `detect()` itself returned no address in the audit container |

Source: `app/src/test/java/com/minehost/app/`. The older Gradle-based tests in the repo were not re-run.

## Not tested (every one needs hardware or an account)

- PocketMine-MP start on a device, Bedrock client join on the same Wi-Fi, persistence after restart
- Paper / Java hosting; Geyser cross-play (cross-play is disabled in the app)
- Playit claim flow, public endpoint, external join, reconnect after network loss
- Foreground service survival with the screen locked; Android 14 service types; thermal behaviour
- Room migration 1 to 2 on a device with existing data
- Backup and restore on a real world; release build and signing

See `docs/DEVICE_TESTING.md` for the exact procedure.
