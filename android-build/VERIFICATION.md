# Offline verification report

- GitHub Actions run: https://github.com/karimi71/Base-file/actions/runs/32964884428
- Source revision: 16a5868fee37caa88c9f4b6d19c89cf103d5a0f3
- Runner: ubuntu-24.04 / Linux x86_64
- Gradle: 8.9
- Android Gradle Plugin: 8.7.3
- Kotlin and K2 Compose compiler plugin: 2.0.21
- Compose BOM: 2024.12.01 (UI/Foundation/Runtime 1.7.6; Material3 1.3.1)
- Android SDK: Platform 35; Build Tools 34.0.0
- Dependency mode: local file Maven repository plus Gradle `--offline`, using an empty Gradle user home

## Results

1. A clean Kotlin/Jetpack Compose debug APK was built successfully at
   `compose-smoke-test/app/build/outputs/apk/debug/app-debug.apk`.
2. `apksigner verify --verbose --print-certs` accepted the Gradle-signed debug APK.
3. A clean unsigned release APK was aligned with `zipalign`, signed explicitly
   by Build Tools `apksigner` using a one-day ephemeral CI key, and verified.
4. The ephemeral JKS and random password were deleted and were never added to Git.
5. The APK contains `META-INF/androidx.compose.runtime_runtime.version`, and the
   source uses Compose `setContent`, Material3, runtime state, layout, graphics,
   and `@Preview`; this is not an Android Views substitution.

Debug APK SHA-256 (CI output, not committed): `ba12f601488e649296e31ceaef57a729b294a6cfbb7f0f604415052a162d91de`

Explicitly signed test APK SHA-256 (CI output, not committed): `54212ae69fcb2041646db7ba3f9fd031aa50b002f35d1a51f81cc1b299c25f2c`

Ephemeral signer certificate SHA-256: `ff275b9c1e1816ecbbfdfac0986bcaaea6aaf48e1ad3fbd62376823275b349a5`
