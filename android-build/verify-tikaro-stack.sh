#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Rebuild every pinned Tikaro fixture with no remote dependency resolution.
set -euo pipefail

TOOL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "$TOOL_DIR/versions.env"

TIKARO_SMOKE="$TOOL_DIR/tikaro-stack-smoke-test"
PAPARAZZI_SMOKE="$TOOL_DIR/paparazzi-smoke-test"
QUALITY_SMOKE="$TOOL_DIR/quality-smoke-test"
MAVEN_REPO="$TOOL_DIR/maven"

"$TOOL_DIR/prepare-offline-toolchain.sh" >/dev/null
CACHE_DIR="${BASE_FILE_CACHE:-$TOOL_DIR/.cache}"
export JAVA_HOME="$CACHE_DIR/jdk17-gradle"
export ANDROID_HOME="$CACHE_DIR/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

remove_gradle_home=0
if [[ -z "${BASE_FILE_GRADLE_USER_HOME:-}" ]]; then
    BASE_FILE_GRADLE_USER_HOME="$(mktemp -d "${TMPDIR:-/tmp}/base-file-tikaro-gradle.XXXXXX")"
    remove_gradle_home=1
fi
export GRADLE_USER_HOME="$BASE_FILE_GRADLE_USER_HOME"
ANDROID_USER_TEMP="$(mktemp -d "${TMPDIR:-/tmp}/base-file-tikaro-android.XXXXXX")"
export ANDROID_USER_HOME="$ANDROID_USER_TEMP"
cleanup() {
    rm -rf "$ANDROID_USER_TEMP"
    if [[ "$remove_gradle_home" == 1 ]]; then
        rm -rf "$BASE_FILE_GRADLE_USER_HOME"
    fi
}
trap cleanup EXIT INT TERM

python3 "$TOOL_DIR/ci/verify_requested_coordinates.py" \
    "$TIKARO_SMOKE/REQUESTED_COORDINATES.tsv" \
    "$MAVEN_REPO/BASE_FILE_COORDINATES.tsv"

rm -rf \
    "$TIKARO_SMOKE/.gradle" "$TIKARO_SMOKE/app/build" "$TIKARO_SMOKE/benchmark/build" \
    "$PAPARAZZI_SMOKE/.gradle" "$PAPARAZZI_SMOKE/screenshot/build" \
    "$QUALITY_SMOKE/.gradle" "$QUALITY_SMOKE/build"

run_offline_gradle() {
    local project="$1"
    shift
    "$TOOL_DIR/gradle/gradle-$GRADLE_VERSION/bin/gradle" \
        --offline \
        --no-daemon \
        --stacktrace \
        --init-script "$TOOL_DIR/offline.init.gradle" \
        -Dbasefile.repo="$MAVEN_REPO" \
        -PbaseFileMaven="$MAVEN_REPO" \
        -p "$project" \
        "$@"
}

run_offline_gradle "$TIKARO_SMOKE" \
    :app:assembleDebug \
    :app:assembleRelease \
    :app:testDebugUnitTest \
    :app:assembleDebugAndroidTest \
    :benchmark:assembleNonMinifiedBenchmark
run_offline_gradle "$PAPARAZZI_SMOKE" :screenshot:testDebugUnitTest
run_offline_gradle "$QUALITY_SMOKE" classes resolveQualityTools

TIKARO_DEBUG_APK="$TIKARO_SMOKE/app/build/outputs/apk/debug/app-debug.apk"
TIKARO_RELEASE_APK="$TIKARO_SMOKE/app/build/outputs/apk/release/app-release-unsigned.apk"
test -s "$TIKARO_DEBUG_APK"
test -s "$TIKARO_RELEASE_APK"
"$ANDROID_HOME/build-tools/$ANDROID_BUILD_TOOLS_VERSION/apksigner" verify \
    --verbose --print-certs "$TIKARO_DEBUG_APK"
unzip -l "$TIKARO_RELEASE_APK" | grep -q 'assets/dexopt/baseline.prof'
find "$TIKARO_SMOKE/app/build/outputs/apk/androidTest" \
    -type f -name '*.apk' -size +0c -print -quit | grep -q .
find "$TIKARO_SMOKE/benchmark/build/outputs/apk" \
    -type f -name '*.apk' -size +0c -print -quit | grep -q .
test -d "$PAPARAZZI_SMOKE/screenshot/build/reports/paparazzi"

printf 'Tikaro debug APK: %s\n' "$TIKARO_DEBUG_APK"
sha256sum "$TIKARO_DEBUG_APK"
echo "All offline Tikaro stack, test, screenshot, benchmark, and quality checks passed."
