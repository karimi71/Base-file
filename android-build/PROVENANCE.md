# Offline Android toolchain provenance

All vendored files are produced by `ci/prepare-toolchain.sh` on a clean
`ubuntu-24.04` GitHub-hosted runner and then tested again with network dependency
resolution disabled. No generated signing key is copied into this repository.

| Component | Pinned version | Upstream channel |
|---|---:|---|
| Gradle binary distribution | 8.9 | `https://services.gradle.org/distributions/gradle-8.9-bin.zip` via `gradle/actions/setup-gradle` |
| Android Gradle Plugin | 8.7.3 | Google Maven |
| Kotlin compiler / Gradle plugin / Compose compiler plugin | 2.0.21 | Maven Central |
| Android SDK Platform | API 35 | Google's `sdkmanager` package `platforms;android-35` |
| Android SDK Build Tools | 34.0.0 | Google's `sdkmanager` package `build-tools;34.0.0` |
| Compose BOM | 2024.12.01 | Google Maven |
| KSP / Room / DataStore | 2.0.21-1.0.28 / 2.7.2 / 1.1.7 | Maven Central and Google Maven |
| Navigation / WorkManager / Glance | 2.8.9 / 2.10.0 / 1.1.1 | Google Maven |
| AndroidX Test / Benchmark / Baseline Profile | pinned in `tikaro-stack-smoke-test/REQUESTED_COORDINATES.tsv` | Google Maven |
| Paparazzi / Detekt / Ktlint / Dependency Analysis | pinned in `TIKARO_STACK.md` | Maven Central and Gradle Plugin Portal |
| SQLite / Proto DataStore / protobuf / protoc | pinned in `FUTURE_STACK.md` | Google Maven and Maven Central |
| MockK / JUnit 5 / Kotest / jqwik | pinned in `FUTURE_STACK.md` | Maven Central and Gradle Plugin Portal |
| Roborazzi / Robolectric Android 15 runtime | 1.39.0 / 4.14.1 | Maven Central and Gradle Plugin Portal |
| Coil / Tink / Security / PDFBox / datetime / Moshi / Gson | pinned in `FUTURE_STACK.md` | Google Maven and Maven Central |
| Gradle License Report | 2.9 | Maven Central and Gradle Plugin Portal |
| JDK used to run Gradle | compact OpenJDK/Temurin 17 image | generated with `jlink` from `actions/setup-java` Temurin 17 |

The local Maven repository is not a copy of a global Gradle cache. It is rebuilt
from a fresh `GRADLE_USER_HOME` after resolving only the committed base Compose,
Tikaro-stack, Paparazzi, and quality smoke projects. `ci/cache_to_maven.py`
converts those exact coordinates and their POM / Gradle Module Metadata into
Maven layout. This includes classifier filenames such as the two real native
`protoc` executables. A second build starts with separate empty Gradle user homes
for the essential and future fixtures and resolves exclusively from
`android-build/maven` with Gradle's `--offline` flag.

The official preinstrumented Robolectric Android-15 JAR is losslessly chunked to
respect GitHub's per-file limit. `maven/BASE_FILE_SPLIT_ARTIFACTS.tsv` records its
original path, exact byte count, SHA-256, and ordered parts. The preparation
entry point verifies every part and the reconstructed hash before use.

`SHA256SUMS.txt` records every committed file below `android-build/` except the
mutable CI diagnostic log and generated build/cache directories. The checksum
file therefore covers all archives, executables, JARs, AARs, POMs, module
metadata, scripts, notices, and source files that form the offline kit.
