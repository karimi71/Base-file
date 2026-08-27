# Offline verification report

- GitHub Actions run: https://github.com/karimi71/Base-file/actions/runs/33087119777
- Source revision: 9d55d01e01c8796fb85998e859a935be4bc1e447
- Runner: ubuntu-24.04 / Linux x86_64
- Gradle: 8.9
- Android Gradle Plugin: 8.7.3
- Kotlin and K2 Compose/serialization plugins: 2.0.21
- KSP / Room / DataStore: 2.0.21-1.0.28 / 2.7.2 / 1.1.7
- Coroutines / Navigation / WorkManager / Glance: 1.9.0 / 2.8.9 / 2.10.0 / 1.1.1
- Compose BOM: 2024.12.01 (UI/Foundation/Runtime 1.7.6; Material3 1.3.1)
- Android SDK: Platform 35; Build Tools 34.0.0
- Dependency mode: local file Maven repository plus Gradle `--offline`, starting with an empty Gradle user home
- Pinned Tikaro direct coordinates: 117; complete selected Maven graph: 729 coordinates

## Results

1. The minimal Kotlin/Jetpack Compose fixture produced a clean debug APK.
2. The Tikaro stack fixture compiled real Room entities/DAO/database through KSP,
   Preferences DataStore, kotlinx.serialization, Navigation Compose, WorkManager,
   Glance, stable Biometric, DocumentFile, and ExifInterface into debug and release.
3. Tikaro JVM tests executed with Coroutines Test, Truth, and Turbine. Its AndroidX
   Test/Compose/Espresso/UI Automator APK and Macrobenchmark APK were compiled;
   device-only tests were not executed on the host-only runner.
4. Paparazzi 1.3.5 rendered the Compose screenshot test on the JVM,
   proving compatibility with AGP 8.7.3. Detekt, Ktlint,
   and Dependency Analysis plugins loaded, and their pinned engines resolved.
5. The Tikaro release APK contains `assets/dexopt/baseline.prof`; the Benchmark
   and Baseline Profile plugin graph is available offline. Runtime profile capture
   itself requires a physical or managed Android device.
6. `apksigner verify` accepted both Gradle-signed debug APKs. A clean unsigned
   release APK was also aligned with `zipalign`, signed explicitly with a
   one-day ephemeral CI key, and verified.
7. Gradle lockfiles were generated online, then all fixtures were rebuilt with
   network repositories removed, `--offline`, and an initially empty cache.
8. The ephemeral JKS/password were deleted and never added to Git. Coordinate,
   SHA-256, provenance, license, and embedded NOTICE inventories were regenerated.

Minimal Compose debug APK SHA-256 (CI output, not committed): `a96e4f90c5d29fd82e34457a9b1dc552702392e1cd986e20607f571b89772d87`

Tikaro stack debug APK SHA-256 (CI output, not committed): `526a7c47f243d96537cc16de4786d8f2dd30d53bcee551e1101731f96ff9c8f1`

Explicitly signed test APK SHA-256 (CI output, not committed): `3c2348b1a8811f0cdb6df3ee36f2917524d147723a876d826ff5091787993adc`

Ephemeral signer certificate SHA-256: `966ae9f059e0857c75f54f38e405d4164323cdabdcc8d5003f8e9b73b4bd0de7`
