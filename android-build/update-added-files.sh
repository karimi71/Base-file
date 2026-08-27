#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Regenerate the exact list of paths added on this Arena branch.
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
BASE_REVISION="a978940c92297269142c10614bc992793f8d788f"
OUTPUT="$ROOT/android-build/ADDED_FILES.txt"

git -C "$ROOT" cat-file -e "$BASE_REVISION^{commit}"
tmp="$(mktemp)"
trap 'rm -f "$tmp"' EXIT

# Compare the base tree with the index, rather than only HEAD. This lets CI
# include freshly generated lockfiles/legal reports in the same final commit.
git -C "$ROOT" diff --cached --diff-filter=A --name-only "$BASE_REVISION" \
    | LC_ALL=C sort -u > "$tmp"

# The inventory is itself an added path and must remain self-describing.
grep -qxF 'android-build/ADDED_FILES.txt' "$tmp" \
    || printf '%s\n' 'android-build/ADDED_FILES.txt' >> "$tmp"
LC_ALL=C sort -u -o "$tmp" "$tmp"
mv "$tmp" "$OUTPUT"
trap - EXIT
