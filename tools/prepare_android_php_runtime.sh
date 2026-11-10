#!/usr/bin/env bash
# Prepare the pinned, statically linked PocketMine PHP CLI for Android ARM64.
#
# The executable is packaged inside the APK under jniLibs so Android extracts it
# into nativeLibraryDir. Do NOT download this ELF into filesDir and try to exec it:
# modern Android restricts execution from app-writable directories.
#
# This is a development/build-time dependency bootstrap. The PHP-Binaries release
# and every dependency licence must be reviewed before publishing a release APK.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

PHP_RELEASE_TAG="pm5-php-8.4-latest"
PHP_ASSET="PHP-8.4-Android-arm64-PM5.tar.gz"
PHP_URL="https://github.com/pmmp/PHP-Binaries/releases/download/${PHP_RELEASE_TAG}/${PHP_ASSET}"
# SHA-256 digest published by the GitHub Releases API for this exact asset.
EXPECTED_SHA256="d8867966340121f821591b9bb29c80a58ad77abd6cc8e0e44d1cbbb3aaedd70c"

OUT_DIR="$ROOT_DIR/app/src/main/jniLibs/arm64-v8a"
OUT_FILE="$OUT_DIR/libphp.so"
ARCHIVE="$WORK_DIR/php-android-arm64.tar.gz"

command -v curl >/dev/null 2>&1 || { echo "curl is required" >&2; exit 1; }
command -v sha256sum >/dev/null 2>&1 || { echo "sha256sum is required" >&2; exit 1; }
command -v tar >/dev/null 2>&1 || { echo "tar is required" >&2; exit 1; }
command -v readelf >/dev/null 2>&1 || { echo "readelf is required (binutils)" >&2; exit 1; }

echo "Downloading pinned PocketMine PHP runtime asset..."
curl --fail --location --retry 3 --retry-delay 2 --output "$ARCHIVE" "$PHP_URL"

echo "Verifying published SHA-256..."
echo "${EXPECTED_SHA256}  ${ARCHIVE}" | sha256sum --check --status || {
  echo "PHP runtime archive checksum mismatch; refusing to package it." >&2
  exit 1
}

echo "Extracting only the Android ARM64 PHP executable..."
mkdir -p "$WORK_DIR/extracted"
tar -xzf "$ARCHIVE" -C "$WORK_DIR/extracted" bin/php7/bin/php
PHP_BINARY="$WORK_DIR/extracted/bin/php7/bin/php"
test -s "$PHP_BINARY" || { echo "Expected PHP binary missing from archive" >&2; exit 1; }

echo "Checking ELF architecture..."
readelf -h "$PHP_BINARY" | grep -q "AArch64" || {
  echo "The downloaded PHP executable is not AArch64." >&2
  exit 1
}

echo "Checking static linkage..."
readelf -d "$PHP_BINARY" >"$WORK_DIR/dynamic-section.txt" 2>&1 || true
grep -q "There is no dynamic section in this file" "$WORK_DIR/dynamic-section.txt" || {
  echo "PHP binary is not statically linked; refusing to package an unverified runtime." >&2
  cat "$WORK_DIR/dynamic-section.txt" >&2
  exit 1
}

mkdir -p "$OUT_DIR"
install -m 0755 "$PHP_BINARY" "$OUT_FILE"

echo "Prepared: $OUT_FILE"
echo "Binary SHA-256: $(sha256sum "$OUT_FILE" | cut -d' ' -f1)"
echo "NOTE: this validates archive integrity, ELF architecture and static linkage only."
echo "A physical Android device test is still required before marking PocketMine supported."
