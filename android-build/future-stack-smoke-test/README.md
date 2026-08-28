# Future Tikaro offline stack fixtures

These fixtures validate optional future capabilities without adding any of them
to a Tikaro production APK:

- `jvm`: Proto DataStore/protoc, bundled SQLite, MockK, JUnit 5, property tests,
  kotlinx-datetime, Moshi/Gson, and Tink.
- `android`: core-library desugaring, Coil, AndroidX Security, PDFBox Android,
  extended Material icons, WorkManager multiprocess, Android JUnit 5, and R8.
- `roborazzi`: JVM Android integration tests for Compose visual regression,
  accessibility, persistence migrations, local image loading, and PDF handling.

`REQUESTED_COORDINATES.tsv` is the exact direct-coordinate contract. Every
fixture uses dependency locking and is rebuilt from an empty Gradle user home
against only `../maven` with Gradle `--offline` during publication.
