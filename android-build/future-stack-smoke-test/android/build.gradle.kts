plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("de.mannodermaus.android-junit5")
}

android {
    namespace = "dev.basefile.future.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.basefile.future.android"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    packaging {
        resources.excludes += setOf(
            "META-INF/DEPENDENCIES",
            "META-INF/INDEX.LIST",
            "META-INF/LICENSE.md",
            "META-INF/LICENSE-notice.md",
        )
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.7.6")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    implementation(platform("io.coil-kt.coil3:coil-bom:3.1.0"))
    implementation("io.coil-kt.coil3:coil:3.1.0")
    implementation("io.coil-kt.coil3:coil-compose:3.1.0")
    implementation("io.coil-kt.coil3:coil-svg:3.1.0")
    implementation("io.coil-kt.coil3:coil-gif:3.1.0")
    implementation("io.coil-kt.coil3:coil-video:3.1.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.1.0")

    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("androidx.work:work-multiprocess:2.10.0")
    implementation("androidx.sqlite:sqlite:2.5.2")
    implementation("androidx.sqlite:sqlite-ktx:2.5.2")
    implementation("androidx.sqlite:sqlite-framework:2.5.2")
    implementation("com.google.crypto.tink:tink-android:1.15.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.security:security-crypto-ktx:1.1.0-alpha06")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.4")
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")

    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-accessibility:3.6.1")
    androidTestImplementation(
        "com.google.android.apps.common.testing.accessibility.framework:" +
            "accessibility-test-framework:4.1.1",
    )
    androidTestImplementation("androidx.room:room-testing:2.7.2")
    androidTestImplementation("io.mockk:mockk-android:1.13.13")
    androidTestImplementation("io.mockk:mockk-agent-android:1.13.13")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
