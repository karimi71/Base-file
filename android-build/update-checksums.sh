#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
TOOL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$TOOL_DIR"
# Reconstructed oversized Maven artifacts are derived working-tree files. Their
# committed chunks and original SHA-256 are covered by this manifest instead.
if [[ -d maven && -f ci/split_maven_artifacts.py ]]; then
    python3 ci/split_maven_artifacts.py clean maven
fi
tmp="$(mktemp)"
trap 'rm -f "$tmp"' EXIT
{
    echo "# SHA-256 checksums for every committed toolchain file (text and binary)."
    echo "# Paths are relative to android-build/. Generated deterministically."
    find . -type f \
        ! -path './SHA256SUMS.txt' \
        ! -path './ci/LAST_RUN.log' \
        ! -path '*/.cache/*' \
        \( ! -path '*/build/*' -o -path './maven/*' \) \
        ! -path '*/.gradle/*' \
        ! -path '*/.gradle-home/*' \
        ! -path '*/.kotlin/*' \
        ! -path '*/.jqwik-database' \
        ! -path '*/.jqwik-database/*' \
        ! -path '*/local.properties' \
        -print0 \
        | LC_ALL=C sort -z \
        | xargs -0 sha256sum
} > "$tmp"
mv "$tmp" SHA256SUMS.txt
trap - EXIT
