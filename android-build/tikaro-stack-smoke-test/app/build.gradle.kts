plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("androidx.baselineprofile")
}

android {
    namespace = "io.github.karimi71.basefile.tikaro"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.karimi71.basefile.tikaro"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = false
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    sourceSets["androidTest"].assets.srcDir("$projectDir/schemas")

    packaging {
        resources.excludes += setOf(
            "META-INF/AL2.0",
            "META-INF/LGPL2.1",
            "META-INF/LICENSE.md",
            "META-INF/LICENSE-notice.md"
        )
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core:1.15.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity:1.10.0")
    implementation("androidx.activity:activity-ktx:1.10.0")
    implementation("androidx.activity:activity-compose:1.10.0")

    implementation("androidx.lifecycle:lifecycle-runtime:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.8.7")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation("androidx.lifecycle:lifecycle-common:2.8.7")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata-core:2.8.7")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")

    implementation("androidx.savedstate:savedstate:1.2.1")
    implementation("androidx.savedstate:savedstate-ktx:1.2.1")

    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.runtime:runtime-saveable")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-text")
    implementation("androidx.compose.ui:ui-unit")
    implementation("androidx.compose.ui:ui-util")
    implementation("androidx.compose.ui:ui-geometry")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.foundation:foundation-layout")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.animation:animation-core")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-ripple")
    implementation("androidx.compose.material:material-icons-core")

    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")

    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.datastore:datastore-preferences-core:1.1.7")
    implementation("androidx.datastore:datastore-core:1.1.7")
    implementation("androidx.datastore:datastore-core-okio:1.1.7")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("androidx.navigation:navigation-compose:2.8.9")
    implementation("androidx.navigation:navigation-runtime:2.8.9")
    implementation("androidx.navigation:navigation-runtime-ktx:2.8.9")
    implementation("androidx.navigation:navigation-common:2.8.9")
    implementation("androidx.navigation:navigation-common-ktx:2.8.9")

    implementation("androidx.work:work-runtime:2.10.0")
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    implementation("androidx.glance:glance:1.1.1")
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")

    implementation("androidx.startup:startup-runtime:1.2.0")
    implementation("androidx.tracing:tracing:1.2.0")
    implementation("androidx.tracing:tracing-ktx:1.2.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.7.3")

    implementation("androidx.profileinstaller:profileinstaller:1.4.1")

    debugImplementation(composeBom)
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.hamcrest:hamcrest-core:1.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("com.google.truth:truth:1.4.4")
    testImplementation("app.cash.turbine:turbine:1.2.0")

    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test:1.7.6")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.7.6")
    androidTestImplementation("androidx.room:room-testing:2.7.2")
    androidTestImplementation("androidx.navigation:navigation-testing:2.8.9")
    androidTestImplementation("androidx.work:work-testing:2.10.0")
    androidTestImplementation("androidx.glance:glance-appwidget-testing:1.1.1")
    androidTestImplementation("androidx.glance:glance-testing:1.1.1")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:core-ktx:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test:monitor:1.7.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-idling-resource:3.6.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
