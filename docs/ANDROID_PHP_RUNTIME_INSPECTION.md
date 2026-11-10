# Android PHP Runtime Inspection Gate

MineHost targets Axolotl-PM 5.51.1 and the matching PHP 8.4 Android ARM64 runtime release asset.

## Pinned input

- Repository: `axolotl-pm/PHP-Binaries`
- Release tag: `pm5-php-8.4-latest`
- Asset: `PHP-8.4-Android-arm64-PM5.tar.gz`
- Expected SHA-256: `45b462ff5e290ee06d34eced589c325d27a2743185acab53acab291b0188e181`

## Automated gate

`scripts/inspect-android-php-runtime.sh` downloads the pinned asset, verifies the published SHA-256, extracts it in a temporary directory, inventories the archive, locates PHP CLI candidates, and checks that candidate ELF files target AArch64. It records the ELF program interpreter and likely extension files in a CI artifact.

The gate is intentionally structural. It does not execute the Android binary on the Ubuntu runner, and therefore does not prove that its loader and shared-library dependencies exist on Android, that PHP extensions load, or that Android permits the process to launch from a location MineHost can use.

## Required device acceptance before runtime integration

1. Inspect the generated report and archive layout.
2. Resolve the reported ELF interpreter and every non-system dependency against Android/Bionic and the chosen packaging strategy.
3. Verify the required PHP extensions, especially Axolotl-PM's WebRTC extension, by running the PHP CLI on a physical ARM64 Android device.
4. Run `php -v`, `php -m`, and a small PHP script that loads the PHAR.
5. Launch Axolotl-PM 5.51.1 from MineHost, confirm the server's protocol-ready response, then join from a real Bedrock client over LAN.
6. Only after those checks pass should the runtime be marked installed or the Bedrock engine be described as runnable.

**Current status:** archive inspection is automated in CI; Android execution and server launch remain unverified. Do not treat the CI archive inspection as proof of a working MineHost runtime.
