# Android PHP Runtime Inspection Gate

MineHost targets Axolotl-PM 5.51.1 and the matching PHP 8.4 Android ARM64 runtime release asset.

## Pinned input

- Repository: `axolotl-pm/PHP-Binaries`
- Release tag: `pm5-php-8.4-latest`
- Asset: `PHP-8.4-Android-arm64-PM5.tar.gz`
- Expected SHA-256: `45b462ff5e290ee06d34eced589c325d27a2743185acab53acab291b0188e181`

## Verified by CI

The runtime inspection job has now established from the release archive that:

- The asset checksum matches the release's published SHA-256.
- The PHP CLI is at `bin/php7/bin/php` inside the archive.
- It is an AArch64 ELF executable with executable mode set.
- It has no `PT_INTERP` segment and is reported by `file` as statically linked. The CLI reports PHP 8.4.27 (ZTS), and its compiled module list includes the PocketMine/Axolotl extensions such as `chunkutils2`, `crypto`, `encoding`, `leveldb`, `morton`, `pmmpthread`, `webrtc`, and `yaml`.
- The CI job executes the ARM64 CLI under QEMU, checks PHP 8.4's version, validates the PHP modules required by Axolotl-PM 5.51.1 (including `webrtc`), and exercises the bundled `php.ini` when present.

This is a meaningful improvement over checking the archive structure alone. QEMU is an ARM64 userspace smoke test, not an Android device. It does not emulate app SELinux policy, Android's executable-memory restrictions, noexec mounts, or the device's process supervision behavior.

## Runtime layout correction

The PHP CLI path in the archive is `bin/php7/bin/php`. When this archive is extracted into MineHost's managed `runtimes/php` directory, the expected CLI path is:

`<filesDir>/minehost/runtimes/php/bin/php7/bin/php`

Runtime discovery now checks this exact path first. It still requires a regular file with the executable bit set. Presence alone does not prove that Android will launch it.

## Required physical-device acceptance

1. Install the debug APK on a 64-bit ARM Android device.
2. Verify the binary can be launched from the exact packaging/location chosen by MineHost (not merely from a shell app).
3. Run `php -v`, `php -m`, and a PHP script that loads the Axolotl-PM PHAR.
4. Launch Axolotl-PM 5.51.1 from MineHost, confirm its RakNet protocol-ready response, then join from a real Bedrock client over LAN.
5. Verify save, stop, restart, and world persistence.
6. Only after those checks pass should Diagnostics mark the runtime as runnable or the Bedrock engine be described as working.

**Current status:** archive integrity, static AArch64 ELF shape, and QEMU PHP CLI/module smoke tests are CI-verifiable. Android process launch, actual server launch, and a real client join remain unverified.
