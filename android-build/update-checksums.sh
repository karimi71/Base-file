#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
TOOL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$TOOL_DIR"
tmp="$(mktemp)"
trap 'rm -f "$tmp"' EXIT
{
    echo "# SHA-256 checksums for every committed toolchain file (text and binary)."
    echo "# Paths are relative to android-build/. Generated deterministically."
    find . -type f \
        ! -path './SHA256SUMS.txt' \
        ! -path './ci/LAST_RUN.log' \
        ! -path './.cache/*' \
        \( ! -path '*/build/*' -o -path './maven/*' \) \
        ! -path '*/.gradle/*' \
        -print0 \
        | LC_ALL=C sort -z \
        | xargs -0 sha256sum
} > "$tmp"
mv "$tmp" SHA256SUMS.txt
trap - EXIT
