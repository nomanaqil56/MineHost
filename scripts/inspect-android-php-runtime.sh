#!/usr/bin/env bash
# Structural inspection only: this does NOT prove Android can execute the binary.
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
  echo "MineHost Android PHP runtime structural inspection"
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
for candidate in "${PHP_CANDIDATES[@]}"; do
  {
    echo
    echo "Candidate: ${candidate#"$EXTRACT_DIR"/}"
    file "$candidate"
    echo "Archive executable mode: $(stat -c '%A (%a)' "$candidate")"
    echo "ELF header:"
    readelf -h "$candidate" | grep -E 'Class:|Data:|Type:|Machine:'
    echo "Program interpreter (if dynamically linked):"
    readelf -l "$candidate" | grep -F 'Requesting program interpreter' || echo "No PT_INTERP segment reported (possibly static)."
  } | tee -a "$REPORT"
  if ! readelf -h "$candidate" | grep -q 'Machine:.*AArch64'; then
    echo "ERROR: PHP candidate is not AArch64." | tee -a "$REPORT"
    cat "$REPORT"
    exit 1
  fi
done
{
  echo
  echo "Potential WebRTC / runtime extension files:"
  find "$EXTRACT_DIR" -type f \( -iname '*webrtc*' -o -iname '*sockets*' -o -iname '*openssl*' -o -iname '*curl*' \) -printf '%P\n' | sort || true
  echo
  echo "IMPORTANT: this job verifies download integrity, archive structure, and ELF architecture only."
  echo "It does NOT execute the Android binary, prove Bionic/musl loader compatibility, verify PHP extensions at runtime,"
  echo "or establish that Android permits process launch from MineHost's packaged or app-private locations."
} | tee -a "$REPORT"
echo "Inspection report: $REPORT"
