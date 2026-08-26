#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail

TOOL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "$TOOL_DIR/versions.env"

(
    cd "$TOOL_DIR"
    sha256sum --check SHA256SUMS.txt
)

"$TOOL_DIR/prepare-offline-toolchain.sh" >/dev/null
CACHE_DIR="${BASE_FILE_CACHE:-$TOOL_DIR/.cache}"
export JAVA_HOME="$CACHE_DIR/jdk17-gradle"
export ANDROID_HOME="$CACHE_DIR/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
BUILD_TOOLS="$ANDROID_HOME/build-tools/$ANDROID_BUILD_TOOLS_VERSION"
PLATFORM="$ANDROID_HOME/platforms/android-$ANDROID_COMPILE_SDK"

"$JAVA_HOME/bin/java" -version
"$TOOL_DIR/gradle/gradle-$GRADLE_VERSION/bin/gradle" --version
"$TOOL_DIR/kotlin/bin/kotlinc" -version
"$BUILD_TOOLS/aapt2" version
"$BUILD_TOOLS/d8" --version
"$BUILD_TOOLS/apksigner" version
"$BUILD_TOOLS/aapt2" dump resources "$PLATFORM/framework-res.apk" >/dev/null
"$BUILD_TOOLS/zipalign" -h >/dev/null 2>&1 || [[ $? -eq 1 ]]

test -d "$PLATFORM/data/res"
test -s "$PLATFORM/android.jar"
test -s "$PLATFORM/framework-res.apk"

if [[ "${1:-}" != --tools-only ]]; then
    rm -rf "$TOOL_DIR/compose-smoke-test/.gradle" \
           "$TOOL_DIR/compose-smoke-test/app/build"
    BASE_FILE_GRADLE_USER_HOME="$(mktemp -d "${TMPDIR:-/tmp}/base-file-gradle-verify.XXXXXX")"
    export BASE_FILE_GRADLE_USER_HOME
    trap 'rm -rf "$BASE_FILE_GRADLE_USER_HOME"' EXIT INT TERM
    "$TOOL_DIR/build-compose-apk.sh" "$TOOL_DIR/compose-smoke-test"
    APK="$TOOL_DIR/compose-smoke-test/app/build/outputs/apk/debug/app-debug.apk"
    "$BUILD_TOOLS/apksigner" verify --verbose --print-certs "$APK"
    unzip -l "$APK" | grep -q 'META-INF/androidx.compose.runtime_runtime.version'
fi

echo "All offline Android toolchain checks passed."
