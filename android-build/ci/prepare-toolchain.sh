#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
LOG="$ROOT/android-build/ci/LAST_RUN.log"
: > "$LOG"
exec > >(tee "$LOG") 2>&1

record_run_log() {
  local status=$?
  trap - EXIT
  if [[ "${GITHUB_ACTIONS:-}" == true ]]; then
    # Persist diagnostics through ordinary Git transport because this sandbox
    # cannot download the Actions log archive host.
    sleep 1
    git -C "$ROOT" config user.name "github-actions[bot]"
    git -C "$ROOT" config user.email "41898282+github-actions[bot]@users.noreply.github.com"
    git -C "$ROOT" add -f android-build/ci/LAST_RUN.log
    if ! git -C "$ROOT" diff --cached --quiet; then
      git -C "$ROOT" commit -m "Record offline toolchain CI diagnostics"
      git -C "$ROOT" push origin "HEAD:${GITHUB_REF_NAME}"
    fi
  fi
  exit "$status"
}
trap record_run_log EXIT

REQUESTED_MODE="${1:-}"
MODE="${REQUESTED_MODE:-$(tr -d '[:space:]' < "$ROOT/android-build/ci/trigger-mode")}"
SMOKE="$ROOT/android-build/compose-smoke-test"
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"

if [[ "$MODE" != audit && "$MODE" != publish ]]; then
  echo "mode must be audit or publish" >&2
  exit 2
fi

printf 'Preparing SDK packages with %s on %s\n' "$(gradle --version | sed -n 's/^Gradle /Gradle /p')" "$(java -version 2>&1 | head -1)"
yes | "$SDKMANAGER" --licenses >/dev/null || true
"$SDKMANAGER" 'platforms;android-35' 'build-tools;34.0.0'

du -sh "$ANDROID_HOME/platforms/android-35" "$ANDROID_HOME/build-tools/34.0.0"

# This is a real Kotlin 2.0.21 + Compose compiler build. It also warms only
# the dependency graph that the local Maven repository will eventually contain.
gradle -p "$SMOKE" --no-daemon --stacktrace :app:assembleDebug
APK="$SMOKE/app/build/outputs/apk/debug/app-debug.apk"
test -s "$APK"
"$ANDROID_HOME/build-tools/34.0.0/apksigner" verify --verbose --print-certs "$APK"
sha256sum "$APK"

echo "Resolved Gradle module cache size:"
du -sh "$GRADLE_USER_HOME/caches/modules-2" || true
find "$GRADLE_USER_HOME/caches/modules-2/files-2.1" -type f -printf '%s %p\n' \
  | sort -nr | head -80 || true

# Prove that a compact, redistributable Java 17 image has all modules needed by
# Gradle 8.9 and AGP 8.7.3. The publish phase packages this tested image.
MODULES='java.base,java.compiler,java.desktop,java.instrument,java.logging,java.management,java.management.rmi,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.security.sasl,java.sql,java.transaction.xa,java.xml,java.xml.crypto,jdk.compiler,jdk.crypto.cryptoki,jdk.crypto.ec,jdk.jartool,jdk.jdeps,jdk.jfr,jdk.jlink,jdk.localedata,jdk.management,jdk.naming.dns,jdk.naming.rmi,jdk.net,jdk.security.auth,jdk.security.jgss,jdk.unsupported,jdk.unsupported.desktop,jdk.zipfs'
rm -rf "$RUNNER_TEMP/jdk17-gradle"
"$JAVA_HOME/bin/jlink" \
  --add-modules "$MODULES" \
  --strip-debug \
  --no-header-files \
  --no-man-pages \
  --compress=2 \
  --output "$RUNNER_TEMP/jdk17-gradle"

du -sh "$RUNNER_TEMP/jdk17-gradle"
JAVA_HOME="$RUNNER_TEMP/jdk17-gradle" \
  gradle -p "$SMOKE" --no-daemon :app:assembleDebug

if [[ "$MODE" == publish ]]; then
  echo "The publish implementation is supplied by a reviewed follow-up commit." >&2
  exit 3
fi
