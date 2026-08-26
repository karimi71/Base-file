#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail

TOOL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "$TOOL_DIR/versions.env"
CACHE_DIR="${BASE_FILE_CACHE:-$TOOL_DIR/.cache}"
JDK_ARCHIVE="$TOOL_DIR/jdk/openjdk17-gradle-linux-x64.tar.gz"
PLATFORM_ARCHIVE="$TOOL_DIR/sdk/android-sdk-platform-35.tar.xz"
BUILD_TOOLS_ARCHIVE="$TOOL_DIR/sdk/android-sdk-build-tools-34.0.0.tar.xz"
FRAMEWORK_APK="$TOOL_DIR/sdk/framework-res-api35.apk"

if [[ "${1:-}" == --clean ]]; then
    rm -rf "$CACHE_DIR"
    exit 0
fi

for required in "$JDK_ARCHIVE" "$PLATFORM_ARCHIVE" "$BUILD_TOOLS_ARCHIVE" "$FRAMEWORK_APK"; do
    if [[ ! -s "$required" ]]; then
        echo "Missing toolchain artifact: $required" >&2
        exit 1
    fi
done

mkdir -p "$CACHE_DIR"
archive_key="$({ sha256sum "$JDK_ARCHIVE" "$PLATFORM_ARCHIVE" "$BUILD_TOOLS_ARCHIVE" "$FRAMEWORK_APK"; } | sha256sum | cut -d' ' -f1)"
marker="$CACHE_DIR/.prepared-${archive_key}"

if [[ ! -f "$marker" ]]; then
    echo "Extracting pinned JDK and Android SDK into $CACHE_DIR"
    rm -rf "$CACHE_DIR/jdk17-gradle" "$CACHE_DIR/android-sdk"
    mkdir -p "$CACHE_DIR/android-sdk"
    tar -xzf "$JDK_ARCHIVE" -C "$CACHE_DIR"
    tar -xJf "$PLATFORM_ARCHIVE" -C "$CACHE_DIR/android-sdk"
    tar -xJf "$BUILD_TOOLS_ARCHIVE" -C "$CACHE_DIR/android-sdk"
    install -m 0644 "$FRAMEWORK_APK" \
        "$CACHE_DIR/android-sdk/platforms/android-${ANDROID_COMPILE_SDK}/framework-res.apk"
    find "$CACHE_DIR" -maxdepth 1 -name '.prepared-*' -delete
    : > "$marker"
fi

JAVA_HOME_PATH="$CACHE_DIR/jdk17-gradle"
SDK_ROOT_PATH="$CACHE_DIR/android-sdk"
GRADLE_HOME_PATH="$TOOL_DIR/gradle/gradle-${GRADLE_VERSION}"

for executable in \
    "$JAVA_HOME_PATH/bin/java" \
    "$JAVA_HOME_PATH/bin/javac" \
    "$GRADLE_HOME_PATH/bin/gradle" \
    "$SDK_ROOT_PATH/build-tools/${ANDROID_BUILD_TOOLS_VERSION}/aapt2" \
    "$SDK_ROOT_PATH/build-tools/${ANDROID_BUILD_TOOLS_VERSION}/d8" \
    "$SDK_ROOT_PATH/build-tools/${ANDROID_BUILD_TOOLS_VERSION}/apksigner" \
    "$SDK_ROOT_PATH/build-tools/${ANDROID_BUILD_TOOLS_VERSION}/zipalign"; do
    if [[ ! -x "$executable" ]]; then
        echo "Toolchain executable is absent or not executable: $executable" >&2
        exit 1
    fi
done

test -s "$SDK_ROOT_PATH/platforms/android-${ANDROID_COMPILE_SDK}/android.jar"
test -s "$SDK_ROOT_PATH/platforms/android-${ANDROID_COMPILE_SDK}/framework-res.apk"
test -d "$SDK_ROOT_PATH/platforms/android-${ANDROID_COMPILE_SDK}/data/res"

if [[ "${1:-}" == --print-env ]]; then
    printf 'export JAVA_HOME=%q\n' "$JAVA_HOME_PATH"
    printf 'export ANDROID_HOME=%q\n' "$SDK_ROOT_PATH"
    printf 'export ANDROID_SDK_ROOT=%q\n' "$SDK_ROOT_PATH"
    printf 'export GRADLE_HOME=%q\n' "$GRADLE_HOME_PATH"
    printf 'export PATH=%q\n' "$JAVA_HOME_PATH/bin:$GRADLE_HOME_PATH/bin:$SDK_ROOT_PATH/build-tools/${ANDROID_BUILD_TOOLS_VERSION}:$PATH"
else
    echo "Offline toolchain is ready:"
    echo "  JAVA_HOME=$JAVA_HOME_PATH"
    echo "  ANDROID_SDK_ROOT=$SDK_ROOT_PATH"
    echo "  GRADLE_HOME=$GRADLE_HOME_PATH"
fi
