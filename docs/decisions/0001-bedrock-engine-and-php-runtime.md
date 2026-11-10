# Decision 0001: Bedrock engine and Android PHP runtime

- **Status:** Accepted as the implementation direction; runtime execution remains unverified.
- **Date:** 2026-11-10
- **Scope:** Bedrock engine source, PHP runtime source, and Android packaging strategy.

## Decision

Use **Axolotl-PM** as MineHost's primary PocketMine-compatible Bedrock server, and use the matching **Axolotl-PM PHP-Binaries Android ARM64 build** as the first runtime candidate.

Do not continue treating the original `pmmp/PocketMine-MP` or `pmmp/PHP-Binaries` repositories as maintained upstream dependencies.

## Why the previous source is no longer the right default

The original `pmmp/PocketMine-MP` repository was archived on 2026-07-09. Its final published release was 5.44.3. The original `pmmp/PHP-Binaries` repository was also archived in August 2026. Continuing to download from these repositories would freeze MineHost against an upstream that no longer publishes Bedrock protocol updates.

Sources:
- [Archived PocketMine-MP repository](https://github.com/pmmp/PocketMine-MP)
- [Archived PHP-Binaries releases](https://github.com/pmmp/PHP-Binaries/releases)

## Selected Bedrock server

The Axolotl-PM fork is an actively maintained continuation of PocketMine-MP. It preserves the PocketMine namespaces and most plugin/world/config compatibility, with caveats for plugins that use internal network APIs.

At decision time:
- Axolotl-PM release: **5.51.1**
- Target Bedrock version: **1.26.60**
- Release date: **2026-11-02**
- Source: [Axolotl-PM releases](https://github.com/axolotl-pm/PocketMine-MP/releases)
- Migration and compatibility notes: [Axolotl-PM migration guide](https://axolotl-pm.org/docs/migrating-from-pocketmine-mp)

MineHost keeps the existing engine ID `bedrock-pocketmine-v5` for database compatibility, but the user-facing engine name and release source now identify Axolotl-PM.

## Selected PHP runtime candidate

Use the matching release from [Axolotl-PM PHP-Binaries](https://github.com/axolotl-pm/PHP-Binaries/releases).

For the first validation target, pin:
- PHP line: **8.4**
- Release tag: `pm5-php-8.4-latest`
- Asset: `PHP-8.4-Android-arm64-PM5.tar.gz`
- Published SHA-256: `45b462ff5e290ee06d34eced589c325d27a2743185acab53acab291b0188e181`
- Published asset size: 16,030,349 bytes
- Release page: [PHP 8.4 PM5 release](https://github.com/axolotl-pm/PHP-Binaries/releases/tag/pm5-php-8.4-latest)

PHP 8.5 is available, but its release notes warn that some PM5 plugins may not yet work. Use 8.4 for the first compatibility baseline rather than selecting the newest version blindly.

Axolotl-PM's migration documentation says its PHP binaries include the `ext-webrtc` extension required for the newer NetherNet transport. This matters because the current Bedrock client/server networking path is evolving beyond RakNet alone.

## Android execution constraint — unresolved gate

Downloading an executable into `filesDir` and calling `setExecutable(true)` is **not** an acceptable Android runtime strategy. Android 10+ executable-memory and mount/SELinux policies can reject launching code from app-writable storage.

The archive is a runtime candidate, not yet an integrated runtime. Before enabling installation in the app, inspect the exact archive and prove:
1. The PHP executable's ELF architecture, ELF type, and program interpreter.
2. Whether the executable and all required extension libraries can run on the Android versions MineHost supports.
3. Whether the runtime can be packaged into the APK's extracted native-library directory and launched as a process, or whether a different supported loader/JNI embedding design is required.
4. The required extensions load successfully, including `pmmpthread`, `leveldb`, `chunkutils2`, `crypto`, `encoding`, `gmp`, `igbinary`, `morton`, `yaml`, `zip`, and `webrtc`.
5. `php -v`, `php -m`, and `php PocketMine-MP.phar --version` work on a physical Android ARM64 device.

Do not report the runtime as installed or enable Bedrock start until these checks pass. A filename ending in `.so` does not prove that the file is a shared library or a runnable CLI executable; inspect the ELF itself.

## Rejected shortcut

The third-party `ItzxDwi/AndroidPHP` project documents a PHP binary for Termux. Its stated setup requires Termux, and its repository history is very small. It is not a suitable production dependency for an Android app that must own its process lifecycle, avoid a separate terminal app, and control runtime packaging.

Source: [ItzxDwi/AndroidPHP](https://github.com/ItzxDwi/AndroidPHP)

## Version and integrity policy

- Do not use unqualified `releases/latest` for a production runtime.
- Pin the Axolotl-PM server release and the PHP runtime tag together.
- Verify the asset against the publisher's SHA-256 digest before extraction.
- Store the exact version, source URL, digest, and verification result in the runtime manifest.
- Never overwrite an active runtime in place; stage, verify, then atomically activate with rollback.
- Audit LGPL and all bundled extension/library licenses before redistribution.

## Acceptance criteria for the Bedrock runtime phase

This decision is **not** the completion of the runtime phase. That phase is complete only when:
- A release-pinned Axolotl-PM PHAR and matching PHP runtime are installed.
- The PHP process actually launches from MineHost on a physical ARM64 Android device.
- Required extensions load without missing-library or linker errors.
- MineHost receives a real RakNet pong from the launched server.
- A real Bedrock client joins over LAN, places/breaks blocks, and the world persists after a clean stop/restart.
- The runtime remains usable under the foreground service and screen-off conditions.
