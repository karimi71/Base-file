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
| JDK used to run Gradle | compact OpenJDK/Temurin 17 image | generated with `jlink` from `actions/setup-java` Temurin 17 |

The local Maven repository is not a copy of a global Gradle cache. It is rebuilt
from a fresh `GRADLE_USER_HOME` after resolving only the committed Compose smoke
project. `ci/cache_to_maven.py` converts those exact coordinates and their POM /
Gradle Module Metadata into Maven layout. A second build starts with another
empty Gradle user home and resolves exclusively from `android-build/maven` with
Gradle's `--offline` flag.

`SHA256SUMS.txt` records every committed file below `android-build/` except the
mutable CI diagnostic log and generated build/cache directories. The checksum
file therefore covers all archives, executables, JARs, AARs, POMs, module
metadata, scripts, notices, and source files that form the offline kit.
