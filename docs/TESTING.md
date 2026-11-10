# MineHost: Testing Status

**Main-branch update (2026-11-10):** GitHub Actions run [42763475505](https://github.com/nomanaqil56/MineHost/actions/runs/42763475505) completed successfully on `eeef4a9`. Gradle unit tests, Android lint, debug APK assembly, APK payload assertions (`lib/arm64-v8a/libphp.so` and `assets/php/php.ini`), and the pinned PHP archive/QEMU smoke test all passed.

This file separates what was **run**, what was **only written**, and what is **not tested**. Earlier versions of this
file listed many items as PASS without a reproducible run; those claims were removed.

## Test levels

| Level | What | Status |
|---|---|---|
| 1. Build | Gradle `assembleDebug`, lint, resources | **PASS in GitHub Actions run 42763475505** on commit `eeef4a9`. This is a CI build, not a physical-device test. |
| 2. Unit tests | Gradle `testDebugUnitTest` | **PASS in GitHub Actions run 42763475505**. The workflow ran the repository's Gradle test task; the older manually compiled 40-test tally below is historical. |
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

Source: `app/src/test/java/com/minehost/app/`. The Gradle-based test suite is now run by the workflow on every push to `main` and pull request targeting `main`.

## Not tested (every one needs hardware or an account)

- PocketMine-MP start on a physical device, Bedrock client join on the same Wi-Fi, persistence after restart
- Paper / Java hosting; Geyser cross-play (cross-play is disabled in the app)
- Playit claim flow, public endpoint, external join, reconnect after network loss
- Foreground service survival with the screen locked; Android 14 service types; thermal behaviour
- Room migration 1 to 2 on a device with existing data
- Backup and restore on a real world; release build and signing

See `docs/DEVICE_TESTING.md` for the exact procedure.
