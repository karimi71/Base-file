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
  if [[ "${GITHUB_ACTIONS:-}" == true && "${PUBLISH_SUCCEEDED:-0}" != 1 ]]; then
    # Persist diagnostics through ordinary Git transport because this sandbox
    # cannot download the Actions log archive host. A successful publish has a
    # concise committed verification report instead, so its live log is skipped.
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
MODE="${REQUESTED_MODE:-$(sed 's/#.*//' "$ROOT/android-build/ci/trigger-mode" | tr -d '[:space:]')}"
SMOKE="$ROOT/android-build/compose-smoke-test"
TIKARO_SMOKE="$ROOT/android-build/tikaro-stack-smoke-test"
PAPARAZZI_SMOKE="$ROOT/android-build/paparazzi-smoke-test"
QUALITY_SMOKE="$ROOT/android-build/quality-smoke-test"
FUTURE_SMOKE="$ROOT/android-build/future-stack-smoke-test"
TIKARO_REQUESTS="$TIKARO_SMOKE/REQUESTED_COORDINATES.tsv"
FUTURE_REQUESTS="$FUTURE_SMOKE/REQUESTED_COORDINATES.tsv"
FUTURE_CLASSIFIERS="$FUTURE_SMOKE/NATIVE_CLASSIFIERS.tsv"
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
# Resolve both the required debug graph and release-only AGP tooling (notably
# lint vital) before converting the fresh cache into the offline Maven layout.
gradle -p "$SMOKE" --no-daemon --stacktrace :app:assembleDebug :app:assembleRelease
APK="$SMOKE/app/build/outputs/apk/debug/app-debug.apk"
test -s "$APK"
"$ANDROID_HOME/build-tools/34.0.0/apksigner" verify --verbose --print-certs "$APK"
sha256sum "$APK"

# Resolve and execute the pinned Tikaro application, test, screenshot, benchmark,
# and build-quality graphs. --write-locks records the exact family-wide choices
# that the subsequent clean-room builds must reproduce without a network.
gradle -p "$TIKARO_SMOKE" --no-daemon --stacktrace --write-locks \
  :app:assembleDebug \
  :app:assembleRelease \
  :app:testDebugUnitTest \
  :app:assembleDebugAndroidTest \
  :benchmark:assembleNonMinifiedBenchmark
gradle -p "$PAPARAZZI_SMOKE" --no-daemon --stacktrace --write-locks \
  :screenshot:testDebugUnitTest
gradle -p "$QUALITY_SMOKE" --no-daemon --stacktrace --write-locks \
  classes resolveQualityTools verifyLicenseReport

# Resolve every future coordinate in isolation, execute host-capable behavior,
# compile Android device tests, run R8, and record deterministic Roborazzi goldens.
gradle -p "$FUTURE_SMOKE" --no-daemon --stacktrace --write-locks \
  resolveFutureCoordinates \
  generateDependencyReports \
  :jvm:test \
  :jvm:verifyGeneratedSources \
  :android:assembleDebug \
  :android:assembleRelease \
  :android:testDebugUnitTest \
  :android:assembleDebugAndroidTest \
  :roborazzi:recordRoborazziDebug

test -s "$TIKARO_SMOKE/app/build/outputs/apk/debug/app-debug.apk"
test -s "$TIKARO_SMOKE/app/build/outputs/apk/release/app-release-unsigned.apk"
find "$TIKARO_SMOKE/app/build/outputs/apk/androidTest" -type f -name '*.apk' -size +0c -print -quit \
  | grep -q .
find "$TIKARO_SMOKE/benchmark/build/outputs/apk" -type f -name '*.apk' -size +0c -print -quit \
  | grep -q .
test -d "$PAPARAZZI_SMOKE/screenshot/build/reports/paparazzi"
test -s "$FUTURE_SMOKE/android/build/outputs/apk/debug/android-debug.apk"
test -s "$FUTURE_SMOKE/android/build/outputs/apk/release/android-release-unsigned.apk"
test -s "$FUTURE_SMOKE/android/build/outputs/mapping/release/mapping.txt"
find "$FUTURE_SMOKE/android/build/outputs/apk/androidTest" \
  -type f -name '*.apk' -size +0c -print -quit | grep -q .
find "$FUTURE_SMOKE/roborazzi/src/test/snapshots" \
  -type f -name '*.png' -size +0c | awk 'END { exit(NR >= 6 ? 0 : 1) }'

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
  echo "Packaging the audited toolchain for repository publication"
  # shellcheck disable=SC1091
  source "$ROOT/android-build/versions.env"
  SOURCE_DATE_EPOCH=1735689600 # 2025-01-01T00:00:00Z
  export SOURCE_DATE_EPOCH

  GRADLE_BIN="$(readlink -f "$(command -v gradle)")"
  GRADLE_SOURCE="$(cd "$(dirname "$GRADLE_BIN")/.." && pwd)"
  GRADLE_DEST="$ROOT/android-build/gradle/gradle-$GRADLE_VERSION"
  test -s "$GRADLE_SOURCE/lib/gradle-launcher-$GRADLE_VERSION.jar"
  rm -rf "$ROOT/android-build/gradle"
  mkdir -p "$GRADLE_DEST"
  cp -a "$GRADLE_SOURCE/." "$GRADLE_DEST/"

  rm -rf "$ROOT/android-build/jdk" "$ROOT/android-build/sdk"
  mkdir -p "$ROOT/android-build/jdk" "$ROOT/android-build/sdk"
  tar --sort=name --mtime="@$SOURCE_DATE_EPOCH" --owner=0 --group=0 --numeric-owner \
    -C "$RUNNER_TEMP" -cf - jdk17-gradle \
    | gzip -9n > "$ROOT/android-build/jdk/openjdk17-gradle-linux-x64.tar.gz"
  tar --sort=name --mtime="@$SOURCE_DATE_EPOCH" --owner=0 --group=0 --numeric-owner \
    -C "$ANDROID_HOME" -cJf "$ROOT/android-build/sdk/android-sdk-platform-35.tar.xz" \
    platforms/android-35 licenses
  tar --sort=name --mtime="@$SOURCE_DATE_EPOCH" --owner=0 --group=0 --numeric-owner \
    -C "$ANDROID_HOME" -cJf "$ROOT/android-build/sdk/android-sdk-build-tools-34.0.0.tar.xz" \
    build-tools/34.0.0

  cp "$ANDROID_HOME/platforms/android-35/android.jar" \
    "$ROOT/android-build/sdk/framework-res-api35.apk"
  zip -q -d "$ROOT/android-build/sdk/framework-res-api35.apk" \
    '*.class' 'META-INF/*' 'NOTICES/*'
  "$ANDROID_HOME/build-tools/34.0.0/aapt2" dump resources \
    "$ROOT/android-build/sdk/framework-res-api35.apk" >/dev/null

  python3 "$ROOT/android-build/ci/cache_to_maven.py" \
    "$GRADLE_USER_HOME/caches/modules-2/files-2.1" \
    "$ROOT/android-build/maven"
  python3 "$ROOT/android-build/ci/verify_requested_coordinates.py" \
    "$TIKARO_REQUESTS" \
    "$ROOT/android-build/maven/BASE_FILE_COORDINATES.tsv"
  python3 "$ROOT/android-build/ci/verify_requested_coordinates.py" \
    "$FUTURE_REQUESTS" \
    "$ROOT/android-build/maven/BASE_FILE_COORDINATES.tsv"
  mapfile -t future_locks < <(find "$FUTURE_SMOKE" -name gradle.lockfile -type f | LC_ALL=C sort)
  python3 "$ROOT/android-build/ci/verify_future_coordinates.py" \
    "$FUTURE_REQUESTS" \
    "$FUTURE_CLASSIFIERS" \
    "$ROOT/android-build/maven/BASE_FILE_COORDINATES.tsv" \
    "$ROOT/android-build/maven" \
    "${future_locks[@]}"
  for sqlite_artifact in sqlite sqlite-framework; do
    grep -q $'^androidx.sqlite\t'"$sqlite_artifact"$'\t' \
      "$ROOT/android-build/maven/BASE_FILE_COORDINATES.tsv" || {
        echo "Room-selected transitive is missing: androidx.sqlite:$sqlite_artifact" >&2
        exit 1
      }
  done
  python3 "$ROOT/android-build/ci/generate_legal_inventory.py" \
    "$ROOT/android-build/maven" \
    "$ROOT/android-build/licenses/MAVEN_ARTIFACTS.tsv" \
    "$ROOT/android-build/licenses/THIRD_PARTY_NOTICES.txt"

  # The official Robolectric API-35 runtime is about 199 MiB. Preserve it
  # losslessly as Git-safe 64 MiB chunks, with original size and SHA-256 in a
  # committed reconstruction manifest. prepare-offline-toolchain restores it.
  python3 "$ROOT/android-build/ci/split_maven_artifacts.py" split \
    "$ROOT/android-build/maven"

  chmod +x \
    "$ROOT/android-build/build-compose-apk.sh" \
    "$ROOT/android-build/prepare-offline-toolchain.sh" \
    "$ROOT/android-build/update-added-files.sh" \
    "$ROOT/android-build/update-checksums.sh" \
    "$ROOT/android-build/verify-offline-toolchain.sh" \
    "$ROOT/android-build/verify-tikaro-stack.sh" \
    "$ROOT/android-build/verify-future-stack.sh" \
    "$ROOT/android-build/ci/split_maven_artifacts.py" \
    "$ROOT/android-build/ci/verify_future_coordinates.py" \
    "$ROOT/android-build/kotlin/bin/kotlinc" \
    "$ROOT/android-build/kotlin/bin/kotlinc-compose" \
    "$ROOT/android-build/gradle/gradle-$GRADLE_VERSION/bin/gradle"

  echo "Largest repository artifacts before publication:"
  find "$ROOT/android-build" -type f \
    ! -path '*/compose-smoke-test/*/build/*' \
    ! -path '*/tikaro-stack-smoke-test/*/build/*' \
    ! -path '*/paparazzi-smoke-test/*/build/*' \
    ! -path '*/future-stack-smoke-test/*/build/*' \
    ! -path '*/quality-smoke-test/build/*' \
    -printf '%s %p\n' | sort -nr | head -30 || true
  if find "$ROOT/android-build" -type f -size +95M \
    ! -path '*/compose-smoke-test/*/build/*' \
    ! -path '*/tikaro-stack-smoke-test/*/build/*' \
    ! -path '*/paparazzi-smoke-test/*/build/*' \
    ! -path '*/future-stack-smoke-test/*/build/*' \
    ! -path '*/quality-smoke-test/build/*' \
    -print -quit | grep -q .; then
    echo "A generated repository file exceeds the conservative 95 MiB GitHub limit:" >&2
    find "$ROOT/android-build" -type f -size +95M \
      ! -path '*/compose-smoke-test/*/build/*' \
      ! -path '*/tikaro-stack-smoke-test/*/build/*' \
      ! -path '*/paparazzi-smoke-test/*/build/*' \
      ! -path '*/future-stack-smoke-test/*/build/*' \
      ! -path '*/quality-smoke-test/build/*' -print >&2
    exit 1
  fi

  echo "Starting clean-room offline verification from packaged files"
  rm -rf "$SMOKE/.gradle" "$SMOKE/app/build" \
    "$RUNNER_TEMP/published-cache" "$RUNNER_TEMP/offline-gradle-home" \
    "$RUNNER_TEMP/offline-future-gradle-home"
  export BASE_FILE_CACHE="$RUNNER_TEMP/published-cache"
  export BASE_FILE_GRADLE_USER_HOME="$RUNNER_TEMP/offline-gradle-home"
  export BASE_FILE_FUTURE_GRADLE_USER_HOME="$RUNNER_TEMP/offline-future-gradle-home"
  "$ROOT/android-build/prepare-offline-toolchain.sh"
  "$ROOT/android-build/kotlin/bin/kotlinc" -version
  "$ROOT/android-build/build-compose-apk.sh" "$SMOKE"
  DEBUG_APK="$SMOKE/app/build/outputs/apk/debug/app-debug.apk"
  test -s "$DEBUG_APK"
  unzip -l "$DEBUG_APK" | grep 'META-INF/androidx.compose.runtime_runtime.version'

  echo "Running Tikaro stack, Paparazzi, benchmark, and quality checks offline"
  "$ROOT/android-build/verify-tikaro-stack.sh"
  TIKARO_DEBUG_APK="$TIKARO_SMOKE/app/build/outputs/apk/debug/app-debug.apk"
  TIKARO_DEBUG_SHA="$(sha256sum "$TIKARO_DEBUG_APK" | cut -d' ' -f1)"

  echo "Running isolated future-stack behavior, R8, Roborazzi, and license checks offline"
  "$ROOT/android-build/verify-future-stack.sh"
  FUTURE_DEBUG_APK="$FUTURE_SMOKE/android/build/outputs/apk/debug/android-debug.apk"
  FUTURE_RELEASE_APK="$FUTURE_SMOKE/android/build/outputs/apk/release/android-release-unsigned.apk"
  FUTURE_DEBUG_SHA="$(sha256sum "$FUTURE_DEBUG_APK" | cut -d' ' -f1)"
  FUTURE_RELEASE_SHA="$(sha256sum "$FUTURE_RELEASE_APK" | cut -d' ' -f1)"

  # Build an unsigned release, align it, and explicitly sign it with apksigner.
  # The random key and password remain under RUNNER_TEMP and are destroyed.
  "$ROOT/android-build/build-compose-apk.sh" "$SMOKE" --task :app:assembleRelease
  UNSIGNED_APK="$SMOKE/app/build/outputs/apk/release/app-release-unsigned.apk"
  SIGNING_DIR="$RUNNER_TEMP/base-file-ephemeral-signing"
  rm -rf "$SIGNING_DIR"
  mkdir -m 0700 "$SIGNING_DIR"
  SIGNING_PASSWORD="$(openssl rand -hex 24)"
  export SIGNING_PASSWORD
  "$BASE_FILE_CACHE/jdk17-gradle/bin/keytool" -genkeypair -noprompt \
    -keystore "$SIGNING_DIR/test.jks" -storepass:env SIGNING_PASSWORD \
    -keypass:env SIGNING_PASSWORD -alias offline-test -keyalg RSA -keysize 2048 \
    -validity 1 -dname 'CN=Ephemeral Base-file CI Test,O=Base-file,C=NO'
  BUILDTOOLS="$BASE_FILE_CACHE/android-sdk/build-tools/$ANDROID_BUILD_TOOLS_VERSION"
  "$BUILDTOOLS/zipalign" -f -p 4 "$UNSIGNED_APK" "$SIGNING_DIR/aligned.apk"
  "$BUILDTOOLS/apksigner" sign \
    --ks "$SIGNING_DIR/test.jks" --ks-key-alias offline-test \
    --ks-pass env:SIGNING_PASSWORD --key-pass env:SIGNING_PASSWORD \
    --out "$SIGNING_DIR/compose-smoke-signed.apk" "$SIGNING_DIR/aligned.apk"
  SIGN_VERIFY_OUTPUT="$($BUILDTOOLS/apksigner verify --verbose --print-certs \
    "$SIGNING_DIR/compose-smoke-signed.apk")"
  printf '%s\n' "$SIGN_VERIFY_OUTPUT"
  SIGNER_DIGEST="$(printf '%s\n' "$SIGN_VERIFY_OUTPUT" | sed -n \
    's/^Signer #1 certificate SHA-256 digest: //p' | head -1)"
  DEBUG_SHA="$(sha256sum "$DEBUG_APK" | cut -d' ' -f1)"
  SIGNED_SHA="$(sha256sum "$SIGNING_DIR/compose-smoke-signed.apk" | cut -d' ' -f1)"
  REQUESTED_COUNT="$(tail -n +2 "$TIKARO_REQUESTS" | wc -l | tr -d ' ')"
  FUTURE_REQUESTED_COUNT="$(tail -n +2 "$FUTURE_REQUESTS" | wc -l | tr -d ' ')"
  MAVEN_COORDINATE_COUNT="$(tail -n +2 "$ROOT/android-build/maven/BASE_FILE_COORDINATES.tsv" | wc -l | tr -d ' ')"
  ROBORAZZI_GOLDEN_COUNT="$(find "$FUTURE_SMOKE/roborazzi/src/test/snapshots" -type f -name '*.png' | wc -l | tr -d ' ')"
  rm -rf "$SIGNING_DIR"
  unset SIGNING_PASSWORD

  cat > "$ROOT/android-build/VERIFICATION.md" <<EOF
# Offline verification report

- GitHub Actions run: ${GITHUB_SERVER_URL}/${GITHUB_REPOSITORY}/actions/runs/${GITHUB_RUN_ID}
- Source revision: ${GITHUB_SHA}
- Runner: ubuntu-24.04 / Linux x86_64
- Gradle: $GRADLE_VERSION
- Android Gradle Plugin: $ANDROID_GRADLE_PLUGIN_VERSION
- Kotlin and K2 Compose/serialization plugins: $KOTLIN_VERSION
- KSP / Room / DataStore: $KSP_VERSION / $ROOM_VERSION / $DATASTORE_VERSION
- Coroutines / Navigation / WorkManager / Glance: $COROUTINES_VERSION / $NAVIGATION_VERSION / $WORK_VERSION / $GLANCE_VERSION
- Compose BOM: $COMPOSE_BOM_VERSION (UI/Foundation/Runtime 1.7.6; Material3 1.3.1)
- Android SDK: Platform 35; Build Tools $ANDROID_BUILD_TOOLS_VERSION
- Dependency mode: local file Maven repository plus Gradle \`--offline\`, starting with an empty Gradle user home
- Pinned Tikaro direct coordinates: $REQUESTED_COUNT; isolated future coordinates: $FUTURE_REQUESTED_COUNT; complete selected Maven graph: $MAVEN_COORDINATE_COUNT coordinates
- Future pins: SQLite $SQLITE_VERSION; protobuf/protoc $PROTOBUF_VERSION; MockK $MOCKK_VERSION; JUnit Jupiter $JUNIT_JUPITER_VERSION / Platform $JUNIT_PLATFORM_VERSION
- Rendering/image/security pins: Roborazzi $ROBORAZZI_VERSION; Coil $COIL_VERSION; Tink $TINK_VERSION; PDFBox Android $PDFBOX_ANDROID_VERSION

## Results

1. The minimal Kotlin/Jetpack Compose fixture produced a clean debug APK.
2. The Tikaro stack fixture compiled real Room entities/DAO/database through KSP,
   Preferences DataStore, kotlinx.serialization, Navigation Compose, WorkManager,
   Glance, stable Biometric, DocumentFile, and ExifInterface into debug and release.
3. Tikaro JVM tests executed with Coroutines Test, Truth, and Turbine. Its AndroidX
   Test/Compose/Espresso/UI Automator APK and Macrobenchmark APK were compiled;
   device-only tests were not executed on the host-only runner.
4. Paparazzi $PAPARAZZI_VERSION remained intact and rendered its Compose test.
   Detekt, Ktlint, Dependency Analysis, and Gradle License Report $LICENSE_REPORT_VERSION
   loaded; the license task generated a non-empty report containing a real dependency.
5. The Tikaro release APK contains \`assets/dexopt/baseline.prof\`; the Benchmark
   and Baseline Profile plugin graph is available offline. Runtime profile capture
   itself requires a physical or managed Android device.
6. \`apksigner verify\` accepted both Gradle-signed debug APKs. A clean unsigned
   release APK was also aligned with \`zipalign\`, signed explicitly with a
   one-day ephemeral CI key, and verified.
7. Gradle lockfiles were generated online, then all fixtures were rebuilt with
   network repositories removed, \`--offline\`, and an initially empty cache.
8. The ephemeral JKS/password were deleted and never added to Git. Coordinate,
   SHA-256, provenance, license, and embedded NOTICE inventories were regenerated.
9. Proto DataStore executed migration, restart persistence, and corruption recovery;
   protoc generated lite Java/Kotlin sources and both Linux native classifiers resolved.
   Moshi KSP generated an adapter. Bundled SQLite executed transaction, persistence,
   uniqueness, foreign-key, and cascade behavior. JUnit 5, MockK, Kotest, jqwik,
   datetime serialization, Gson/Moshi, and Tink AEAD/keyset tests all ran on Java 17.
10. The isolated Android fixture built debug, minified release through R8, and a
    device-test APK with core desugaring, Coil modules, Security Crypto, PDFBox,
    extended Material icons, and WorkManager multiprocess. Instrumentation code was
    compiled but not device-executed on this host-only runner.
11. Roborazzi $ROBORAZZI_VERSION with Robolectric $ROBOLECTRIC_VERSION / API 35 native
    graphics recorded and verified $ROBORAZZI_GOLDEN_COUNT Compose goldens covering
    light, dark, RTL, large-font, and long-data variants. An intentional mismatch
    produced Grid-style comparison PNG/JSON/report evidence.
12. Host Android integration tests ran real ATF $ACCESSIBILITY_TEST_FRAMEWORK_VERSION
    checks and suppression, Room $ROOM_VERSION migration over SQLite framework
    $SQLITE_VERSION with constraints/cascade/downgrade rejection, local Coil decode,
    cache/cancellation/no-network behavior, PDF creation/load/render/password/corrupt
    handling, and authenticated preference migration.
13. Robolectric's official Android 15 runtime exceeded GitHub's single-file limit;
    it was losslessly split into committed chunks and was reconstructed with its
    original recorded SHA-256 before the empty-cache offline build.

Minimal Compose debug APK SHA-256 (CI output, not committed): \`$DEBUG_SHA\`

Tikaro stack debug APK SHA-256 (CI output, not committed): \`$TIKARO_DEBUG_SHA\`

Future fixture debug APK SHA-256 (CI output, not committed): \`$FUTURE_DEBUG_SHA\`

Future fixture R8 release APK SHA-256 (CI output, not committed): \`$FUTURE_RELEASE_SHA\`

Explicitly signed test APK SHA-256 (CI output, not committed): \`$SIGNED_SHA\`

Ephemeral signer certificate SHA-256: \`$SIGNER_DIGEST\`
EOF

  # The mutable CI log and build outputs are intentionally excluded.
  "$ROOT/android-build/update-checksums.sh"
  (
    cd "$ROOT/android-build"
    sha256sum --check SHA256SUMS.txt
  )
  if git -C "$ROOT" ls-files | grep -Ei '\.(jks|keystore|p12|pem|key)$'; then
    echo "Signing material must not be tracked" >&2
    exit 1
  fi

  git -C "$ROOT" config user.name "github-actions[bot]"
  git -C "$ROOT" config user.email "41898282+github-actions[bot]@users.noreply.github.com"

  # Push in bounded groups so GitHub receives manageable packfiles.
  git -C "$ROOT" add android-build/gradle android-build/jdk android-build/sdk
  git -C "$ROOT" reset -- android-build/ci/LAST_RUN.log || true
  if ! git -C "$ROOT" diff --cached --quiet; then
    git -C "$ROOT" commit -m "Vendor Gradle, JDK 17 and complete Android SDK 35"
    git -C "$ROOT" push origin "HEAD:${GITHUB_REF_NAME}"
  fi

  # -f is deliberate: the Maven group com.android.tools.build maps to a path
  # segment named "build", which must never be confused with project output.
  git -C "$ROOT" add -f android-build/maven
  git -C "$ROOT" reset -- android-build/ci/LAST_RUN.log || true
  if ! git -C "$ROOT" diff --cached --quiet; then
    git -C "$ROOT" commit -m "Vendor exact offline AGP Kotlin and Compose Maven graph"
    git -C "$ROOT" push origin "HEAD:${GITHUB_REF_NAME}"
  fi

  # Stage generated lockfiles/reports first so the base-to-index inventory is
  # exact, then checksum the final inventory and restage both generated files.
  git -C "$ROOT" add android-build
  git -C "$ROOT" reset -- android-build/ci/LAST_RUN.log || true
  "$ROOT/android-build/update-added-files.sh"
  "$ROOT/android-build/update-checksums.sh"
  git -C "$ROOT" add android-build
  git -C "$ROOT" reset -- android-build/ci/LAST_RUN.log || true
  (
    cd "$ROOT/android-build"
    sha256sum --check SHA256SUMS.txt
  )
  if ! git -C "$ROOT" diff --cached --quiet; then
    git -C "$ROOT" commit -m "Verify and document the offline Tikaro dependency stack"
    git -C "$ROOT" push origin "HEAD:${GITHUB_REF_NAME}"
  fi

  PUBLISH_SUCCEEDED=1
  export PUBLISH_SUCCEEDED
  echo "Offline toolchain publication and clean-room verification succeeded."
fi
