plugins {
    kotlin("jvm") version "2.0.21"
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
    id("com.autonomousapps.dependency-analysis") version "2.2.0"
    id("com.github.jk1.dependency-license-report") version "2.9"
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
    implementation("com.google.code.gson:gson:2.11.0")
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

tasks.register("verifyLicenseReport") {
    group = "verification"
    dependsOn("generateLicenseReport")
    doLast {
        val reportDirectory = layout.buildDirectory.dir("reports/dependency-license").get().asFile
        val reports = reportDirectory.walkTopDown().filter { it.isFile && it.length() > 0 }.toList()
        check(reports.isNotEmpty()) { "Gradle License Report produced no non-empty report in $reportDirectory" }
        val reportText = reports
            .filter { it.extension in setOf("html", "json", "txt", "xml", "csv") }
            .joinToString("\n") { it.readText() }
        check(reportText.contains("gson", ignoreCase = true)) {
            "Generated license report did not inventory the real Gson runtime dependency"
        }
        println("Verified ${reports.size} generated dependency-license report files")
    }
}
