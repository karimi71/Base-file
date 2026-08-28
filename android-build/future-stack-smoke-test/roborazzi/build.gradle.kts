plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("io.github.takahirom.roborazzi")
}

android {
    namespace = "dev.basefile.future.roborazzi"
    compileSdk = 35

    defaultConfig {
        minSdk = 23
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    sourceSets["androidTest"].assets.srcDir("$projectDir/schemas")
    packaging.resources.excludes += setOf(
        "META-INF/DEPENDENCIES",
        "META-INF/LICENSE.md",
        "META-INF/LICENSE-notice.md",
    )
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

roborazzi {
    outputDir.set(file("src/test/snapshots"))
    compare {
        outputDir.set(layout.buildDirectory.dir("outputs/roborazzi-compare"))
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.7.6")

    implementation(platform("io.coil-kt.coil3:coil-bom:3.1.0"))
    implementation("io.coil-kt.coil3:coil:3.1.0")
    implementation("io.coil-kt.coil3:coil-compose:3.1.0")
    implementation("io.coil-kt.coil3:coil-svg:3.1.0")
    implementation("io.coil-kt.coil3:coil-gif:3.1.0")
    implementation("io.coil-kt.coil3:coil-video:3.1.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.1.0")

    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    implementation("androidx.sqlite:sqlite-framework:2.5.2")
    ksp("androidx.room:room-compiler:2.7.2")
    implementation("com.google.crypto.tink:tink-android:1.15.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    testImplementation(composeBom)
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("androidx.test:runner:1.6.2")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.test.espresso:espresso-core:3.6.1")
    testImplementation("androidx.test.espresso:espresso-accessibility:3.6.1")
    testImplementation("androidx.room:room-testing:2.7.2")
    testImplementation("io.coil-kt.coil3:coil-test:3.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.39.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.39.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-junit-rule:1.39.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-accessibility-check:1.39.0")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testRuntimeOnly("org.robolectric:android-all-instrumented:15-robolectric-12650502-i7")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

tasks.withType<Test>().configureEach {
    maxHeapSize = "3072m"
    systemProperty("robolectric.usePreinstrumentedJars", "true")
    val localRepository = providers.gradleProperty("baseFileMaven").orNull
    if (localRepository != null) {
        systemProperty("robolectric.dependency.repo.id", "base-file-offline")
        systemProperty("robolectric.dependency.repo.url", localRepository)
    }
    systemProperty(
        "basefile.roborazzi.variant",
        System.getProperty("basefile.roborazzi.variant", "baseline"),
    )
    testLogging {
        events("passed", "skipped", "failed")
    }
}
