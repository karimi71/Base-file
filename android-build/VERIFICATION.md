# Offline verification report

- GitHub Actions run: https://github.com/karimi71/Base-file/actions/runs/32966199806
- Source revision: 43b73915f5f2953e17756e5c6c92730a81332c93
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

Debug APK SHA-256 (CI output, not committed): `76e83d5d504059ee1caf61defc85fe821ce2dcd4795b6aced69ed74f6001f267`

Explicitly signed test APK SHA-256 (CI output, not committed): `2ed5ee8488da9c1c8b41480c753e2780784c7669628aacbf3731f1409c7e0cc6`

Ephemeral signer certificate SHA-256: `8019b39a4e5e4dd52218afab9b9cb8af0e15042e13886bd27d7d9081c0b020da`
