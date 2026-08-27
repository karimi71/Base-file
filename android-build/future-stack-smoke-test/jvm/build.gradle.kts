import com.google.protobuf.gradle.id

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.protobuf")
}

kotlin {
    jvmToolchain(17)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.29.3"
    }
    generateProtoTasks {
        all().configureEach {
            builtins {
                named("java") {
                    option("lite")
                }
                id("kotlin") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {
    implementation("androidx.datastore:datastore-core:1.1.7")
    implementation("androidx.datastore:datastore-core-okio:1.1.7")
    implementation("androidx.sqlite:sqlite:2.5.2")
    implementation("androidx.sqlite:sqlite-bundled:2.5.2")
    implementation("com.google.protobuf:protobuf-javalite:4.29.3")
    implementation("com.google.protobuf:protobuf-kotlin-lite:4.29.3")
    implementation("com.google.crypto.tink:tink:1.15.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.squareup.moshi:moshi:1.15.2")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.2")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.2")
    implementation("com.google.code.gson:gson:2.11.0")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("io.mockk:mockk-jvm:1.13.13")
    testImplementation("io.mockk:mockk-dsl-jvm:1.13.13")
    testImplementation("io.mockk:mockk-agent-jvm:1.13.13")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-property:5.9.1")
    testImplementation("net.jqwik:jqwik:1.9.2")
}

tasks.test {
    useJUnitPlatform()
    maxHeapSize = "1536m"
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.register("verifyGeneratedSources") {
    group = "verification"
    dependsOn("compileKotlin", "compileJava")
    doLast {
        val generated = listOf(
            layout.buildDirectory.file("generated/source/proto/main/java/dev/basefile/future/proto/TikaroSettings.java").get().asFile,
            layout.buildDirectory.file("generated/source/ksp/main/kotlin/dev/basefile/future/GeneratedProfileJsonAdapter.kt").get().asFile,
        )
        generated.forEach { output ->
            check(output.isFile && output.length() > 100) { "Expected generated source is absent: $output" }
        }
        println("Verified protobuf lite and Moshi KSP generated sources")
    }
}
