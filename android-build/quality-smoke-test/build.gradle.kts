plugins {
    kotlin("jvm") version "2.0.21"
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
    id("com.autonomousapps.dependency-analysis") version "2.2.0"
}

kotlin {
    jvmToolchain(17)
}

dependencyLocking {
    lockAllConfigurations()
}

val tikaroQualityTools by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    tikaroQualityTools("io.gitlab.arturbosch.detekt:detekt-cli:1.23.7")
    tikaroQualityTools("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.7")
    tikaroQualityTools("com.pinterest.ktlint:ktlint-cli:1.3.1")
}

detekt {
    buildUponDefaultConfig = true
    ignoreFailures = true
}

ktlint {
    version.set("1.3.1")
    ignoreFailures.set(true)
}

tasks.register("resolveQualityTools") {
    inputs.files(tikaroQualityTools)
    doLast {
        val files = tikaroQualityTools.resolve()
        check(files.isNotEmpty()) { "Quality tooling configuration resolved no artifacts" }
        println("Resolved ${files.size} Detekt/KtLint quality-tool artifacts")
    }
}
