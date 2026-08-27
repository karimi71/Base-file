#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Verify optional future fixtures from an empty cache and the local repository only.
set -euo pipefail

TOOL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "$TOOL_DIR/versions.env"
FUTURE_SMOKE="$TOOL_DIR/future-stack-smoke-test"
QUALITY_SMOKE="$TOOL_DIR/quality-smoke-test"
MAVEN_REPO="$TOOL_DIR/maven"

"$TOOL_DIR/prepare-offline-toolchain.sh" >/dev/null
CACHE_DIR="${BASE_FILE_CACHE:-$TOOL_DIR/.cache}"
export JAVA_HOME="$CACHE_DIR/jdk17-gradle"
export ANDROID_HOME="$CACHE_DIR/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

remove_gradle_home=0
if [[ -z "${BASE_FILE_FUTURE_GRADLE_USER_HOME:-}" ]]; then
    BASE_FILE_FUTURE_GRADLE_USER_HOME="$(mktemp -d "${TMPDIR:-/tmp}/base-file-future-gradle.XXXXXX")"
    remove_gradle_home=1
fi
export GRADLE_USER_HOME="$BASE_FILE_FUTURE_GRADLE_USER_HOME"
ANDROID_USER_TEMP="$(mktemp -d "${TMPDIR:-/tmp}/base-file-future-android.XXXXXX")"
export ANDROID_USER_HOME="$ANDROID_USER_TEMP"
cleanup() {
    rm -rf "$ANDROID_USER_TEMP"
    if [[ "$remove_gradle_home" == 1 ]]; then
        rm -rf "$BASE_FILE_FUTURE_GRADLE_USER_HOME"
    fi
}
trap cleanup EXIT INT TERM

mapfile -t future_locks < <(find "$FUTURE_SMOKE" -name gradle.lockfile -type f | LC_ALL=C sort)
python3 "$TOOL_DIR/ci/verify_future_coordinates.py" \
    "$FUTURE_SMOKE/REQUESTED_COORDINATES.tsv" \
    "$FUTURE_SMOKE/NATIVE_CLASSIFIERS.tsv" \
    "$MAVEN_REPO/BASE_FILE_COORDINATES.tsv" \
    "$MAVEN_REPO" \
    "${future_locks[@]}"

rm -rf \
    "$FUTURE_SMOKE/.gradle" \
    "$FUTURE_SMOKE/jvm/build" \
    "$FUTURE_SMOKE/android/build" \
    "$FUTURE_SMOKE/roborazzi/build" \
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
        -PbaseFileMaven="$(python3 -c 'import pathlib,sys; print(pathlib.Path(sys.argv[1]).resolve().as_uri())' "$MAVEN_REPO")" \
        -p "$project" \
        "$@"
}

run_offline_gradle "$FUTURE_SMOKE" resolveFutureCoordinates generateDependencyReports
run_offline_gradle "$FUTURE_SMOKE" :jvm:test :jvm:verifyGeneratedSources
run_offline_gradle "$FUTURE_SMOKE" \
    :android:assembleDebug \
    :android:assembleRelease \
    :android:testDebugUnitTest \
    :android:assembleDebugAndroidTest
run_offline_gradle "$FUTURE_SMOKE" :roborazzi:verifyRoborazziDebug
run_offline_gradle "$FUTURE_SMOKE" \
    -Dbasefile.roborazzi.variant=mismatch \
    :roborazzi:compareRoborazziDebug
run_offline_gradle "$QUALITY_SMOKE" classes resolveQualityTools verifyLicenseReport

DEBUG_APK="$FUTURE_SMOKE/android/build/outputs/apk/debug/android-debug.apk"
RELEASE_APK="$FUTURE_SMOKE/android/build/outputs/apk/release/android-release-unsigned.apk"
test -s "$DEBUG_APK"
test -s "$RELEASE_APK"
"$ANDROID_HOME/build-tools/$ANDROID_BUILD_TOOLS_VERSION/apksigner" verify \
    --verbose --print-certs "$DEBUG_APK"
test -s "$FUTURE_SMOKE/android/build/outputs/mapping/release/mapping.txt"
find "$FUTURE_SMOKE/android/build/outputs/apk/androidTest" \
    -type f -name '*.apk' -size +0c -print -quit | grep -q .
find "$FUTURE_SMOKE/roborazzi/src/test/snapshots" \
    -type f -name '*.png' -size +0c | awk 'END { exit(NR >= 6 ? 0 : 1) }'
find "$FUTURE_SMOKE/roborazzi/build/outputs/roborazzi-compare" \
    -type f -name '*_compare.png' -size +0c -print -quit | grep -q .
test -s "$FUTURE_SMOKE/roborazzi/build/reports/roborazzi/index.html"
test -s "$FUTURE_SMOKE/roborazzi/schemas/dev.basefile.future.roborazzi.MigrationDatabase/2.json"
test -s "$FUTURE_SMOKE/DEPENDENCY_REPORT.tsv"
test -s "$FUTURE_SMOKE/VERSION_SELECTION_REPORT.tsv"
grep -q $'net.bytebuddy\tbyte-buddy\t' "$FUTURE_SMOKE/DEPENDENCY_REPORT.tsv"
grep -q $'com.squareup.okhttp3\tokhttp\t' "$FUTURE_SMOKE/DEPENDENCY_REPORT.tsv"

PROTOC="$MAVEN_REPO/com/google/protobuf/protoc/$PROTOBUF_VERSION/protoc-$PROTOBUF_VERSION-linux-x86_64.exe"
PROTOC_TEMP="$(mktemp "${TMPDIR:-/tmp}/base-file-protoc.XXXXXX")"
cp "$PROTOC" "$PROTOC_TEMP"
chmod 0700 "$PROTOC_TEMP"
"$PROTOC_TEMP" --version | grep -F "libprotoc 29.3"
rm -f "$PROTOC_TEMP"

printf 'Future debug APK: %s\n' "$DEBUG_APK"
sha256sum "$DEBUG_APK" "$RELEASE_APK"
echo "All strict-offline future JVM, Android, R8, instrumentation-compile, Roborazzi, accessibility, migration, PDF, Coil, crypto, protoc, and license checks passed."
