#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Build a conventional :app Android project with the vendored toolchain only.
set -euo pipefail

TOOL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$TOOL_DIR/.." && pwd)"
# shellcheck disable=SC1091
source "$TOOL_DIR/versions.env"

usage() {
    cat <<'USAGE'
Usage: android-build/build-compose-apk.sh [PROJECT_DIR] [--task TASK]

PROJECT_DIR defaults to a sibling checkout named Animator. The default task is
:app:assembleDebug and the required output is:
  PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk

Examples:
  ./android-build/build-compose-apk.sh ../Animator
  ./android-build/build-compose-apk.sh android-build/compose-smoke-test
USAGE
}

PROJECT_DIR=""
TASK=":app:assembleDebug"
while (($#)); do
    case "$1" in
        --task)
            [[ $# -ge 2 ]] || { usage >&2; exit 2; }
            TASK="$2"
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        --*)
            echo "Unknown option: $1" >&2
            usage >&2
            exit 2
            ;;
        *)
            [[ -z "$PROJECT_DIR" ]] || { echo "Only one project directory is accepted" >&2; exit 2; }
            PROJECT_DIR="$1"
            shift
            ;;
    esac
done

if [[ -z "$PROJECT_DIR" ]]; then
    PROJECT_DIR="$REPO_ROOT/../Animator"
fi
if [[ ! -d "$PROJECT_DIR" ]]; then
    echo "Android project directory not found: $PROJECT_DIR" >&2
    exit 1
fi
PROJECT_DIR="$(cd "$PROJECT_DIR" && pwd)"
if [[ ! -f "$PROJECT_DIR/settings.gradle" && ! -f "$PROJECT_DIR/settings.gradle.kts" ]]; then
    echo "No settings.gradle(.kts) in $PROJECT_DIR" >&2
    exit 1
fi
if [[ ! -d "$PROJECT_DIR/app" ]]; then
    echo "Expected an app module at $PROJECT_DIR/app" >&2
    exit 1
fi

"$TOOL_DIR/prepare-offline-toolchain.sh" >/dev/null
CACHE_DIR="${BASE_FILE_CACHE:-$TOOL_DIR/.cache}"
export JAVA_HOME="$CACHE_DIR/jdk17-gradle"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="$CACHE_DIR/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
GRADLE="$TOOL_DIR/gradle/gradle-${GRADLE_VERSION}/bin/gradle"
MAVEN_REPO="$TOOL_DIR/maven"
INIT_SCRIPT="$TOOL_DIR/offline.init.gradle"

[[ -d "$MAVEN_REPO" ]] || { echo "Offline Maven repository is missing: $MAVEN_REPO" >&2; exit 1; }
[[ -x "$GRADLE" ]] || { echo "Vendored Gradle is missing: $GRADLE" >&2; exit 1; }

MAVEN_URI="$(python3 - "$MAVEN_REPO" <<'PY'
import pathlib, sys
print(pathlib.Path(sys.argv[1]).resolve().as_uri())
PY
)"

export GRADLE_USER_HOME="${BASE_FILE_GRADLE_USER_HOME:-$CACHE_DIR/gradle-home}"
mkdir -p "$GRADLE_USER_HOME"
# Keep the standard debug signing key outside both repositories, then destroy it.
ANDROID_USER_TEMP="$(mktemp -d "${TMPDIR:-/tmp}/base-file-android-user.XXXXXX")"
export ANDROID_USER_HOME="$ANDROID_USER_TEMP"
cleanup() {
    rm -rf "$ANDROID_USER_TEMP"
}
trap cleanup EXIT INT TERM

printf 'Building %s with Gradle %s, AGP %s, Kotlin %s, API %s (strictly offline)\n' \
    "$PROJECT_DIR" "$GRADLE_VERSION" "$ANDROID_GRADLE_PLUGIN_VERSION" \
    "$KOTLIN_VERSION" "$ANDROID_COMPILE_SDK"

"$GRADLE" \
    --offline \
    --no-daemon \
    --stacktrace \
    --init-script "$INIT_SCRIPT" \
    -Dbasefile.repo="$MAVEN_REPO" \
    -PbaseFileMaven="$MAVEN_URI" \
    -p "$PROJECT_DIR" \
    "$TASK"

APK="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
if [[ "$TASK" == *assembleDebug* ]]; then
    [[ -s "$APK" ]] || { echo "Expected APK was not produced: $APK" >&2; exit 1; }
    APKSIGNER="$ANDROID_HOME/build-tools/${ANDROID_BUILD_TOOLS_VERSION}/apksigner"
    ZIPALIGN="$ANDROID_HOME/build-tools/${ANDROID_BUILD_TOOLS_VERSION}/zipalign"
    "$ZIPALIGN" -c -p 4 "$APK"
    "$APKSIGNER" verify --verbose --print-certs "$APK"
    echo "APK: $APK"
    sha256sum "$APK"
fi
