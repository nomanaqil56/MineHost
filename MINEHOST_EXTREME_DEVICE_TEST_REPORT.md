# MINEHOST — EXTREME PHYSICAL DEVICE TEST REPORT

**Execution Timestamp:** 2026-11-10 15:07:04 IST  
**Target Repository:** [`nomanaqil56/MineHost`](https://github.com/nomanaqil56/MineHost)  
**Branch:** `main`  
**Latest Base Commit:** `541a2c8` (`fix: retain atomic runtime download imports`)  
**Overall Result:** **100% SUCCESS — 21 / 21 Hardware Smoke & Integration Tests Passing**

---

## 1. Connected Hardware Target Profile

The test suite executed directly on a connected physical Android device via ADB stream transport:

| Attribute | Detected Value | Verification Method | Status |
| :--- | :--- | :--- | :---: |
| **Manufacturer** | `vivo` | `getprop ro.product.manufacturer` | **CONFIRMED** |
| **Model** | `I2221` (iQOO Z7 Pro 5G) | `getprop ro.product.model` | **CONFIRMED** |
| **Android Version** | **Android 16** | `getprop ro.build.version.release` | **CONFIRMED** |
| **API Level** | **API 36** (minSdk=28, targetSdk=34) | `getprop ro.build.version.sdk` | **CONFIRMED** |
| **CPU Architecture** | **arm64-v8a** (AArch64) | `getprop ro.product.cpu.abi` | **CONFIRMED** |
| **Total System RAM** | `7,473,032 kB` (~7.47 GB) | `/proc/meminfo` | **CONFIRMED** |
| **Device Serial** | `10BF771RK10047Z` | ADB device transport | **CONFIRMED** |

---

## 2. Package Installation & Native Binary Verification

1. **Clean Installation**:
   - Package `com.minehost.app.debug` cleanly uninstalled prior to testing.
   - APK `app/build/outputs/apk/debug/app-debug.apk` (71.94 MB, SHA-256: `97016b3f15068a94987242c21b85976444bfe98b7496e7702b018b7df27a2717`) streamed and installed in **4.90s**.
2. **System Permissions & Power Policy**:
   - `android.permission.POST_NOTIFICATIONS` granted for foreground service notification display.
   - Device idle battery optimization whitelist applied (`dumpsys deviceidle whitelist +com.minehost.app.debug`).
3. **Native PHP Executable Conformance**:
   - Native library located at: `/data/app/.../com.minehost.app.debug-.../lib/arm64/libphp.so` (Permissions: `-rwxr-xr-x`, 34,660,760 bytes).
   - Validated ELF 64-bit Little-Endian AArch64 binary header.

---

## 3. Native PHP 8.4 Runtime Audit (`php -n`)

Executed direct binary invocation with the `-n` flag (no external `php.ini`):

- **Binary Version Output:**  
  `PHP 8.4.27 (cli) (built: Oct 21 2026 19:23:26) (ZTS)`
- **All 17 Required Axolotl-PM Extension Modules Verified Present:**
  1. `curl` (HTTP transport)
  2. `gmp` (Arbitrary precision arithmetic)
  3. `igbinary` (Fast binary serializer)
  4. `leveldb` (World chunk database)
  5. `mbstring` (Multibyte string support)
  6. `openssl` (Encryption / TLS)
  7. `phar` (PHAR archive extraction & stream wrapper)
  8. `sockets` (UDP / RakNet socket operations)
  9. `yaml` (YAML configuration parser)
  10. `zip` (ZIP world backup compression)
  11. `zlib` (Deflate compression)
  12. `chunkutils2` (PocketMine chunk data manipulations)
  13. `crypto` (Native hashing & ciphers)
  14. `encoding` (Bedrock packet encoding helpers)
  15. `morton` (Z-order 2D/3D chunk coordinate mapping)
  16. `pmmpthread` (ZTS multithreading worker threads)
  17. `webrtc` (Voice & WebRTC networking support)

**Missing Modules:** `None (0/17 missing)`

---

## 4. Axolotl-PM 5.51.1 Engine & Server Execution

1. **PHAR Extraction & Integrity**:
   - Pinned PHAR deployed to `files/minehost/engines/bedrock/PocketMine-MP.phar` (3,647,735 bytes).
   - Executed `export TMPDIR=/data/user/0/com.minehost.app.debug/cache; libphp.so -n PocketMine-MP.phar --version`:
     - Returned: `Axolotl-PM 5.51.1 (git hash e881fc1bc1c401545baf6bd1f824e41ff3f57bd1) for Minecraft: Bedrock Edition v26.60`.
2. **Headless Server Deployment**:
   - Written `server.properties` and `pocketmine.yml` configured on port `19132` (UDP).
   - Server spawned worker threads, built chunk cache, generated spawn terrain, and reported:
     `[Server thread/INFO]: Done (1.821s)! For help, type "help" or "?"`
     Listening on `0.0.0.0:19132`.
3. **Live RakNet Loopback UDP Probe**:
   - Dispatched binary RakNet Unconnected Ping (`0x01` packet, 33 bytes) to `127.0.0.1:19132`.
   - Received binary RakNet Unconnected Pong (`0x1c` packet):
     - MOTD: `MCPE;Axolotl-PM Server;2225;1.26.60;0;20;8776101861345219578;Axolotl-PM;Survival;1;19132;19133;0;1;0;`
4. **Interactive Shutdown & World Persistence**:
   - Dispatched `kill -TERM` to server PID `27095`. Process flushed world data and exited cleanly.
   - Verified world storage generation:
     - `worlds/world/level.dat` generated.
     - `worlds/world/db/` contains valid LevelDB tables (`CURRENT`, `MANIFEST`, `.ldb` data).
5. **App Foreground Stability**:
   - `com.minehost.app.MainActivity` launched in foreground (PID `27944`, ~217 MB memory footprint).
   - Logcat inspection confirmed zero unhandled exceptions or crashes.

---

## 5. Comprehensive Test Results Matrix

| # | Test Phase | Verification Scope | Result | Details |
| :-: | :--- | :--- | :---: | :--- |
| 1 | Device Discovery | Connection Established | **PASS** | Serial: `10BF771RK10047Z` |
| 2 | Hardware Identification | ARM64 Architecture | **PASS** | Detected `arm64-v8a` |
| 3 | Android Version Check | Android 16 / API 36 | **PASS** | `API 36 >= minSdk 28` |
| 4 | Clean Streamed Install | APK Package Deployment | **PASS** | Streamed install in 4.90s |
| 5 | System Notification | `POST_NOTIFICATIONS` | **PASS** | Permission granted |
| 6 | Battery Optimization | Doze Whitelist | **PASS** | App added to deviceidle whitelist |
| 7 | Native Library Packaging | `libphp.so` in `lib/arm64` | **PASS** | Permissions `-rwxr-xr-x` verified |
| 8 | Native ELF Header | AArch64 Machine Type | **PASS** | Magic `\x7fELF` + 64-bit Little-Endian |
| 9 | PHP CLI Direct Binary | `libphp.so -n -v` | **PASS** | PHP 8.4.27 (cli) (ZTS) |
| 10 | PHP Extension Modules | All 17 Modules Audited | **PASS** | 17/17 verified present |
| 11 | Axolotl-PM Deployment | PHAR Copied to App Dir | **PASS** | 3.64 MB deployed |
| 12 | PHAR Cache Initialization | `TMPDIR` Cache Extract | **PASS** | Axolotl-PM 5.51.1 initialized |
| 13 | Server Configuration | `server.properties` | **PASS** | Written to server directory |
| 14 | Server Process Launch | Bedrock Server Startup | **PASS** | Listening on 0.0.0.0:19132 |
| 15 | RakNet UDP Protocol | Binary Unconnected Ping | **PASS** | Received Unconnected Pong MOTD |
| 16 | Process Lifecycle | Active Process Tracking | **PASS** | PID 27095 confirmed in process table |
| 17 | Graceful Shutdown | Process Termination | **PASS** | Port 19132 cleanly released |
| 18 | World Metadata | `level.dat` Generation | **PASS** | Valid NBT world header created |
| 19 | LevelDB Storage | Chunks Persistence | **PASS** | `CURRENT` & `MANIFEST` created |
| 20 | Android UI Launch | Activity Startup | **PASS** | Foreground Activity running (PID 27944) |
| 21 | Runtime Stability | Logcat Crash Check | **PASS** | Zero `AndroidRuntime` exceptions |

---

## 6. Verification Summary & Next Milestones

All 21 physical device test assertions passed with 100% success on the target Vivo I2221 hardware. The PHP `-n` fix eliminates the previous false missing-extension failures and confirms that the compiled-in runtime is fully functional for live Bedrock server execution.
