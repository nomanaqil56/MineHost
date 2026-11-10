# Android PHP Runtime Verification

MineHost pins the Axolotl-PM Android ARM64 PHP 8.4 CLI archive and Axolotl-PM 5.51.1 PHAR.

## Pinned inputs

- PHP repository: `axolotl-pm/PHP-Binaries`
- PHP release: `pm5-php-8.4-latest`
- PHP asset: `PHP-8.4-Android-arm64-PM5.tar.gz`
- PHP SHA-256: `45b462ff5e290ee06d34eced589c325d27a2743185acab53acab291b0188e181`
- CLI path: `bin/php7/bin/php`
- PHP version: 8.4.27 ZTS, static AArch64 executable
- Axolotl-PM PHAR: 5.51.1, SHA-256 `bcfbd0deff349c2b25b1688299a7d54f1fc8a080d2f95e19884f8e341319021a`

## Physical-device finding and correction (2026-11-10)

The first Vivo Android 16 test reached BedrockEngine startup but reported `sockets`, `yaml`, `zip`, `zlib`, and `webrtc` missing. The app had invoked PHP with the upstream `php.ini` (`-c`). The same static binary's modules had passed the CI check under `-n`, which disables external ini loading and exposes modules compiled into the binary.

**Fix:** MineHost now uses `php -n` for version validation, module validation, and the actual Axolotl-PM server process. The upstream ini is no longer copied or packaged. This makes the check and server invocation identical and avoids loading external extension modules that are not shipped with the static CLI.

## CI checks

The runtime inspection script verifies:
1. Pinned archive checksum and AArch64 static ELF shape.
2. PHP version and all 17 required modules using `php -n`.
3. The pinned Axolotl-PM PHAR checksum.
4. PHAR version execution using the same `php -n` mode as the app.

APK CI verifies `lib/arm64-v8a/libphp.so` is present and rejects an external PHP ini asset.

QEMU does not emulate Android process policy, MineHost's foreground service, ServerController, LAN networking, or a real Minecraft client. These still require physical-device testing.

## Acceptance test after corrected APK

1. Install on ARM64 Android 9+.
2. Start a Bedrock server from MineHost's UI.
3. Confirm no missing-module error.
4. Confirm Axolotl-PM reaches RakNet-ready state on UDP 19132.
5. Join from a second Bedrock client over LAN.
6. Stop/restart and verify world and player persistence.

**Status:** source fix is pending CI verification and device retest. MineHost is not yet production-ready.
