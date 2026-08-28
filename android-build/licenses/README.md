# Licenses and notices

The build toolchain is an aggregation; no ownership of third-party components is
claimed. Each component remains under its upstream license.

- `Apache-2.0.txt` is the unmodified Apache License 2.0 text used by AndroidX,
  Kotlin, Android Gradle Plugin, and many transitive libraries.
- `MAVEN_ARTIFACTS.tsv` is generated from the exact local Maven repository. It
  records every coordinate, upstream POM license declaration, and binary file,
  including native executable classifiers such as `protoc`.
- `THIRD_PARTY_NOTICES.txt` contains unique `META-INF/NOTICE*` texts extracted
  verbatim from the vendored JAR/AAR files, with their source coordinate and
  SHA-256.
- Gradle's own `LICENSE` and `NOTICE` are retained inside
  `android-build/gradle/gradle-8.9/`.
- Android SDK package notices are retained inside the unmodified SDK archives;
  after extraction they are available in the SDK package directories.
- Kotlin compiler artifacts retain embedded `META-INF` license/notice metadata,
  and their POM license entry is included in `MAVEN_ARTIFACTS.tsv`.
- A GitHub-oversized Robolectric JAR is represented by lossless binary chunks;
  its coordinate/license is still recorded here, while the original byte size
  and SHA-256 are recorded in `maven/BASE_FILE_SPLIT_ARTIFACTS.tsv`.

The Android SDK is also subject to Google's Android SDK terms. Users must ensure
that their use and redistribution comply with those terms:
https://developer.android.com/studio/terms
