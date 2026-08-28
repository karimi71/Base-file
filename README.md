# Base-file — زنجیرهٔ build آفلاین Android/Compose

این مخزن یک زنجیرهٔ **واقعی و self-contained برای Linux x86_64** فراهم می‌کند تا
پروژهٔ Android مبتنی بر **Kotlin 2.0.21 + Jetpack Compose** بدون Android Studio و
بدون دانلود dependency در زمان build به APK تبدیل شود.

> ابزار build آفلاین فقط برای Linux x86_64 بسته‌بندی شده است. APK خروجی، مطابق
> معمول، روی ABI/APIهای تعریف‌شده در خود پروژه قابل اجراست.

## شروع سریع

ریپوی Animator را کنار Base-file checkout کنید:

```text
workspace/
├── Base-file/
└── Animator/
```

سپس:

```bash
cd Base-file
./android-build/build-compose-apk.sh ../Animator
```

اسکریپت فقط از فایل‌های داخل همین checkout استفاده می‌کند و Gradle را با
`--offline` اجرا می‌کند. خروجی مورد انتظار:

```text
../Animator/app/build/outputs/apk/debug/app-debug.apk
```

برای اجرای تست واقعی Compose موجود در همین مخزن:

```bash
./android-build/build-compose-apk.sh android-build/compose-smoke-test
```

برای بررسی checksumها، ابزارها، build تمیز تست Compose و امضای APK:

```bash
./android-build/verify-offline-toolchain.sh
# بررسی جامع Room/KSP/DataStore/Navigation/Test/Paparazzi/Benchmark:
./android-build/verify-tikaro-stack.sh
# بررسی قابلیت‌های future، R8، Roborazzi، Proto، crypto، PDF و license:
./android-build/verify-future-stack.sh
# فقط بررسی ابزارها و checksumها، بدون build:
./android-build/verify-offline-toolchain.sh --tools-only
```

## نسخه‌های قفل‌شده

| جزء | نسخه |
|---|---:|
| Gradle | 8.9 |
| Android Gradle Plugin | 8.7.3 |
| Kotlin compiler / Gradle plugin | 2.0.21 |
| Kotlin Compose compiler plugin (K2) | 2.0.21 |
| Compose BOM | 2024.12.01 |
| Compose UI / Runtime / Foundation / Animation | 1.7.6 |
| Compose Material3 | 1.3.1 |
| `activity-compose` | 1.10.0 |
| `lifecycle-runtime-ktx` | 2.8.7 |
| KSP / Room / DataStore | 2.0.21-1.0.28 / 2.7.2 / 1.1.7 |
| Coroutines / Navigation / WorkManager | 1.9.0 / 2.8.9 / 2.10.0 |
| Glance / Benchmark / Baseline Profile | 1.1.1 / 1.3.3 / 1.3.3 |
| SQLite / protobuf lite + protoc | 2.5.2 / 4.29.3 |
| MockK / JUnit 5 + Platform | 1.13.13 / 5.11.4 + 1.11.4 |
| Roborazzi / Coil 3 | 1.39.0 / 3.1.0 |
| Tink / PDFBox Android / kotlinx-datetime | 1.15.0 / 2.0.27.0 / 0.6.1 |
| Moshi / Gson / License Report | 1.15.2 / 2.11.0 / 2.9 |
| Android SDK Platform / compileSdk | 35 |
| Android SDK Build Tools | 34.0.0 |
| JVM اجرای Gradle/AGP | OpenJDK 17 compact image |

مقادیر machine-readable در `android-build/versions.env` قرار دارند.

### چرا API 35 و JDK 17؟

`androidx.activity:activity-compose:1.10.0` در AAR metadata حداقل compileSdk 35
را الزام می‌کند؛ بنابراین API 34 با dependencyهای دقیق درخواست‌شده build معتبر
نمی‌دهد. AGP 8.7.3 نیز Gradle 8.9 و JDK 17 را به‌عنوان ترکیب سازگار اعلام می‌کند.
Gradle 8.9 روی JDK 25 پشتیبانی نمی‌شود؛ به همین دلیل فایل قدیمی JDK 25 حفظ شده،
اما build Compose عمداً با image سازگار JDK 17 اجرا می‌شود.

## محتوای `android-build/`

```text
android-build/
├── build-compose-apk.sh              # entry point ساخت Animator یا هر پروژه :app
├── prepare-offline-toolchain.sh      # استخراج idempotent آرشیوهای SDK/JDK
├── verify-offline-toolchain.sh       # checksum + tools + clean Compose build
├── offline.init.gradle               # اجبار repository محلی
├── versions.env                      # نسخه‌های pinned
├── SHA256SUMS.txt                     # checksum تمام فایل‌های committed این پوشه
├── gradle/gradle-8.9/                 # distribution واقعی و کامل Gradle
├── jdk/openjdk17-gradle-linux-x64.tar.gz
├── sdk/
│   ├── android-sdk-platform-35.tar.xz
│   ├── android-sdk-build-tools-34.0.0.tar.xz
│   └── framework-res-api35.apk
├── kotlin/bin/
│   ├── kotlinc                        # CLI واقعی compiler-embeddable 2.0.21
│   └── kotlinc-compose                # همان compiler همراه K2 Compose plugin
├── maven/                             # Maven repository دقیق و حداقلی پروژه
├── compose-smoke-test/                # اپ پایهٔ واقعی Jetpack Compose
├── tikaro-stack-smoke-test/           # Room/KSP/DataStore/Navigation/Widget/Test
├── paparazzi-smoke-test/              # Golden screenshot روی JVM (حفظ شده)
├── future-stack-smoke-test/           # JVM/Android/R8/Roborazzi future fixtures
├── quality-smoke-test/                # Detekt/Ktlint/Dependency Analysis/License
├── TIKARO_STACK.md                    # محدودهٔ dependencyهای ضروری Tikaro
├── FUTURE_STACK.md                    # ۱۷ گروه اختیاری و شواهد تست واقعی
├── licenses/                          # Apache-2.0، inventory و NOTICEهای upstream
├── PROVENANCE.md
└── VERIFICATION.md
```

SDK پس از اولین اجرا در `android-build/.cache/android-sdk/` استخراج می‌شود و شامل
موارد زیر است:

- `platforms/android-35/android.jar`
- `platforms/android-35/framework-res.apk`
- `platforms/android-35/data/res/` و تمام framework resourceهای package رسمی
- `build-tools/34.0.0/aapt2`
- `build-tools/34.0.0/d8` و `lib/d8.jar`
- `build-tools/34.0.0/apksigner` و `lib/apksigner.jar`
- `build-tools/34.0.0/zipalign`
- سایر فایل‌های کامل Platform و Build Tools رسمی، از جمله AIDL و NOTICEها

استخراج فقط عملیات محلی `tar` است؛ هیچ downloader یا SDK Manager در مسیر build
مصرف‌کننده اجرا نمی‌شود.

## repository محلی Maven

`android-build/maven/` تنها graph لازم برای همین build را نگه می‌دارد، نه کل
Gradle cache. علاوه بر plugin markerها و dependencyهای build برای AGP 8.7.3 و
Kotlin 2.0.21، artifactهای runtime/compile موردنیاز این مجموعه موجودند:

- `androidx.activity` و `activity-compose:1.10.0`
- `androidx.lifecycle` و `lifecycle-runtime-ktx:2.8.7`
- `androidx.compose.runtime`
- `androidx.compose.ui`، graphics، text، unit، tooling-preview
- `androidx.compose.foundation` و foundation-layout
- `androidx.compose.animation`
- `androidx.compose.material` transitiveها و `androidx.compose.material3`
- `androidx.core`, `savedstate`, `collection`, `annotation`, `customview`
- `kotlinx.coroutines`
- `kotlin-stdlib:2.0.21`
- پشتهٔ ضروری تیکارو: KSP/Room/DataStore، Navigation، WorkManager، Glance،
  Biometric، Serialization، AndroidX/Compose Test، Benchmark، Paparazzi و
  ابزارهای کیفیت build-time
- لایهٔ future ایزوله: SQLite bundled/framework 2.5.2، Proto DataStore/protoc،
  MockK/JUnit 5، Roborazzi، Coil 3، Tink/Security، PDFBox، datetime، Moshi/Gson،
  icons-extended، Work multiprocess، accessibility/property tests و License Report
- تمام dependencyهای transitive انتخاب‌شده توسط Gradle Module Metadata/POM

محدودهٔ ضروری در `android-build/TIKARO_STACK.md` و قابلیت‌های اختیاری در
`android-build/FUTURE_STACK.md` مستند شده‌اند. فهرست directهای pinned در
`android-build/tikaro-stack-smoke-test/REQUESTED_COORDINATES.tsv` و
`android-build/future-stack-smoke-test/REQUESTED_COORDINATES.tsv` آمده است.
فهرست دقیق coordinateها و فایل‌های binary در
`android-build/licenses/MAVEN_ARTIFACTS.tsv` ثبت شده است.

JAR رسمی Android 15 برای Robolectric از محدودیت تک‌فایل GitHub بزرگ‌تر است؛ به
همین دلیل به قطعه‌های binary واقعی تقسیم و مسیر/اندازه/SHA-256 اصلی آن در
`maven/BASE_FILE_SPLIT_ARTIFACTS.tsv` ثبت می‌شود. اسکریپت آماده‌سازی آن را پیش از
build به‌صورت byte-for-byte بازسازی و hash را بررسی می‌کند؛ placeholder نیست.

## استفادهٔ مستقیم از Kotlin compiler

```bash
./android-build/kotlin/bin/kotlinc -version
./android-build/kotlin/bin/kotlinc Hello.kt -d hello.jar

# فعال‌کردن Compose compiler plugin سازگار با Kotlin 2.0.21:
./android-build/kotlin/bin/kotlinc-compose [kotlinc options ...]
```

برای ساخت اپ Android توصیه می‌شود از `build-compose-apk.sh` استفاده شود تا AGP،
resource merge، manifest merge، Compose compiler، D8، zipalign و signing همگی با
تنظیم سازگار اجرا شوند.

## قرارداد پروژهٔ Animator

اسکریپت یک پروژهٔ استاندارد Gradle با module به نام `app` را انتظار دارد. نسخه‌های
پروژه باید با جدول بالا سازگار باشند؛ نمونهٔ پایه در
`android-build/compose-smoke-test/` و نمونهٔ کامل‌تر تیکارو در
`android-build/tikaro-stack-smoke-test/` موجود است. حداقل نکات:

```kotlin
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
```

و در module:

```kotlin
android {
    compileSdk = 35
    buildFeatures { compose = true }
}

dependencies {
    val bom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(bom)
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
}
```

نیازی به حذف `google()` یا `mavenCentral()` از فایل‌های پروژه نیست؛ init script
مخزن محلی را تزریق می‌کند و `--offline` هرگونه resolve شبکه‌ای را ممنوع می‌کند.
البته custom Gradle taskهایی که خودشان عمداً HTTP اجرا می‌کنند خارج از کنترل
dependency resolver هستند و باید در پروژه غیرفعال شوند.

## تست Compose و امضا

تست smoke از APIهای واقعی زیر استفاده می‌کند:

- `ComponentActivity.setContent`
- `@Composable` و state از Compose Runtime
- Material3 `MaterialTheme`, `Surface`, `Button`, `Text`
- Foundation Layout
- Compose UI Graphics و `@Preview`

در clean-room CI ابتدا debug APK ساخته و با `apksigner verify` بررسی شده است. سپس
release unsigned APK با `zipalign` هم‌تراز، با `apksigner` و یک کلید تصادفی موقت
امضا، و دوباره verify شده است. JKS، alias password و فایل APK تست امضاشده پس از
تست حذف می‌شوند. نتیجه و hash همان اجرای CI در `android-build/VERIFICATION.md`
ثبت شده است.

## امنیت و signing

هیچ keystore، password، token، API key یا signing secret در این مخزن وجود ندارد.
برای debug build، کلید استاندارد debug در یک دایرکتوری موقت خارج از repo ساخته و
پس از پایان اسکریپت حذف می‌شود. برای release واقعی باید signing configuration را
در محیط امن مصرف‌کننده و خارج از Git تعریف کنید.

الگوهای `*.jks`, `*.keystore`, `*.p12`, `*.pem` و `*.key` نیز در `.gitignore`
مسدود شده‌اند.

## صحت، provenance و مجوزها

- `android-build/SHA256SUMS.txt`: SHA-256 تمام فایل‌های committed زیر
  `android-build/` (binary و text)، به‌جز log متغیر CI و output/cache.
- `android-build/ADDED_FILES.txt`: فهرست دقیق فایل‌های افزوده‌شده نسبت به commit
  مبنا (`a978940c92297269142c10614bc992793f8d788f`)، هر مسیر در یک سطر؛ این فایل
  هنگام انتشار به‌صورت خودکار بازتولید می‌شود.
- `android-build/PROVENANCE.md`: منبع و روش reproducible تهیهٔ هر جزء.
- `android-build/licenses/Apache-2.0.txt`: متن کامل Apache License 2.0.
- `android-build/licenses/MAVEN_ARTIFACTS.tsv`: coordinate و license هر artifact.
- `android-build/licenses/THIRD_PARTY_NOTICES.txt`: NOTICEهای embedded بدون تغییر.
- `LICENSE` و `NOTICE` رسمی Gradle داخل distribution حفظ شده‌اند.
- NOTICEها و package metadata رسمی Android SDK داخل آرشیوهای SDK حفظ شده‌اند.

مصرف‌کننده مسئول رعایت Android SDK Terms و مجوز هر dependency در محصول نهایی است.
