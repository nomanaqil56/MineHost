#!/usr/bin/env bash
# Verifies the pinned Android ARM64 PHP archive and runs its static AArch64 CLI under QEMU.
# QEMU success is a userspace smoke test, NOT proof of Android SELinux/W^X execution.
set -Eeuo pipefail
RELEASE_TAG="pm5-php-8.4-latest"
ASSET_NAME="PHP-8.4-Android-arm64-PM5.tar.gz"
EXPECTED_SHA256="45b462ff5e290ee06d34eced589c325d27a2743185acab53acab291b0188e181"
DOWNLOAD_URL="https://github.com/axolotl-pm/PHP-Binaries/releases/download/${RELEASE_TAG}/${ASSET_NAME}"
WORK_DIR="${1:-$(mktemp -d)}"
mkdir -p "$WORK_DIR"
ARCHIVE="$WORK_DIR/$ASSET_NAME"
EXTRACT_DIR="$WORK_DIR/extracted"
REPORT="$WORK_DIR/android-php-runtime-inspection.txt"
echo "Downloading pinned Axolotl-PM PHP runtime asset..."
curl --fail --location --retry 3 --retry-delay 2 "$DOWNLOAD_URL" --output "$ARCHIVE"
echo "$EXPECTED_SHA256  $ARCHIVE" | sha256sum --check -
mkdir -p "$EXTRACT_DIR"
tar -xzf "$ARCHIVE" -C "$EXTRACT_DIR"
{
  echo "MineHost Android PHP runtime inspection"
  echo "Release: $RELEASE_TAG"
  echo "Asset: $ASSET_NAME"
  echo "Expected SHA-256: $EXPECTED_SHA256"
  echo "Actual SHA-256: $(sha256sum "$ARCHIVE" | awk '{print $1}')"
  echo
  echo "Archive contents:"
  tar -tzf "$ARCHIVE"
  echo
  echo "PHP executable candidates:"
} > "$REPORT"
mapfile -t PHP_CANDIDATES < <(find "$EXTRACT_DIR" -type f \( -name php -o -name php8 -o -name php8.4 \) | sort)
if (("${#PHP_CANDIDATES[@]}" == 0)); then
  echo "ERROR: no PHP CLI candidate named php/php8/php8.4 was found in the verified archive." | tee -a "$REPORT"
  cat "$REPORT"
  exit 1
fi
QEMU_BIN="$(command -v qemu-aarch64 || command -v qemu-aarch64-static || true)"
if [[ -z "$QEMU_BIN" ]]; then
  echo "ERROR: qemu-aarch64 is required for the ARM64 userspace smoke test." | tee -a "$REPORT"
  cat "$REPORT"
  exit 1
fi
for candidate in "${PHP_CANDIDATES[@]}"; do
  {
    echo
    echo "Candidate: ${candidate#"$EXTRACT_DIR"/}"
    file "$candidate"
    echo "Archive executable mode: $(stat -c '%A (%a)' "$candidate")"
    echo "ELF header:"
    readelf -h "$candidate" | grep -E 'Class:|Data:|Type:|Machine:'
    echo "Program interpreter (if dynamically linked):"
    readelf -l "$candidate" | grep -F 'Requesting program interpreter' || echo "No PT_INTERP segment reported (static ELF)."
  } | tee -a "$REPORT"
  if ! readelf -h "$candidate" | grep -q 'Machine:.*AArch64'; then
    echo "ERROR: PHP candidate is not AArch64." | tee -a "$REPORT"
    cat "$REPORT"
    exit 1
  fi
  if readelf -l "$candidate" | grep -q 'Requesting program interpreter'; then
    echo "ERROR: expected a static Android cross-compiled CLI, but a PT_INTERP segment exists." | tee -a "$REPORT"
    cat "$REPORT"
    exit 1
  fi
  echo "QEMU PHP version output:" | tee -a "$REPORT"
  "$QEMU_BIN" "$candidate" -n -v 2>&1 | tee -a "$REPORT"
  echo "QEMU PHP module list:" | tee -a "$REPORT"
  MODULE_OUTPUT="$("$QEMU_BIN" "$candidate" -n -m 2>&1)"
  printf '%s\n' "$MODULE_OUTPUT" | tee -a "$REPORT"
  REQUIRED_MODULES=(curl gmp igbinary leveldb mbstring openssl phar sockets yaml zip zlib chunkutils2 crypto encoding morton pmmpthread webrtc)
  MISSING=()
  for module in "${REQUIRED_MODULES[@]}"; do
    if ! grep -Fxiq "$module" <<< "$MODULE_OUTPUT"; then
      MISSING+=("$module")
    fi
  done
  if (("${#MISSING[@]}" > 0)); then
    echo "ERROR: required PocketMine/Axolotl PHP modules missing under QEMU: ${MISSING[*]}" | tee -a "$REPORT"
    cat "$REPORT"
    exit 1
  fi
  echo "Required PHP module smoke check with production -n mode: PASS" | tee -a "$REPORT"
  echo "PHP version constant with production -n mode:" | tee -a "$REPORT"
  "$QEMU_BIN" "$candidate" -n -r 'echo PHP_VERSION, PHP_EOL;' 2>&1 | tee -a "$REPORT"
done

# Verify the pinned Axolotl-PM PHAR with the same no-ini mode used by MineHost.
PHAR_TAG="5.51.1"
PHAR_SHA256="bcfbd0deff349c2b25b1688299a7d54f1fc8a080d2f95e19884f8e341319021a"
PHAR_URL="https://github.com/axolotl-pm/PocketMine-MP/releases/download/${PHAR_TAG}/PocketMine-MP.phar"
PHAR_FILE="$WORK_DIR/PocketMine-MP.phar"
curl --fail --location --retry 3 --retry-delay 2 "$PHAR_URL" --output "$PHAR_FILE"
echo "$PHAR_SHA256  $PHAR_FILE" | sha256sum --check -
PHP_CANDIDATE="${PHP_CANDIDATES[0]}"
echo "Pinned Axolotl-PM PHAR launch smoke test (-n):" | tee -a "$REPORT"
"$QEMU_BIN" "$PHP_CANDIDATE" -n "$PHAR_FILE" --version 2>&1 | tee -a "$REPORT"
{
  echo
  echo "Potential extension and support files:"
  find "$EXTRACT_DIR" -type f | sed "s#^$EXTRACT_DIR/##" | sort
  echo
  echo "RESULT: checksum, AArch64 static ELF checks, and QEMU CLI/module smoke tests passed."
  echo "LIMITATION: QEMU does not reproduce Android app SELinux policy, noexec mounts, linker namespace, or foreground-service behavior."
  echo "A physical Android ARM64 device test is still mandatory before marking the PHP runtime runnable."
} | tee -a "$REPORT"
echo "Inspection report: $REPORT"
