# Optional future Android/JVM stack

This layer vendors and tests future-facing capabilities independently of the
Tikaro application fixture. Nothing in this layer is a dependency of
`tikaro-stack-smoke-test`, and no `tikaro_app` directory is created or modified.
A future product module must opt into a library only after it has a measurable
use; merely being available in Base-file is not a reason to ship it in an APK.

## Compatibility baseline

All fixtures retain Gradle 8.9, AGP 8.7.3, Kotlin/K2 Compose 2.0.21, Compose BOM
2024.12.01, Java 17, compileSdk/targetSdk 35, and minSdk 23. The authoritative
pins are in `versions.env`; the direct-coordinate contract is
`future-stack-smoke-test/REQUESTED_COORDINATES.tsv`. Gradle lockfiles record the
exact selected variants and transitives for each resolvable configuration.
`DEPENDENCY_REPORT.tsv` and `VERSION_SELECTION_REPORT.tsv` are generated from
Gradle's real resolution result and record selected components, requested edges,
selection reasons, and conflict-resolution decisions.

## Included groups and evidence

| # | Capability and pin | Functional evidence |
|---:|---|---|
| 1 | `desugar_jdk_libs:2.1.5` | Android source uses `java.time` and streams; debug and R8 release APKs are built with core-library desugaring enabled. |
| 2 | SQLite `2.5.2` (`sqlite`, KTX, framework, bundled) | Bundled host driver executes a transaction, reopen/persistence, unique constraint, foreign key, and cascade test. Room integration uses the framework driver. |
| 3 | DataStore `1.1.7`, protobuf lite/protoc `4.29.3`, protobuf plugin/marker `0.9.4` | A real `.proto` generates lite Java and Kotlin. Proto DataStore executes migration, restart persistence, and corruption replacement. Both Linux x86_64 and aarch64 protoc classifiers resolve; the x86_64 binary executes `--version`. |
| 4 | Complete MockK `1.13.13` family | JVM agent mocking executes and Android agent/API/DSL artifacts compile into the instrumentation graph. Byte Buddy, its agent, Objenesis, and DexMaker are retained as selected transitives. |
| 5 | JUnit Jupiter `5.11.4`, Platform `1.11.4`, Android JUnit5 `1.11.2.0` | Jupiter, nested, parameterized, coroutine, and JUnit 4 Vintage tests execute; implementation and official marker are audited. |
| 6 | Roborazzi `1.39.0` | Compose captures execute with Robolectric 4.14.1, Android 15 SDK 35, and native graphics. Six golden variants, verify mode, intentional mismatch output, Grid comparison style, and reports are checked. Paparazzi 1.3.5 remains unchanged. |
| 7 | Coil 3 `3.1.0` | Core/Compose/SVG/GIF/video/network/test modules resolve. Host Android tests decode local SVG/GIF, prove a memory-cache hit and cancellation, and use a fake network-shaped request with zero network I/O. OkHttp/Okio are audited transitives. |
| 8 | Tink/Tink Android `1.15.0`, Security Crypto/KTX `1.1.0-alpha06` | JVM tests create and encrypt a persisted keyset and reject ciphertext/context tampering. A host Android test executes authenticated migration ordering; the Android source contains an `EncryptedSharedPreferences` migration entry point. Security Crypto remains fixture-only alpha software. |
| 9 | PDFBox Android `2.0.27.0` | API-35 host Android tests initialize resources, create/load/inspect/render a PDF, exercise password protection, reject truncated input, and execute framework `PdfDocument`. |
| 10 | kotlinx-datetime `0.6.1` metadata/JVM | Oslo DST arithmetic and kotlinx.serialization round-trip execute. |
| 11 | Moshi/core/Kotlin/codegen `1.15.2`, Gson `2.11.0` | KSP generates and compiles `GeneratedProfileJsonAdapter`; generated Moshi and Gson round-trips execute. |
| 12 | Material Icons Extended `1.7.6` | Extended PDF/calendar icons compile and render in Compose Roborazzi goldens and the Android APK. |
| 13 | WorkManager multiprocess `2.10.0` | A real `RemoteCoroutineWorker` and remote service process compile through debug/release/R8 and instrumentation packaging. IPC execution remains device-only. |
| 14 | Espresso accessibility `3.6.1`, ATF `4.1.1` | Roborazzi runs ATF after-test/manual checks under API 35 native graphics, including contrast/touch semantics and an explicit element suppression. Espresso accessibility also compiles into the device-test APK. |
| 15 | Kotest `5.9.1`, jqwik `1.9.2` | Kotest runs 300 generated values; jqwik uses a domain provider, fixed seed, 300 tries, and full shrinking mode. |
| 16 | Room `2.7.2` migration + SQLite framework `2.5.2` | A real version-1 framework database migrates through Room to generated schema 2; persistence, unique/foreign-key/cascade behavior and downgrade rejection execute. No destructive fallback API is enabled. |
| 17 | Gradle License Report `2.9` implementation/marker | `generateLicenseReport` runs and a verification task requires a non-empty report containing a real Gson runtime dependency. Detekt, Ktlint, and Dependency Analysis remain applied. |

## Host-executed versus device-only

Java/JVM and Robolectric tests above execute on the Linux x86_64 GitHub runner.
Robolectric accessibility checks deliberately use SDK 35 and native graphics;
Roborazzi itself skips ATF below API 34 or with legacy graphics.

The following Android-only paths are compiled but not claimed as executed:

- Espresso instrumentation against a device view hierarchy;
- Android multiprocess WorkManager IPC/service lifecycle;
- hardware-backed Android Keystore behavior;
- device-specific codec/video frame behavior.

`android:assembleDebugAndroidTest` must produce a non-empty test APK. The source
comments and final verification report preserve this distinction rather than
misrepresenting host compilation as device runtime evidence.

## Strict-offline contract

`verify-future-stack.sh` starts with a separate empty Gradle user home, injects
only `android-build/maven`, passes `--offline`, and runs catalog/classifier
resolution, code generation, JVM tests, Android debug/release/R8, instrumentation
compilation, Roborazzi verify/compare, and the license task. Network-capable Coil
classes are constructed only in fixtures; all image test data is local or fake.

The official preinstrumented Robolectric Android-15 JAR is larger than GitHub's
single-file limit. `ci/split_maven_artifacts.py` stores it as 64 MiB binary parts
and records the original relative path, byte size, and SHA-256 in
`maven/BASE_FILE_SPLIT_ARTIFACTS.tsv`. `prepare-offline-toolchain.sh` reconstructs
it byte-for-byte and rejects any size/hash mismatch before Gradle runs. All
committed parts are covered by `SHA256SUMS.txt`; this is lossless packaging, not a
placeholder or synthetic substitute.

Base-file supports Linux x86_64 as its executable build host. The additional
Linux aarch64 protoc classifier is included because it was explicitly requested.
macOS and Windows protoc classifiers are not included because the packaged JDK,
Gradle entry point, SDK tools, and verifier do not support those hosts.
