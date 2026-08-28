# Offline verification report

- GitHub Actions run: https://github.com/karimi71/Base-file/actions/runs/33135578071
- Source revision: 1d08cb33d424995634c825e39126fd99dbc58ec2
- Runner: ubuntu-24.04 / Linux x86_64
- Gradle: 8.9
- Android Gradle Plugin: 8.7.3
- Kotlin and K2 Compose/serialization plugins: 2.0.21
- KSP / Room / DataStore: 2.0.21-1.0.28 / 2.7.2 / 1.1.7
- Coroutines / Navigation / WorkManager / Glance: 1.9.0 / 2.8.9 / 2.10.0 / 1.1.1
- Compose BOM: 2024.12.01 (UI/Foundation/Runtime 1.7.6; Material3 1.3.1)
- Android SDK: Platform 35; Build Tools 34.0.0
- Dependency mode: local file Maven repository plus Gradle `--offline`, starting with an empty Gradle user home
- Pinned Tikaro direct coordinates: 117; isolated future coordinates: 80; complete selected Maven graph: 1007 coordinates
- Future pins: SQLite 2.5.2; protobuf/protoc 4.29.3; MockK 1.13.13; JUnit Jupiter 5.11.4 / Platform 1.11.4
- Rendering/image/security pins: Roborazzi 1.39.0; Coil 3.1.0; Tink 1.15.0; PDFBox Android 2.0.27.0

## Results

1. The minimal Kotlin/Jetpack Compose fixture produced a clean debug APK.
2. The Tikaro stack fixture compiled real Room entities/DAO/database through KSP,
   Preferences DataStore, kotlinx.serialization, Navigation Compose, WorkManager,
   Glance, stable Biometric, DocumentFile, and ExifInterface into debug and release.
3. Tikaro JVM tests executed with Coroutines Test, Truth, and Turbine. Its AndroidX
   Test/Compose/Espresso/UI Automator APK and Macrobenchmark APK were compiled;
   device-only tests were not executed on the host-only runner.
4. Paparazzi 1.3.5 remained intact and rendered its Compose test.
   Detekt, Ktlint, Dependency Analysis, and Gradle License Report 2.9
   loaded; the license task generated a non-empty report containing a real dependency.
5. The Tikaro release APK contains `assets/dexopt/baseline.prof`; the Benchmark
   and Baseline Profile plugin graph is available offline. Runtime profile capture
   itself requires a physical or managed Android device.
6. `apksigner verify` accepted both Gradle-signed debug APKs. A clean unsigned
   release APK was also aligned with `zipalign`, signed explicitly with a
   one-day ephemeral CI key, and verified.
7. Gradle lockfiles were generated online, normalized against the final local
   Maven metadata, then all fixtures were rebuilt with network repositories removed,
   `--offline`, and an initially empty cache.
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
11. Roborazzi 1.39.0 with Robolectric 4.14.1 / API 35 native
    graphics recorded and verified 6 Compose goldens covering
    light, dark, RTL, large-font, and long-data variants. An intentional mismatch
    produced Grid-style comparison PNG/JSON/report evidence.
12. Host Android integration tests ran real ATF 4.1.1
    checks and suppression, Room 2.7.2 migration over SQLite framework
    2.5.2 with constraints/cascade/downgrade rejection, local Coil decode,
    cache/cancellation/no-network behavior, PDF creation/load/render/password/corrupt
    handling, and authenticated preference migration.
13. Robolectric's official Android 15 runtime exceeded GitHub's single-file limit;
    it was losslessly split into committed chunks and was reconstructed with its
    original recorded SHA-256 before the empty-cache offline build.

Minimal Compose debug APK SHA-256 (CI output, not committed): `8f266c4ee21e5dda620194e48f07607586ac3ef7a6056266947b78b632af9cf7`

Tikaro stack debug APK SHA-256 (CI output, not committed): `7f29173c5136259d51d3ffd8a721745b320ff6ccd3e2b6c6caa64af08c194c3d`

Future fixture debug APK SHA-256 (CI output, not committed): `9e67379bab3bf947f23f8cbfb1e8931a1579010b232a67f0ce8a79e8431c811d`

Future fixture R8 release APK SHA-256 (CI output, not committed): `eae6b951ef6b0d8516e723e7f0c90599e4bcac4a900eaf8bf9342b6629cb013d`

Explicitly signed test APK SHA-256 (CI output, not committed): `e1d824ac699672f38d12cb6a47e5f2ddfde6b27333f6883b543de194a246c737`

Ephemeral signer certificate SHA-256: `0fce15de0065423b9852f19251a0323ad5096356e6887c954c067b0eb4cb37b3`
