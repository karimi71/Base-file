import org.gradle.api.artifacts.result.ResolvedDependencyResult

plugins {
    id("com.android.application") version "8.7.3" apply false
    id("com.android.library") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.jvm") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
    id("com.google.protobuf") version "0.9.4" apply false
    id("de.mannodermaus.android-junit5") version "1.11.2.0" apply false
    id("io.github.takahirom.roborazzi") version "1.39.0" apply false
}

allprojects {
    dependencyLocking {
        lockAllConfigurations()
    }
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            val pinned = when {
                requested.group == "androidx.sqlite" -> "2.5.2"
                requested.group == "androidx.datastore" -> "1.1.7"
                requested.group == "io.mockk" -> "1.13.13"
                requested.group in setOf("org.junit.jupiter", "org.junit.vintage") -> "5.11.4"
                requested.group == "org.junit.platform" -> "1.11.4"
                requested.group == "io.github.takahirom.roborazzi" -> "1.39.0"
                requested.group == "io.coil-kt.coil3" -> "3.1.0"
                requested.group == "com.google.crypto.tink" -> "1.15.0"
                requested.group == "androidx.security" -> "1.1.0-alpha06"
                requested.group == "com.squareup.moshi" -> "1.15.2"
                requested.group == "androidx.work" -> "2.10.0"
                requested.group == "androidx.room" -> "2.7.2"
                requested.group == "io.kotest" -> "5.9.1"
                requested.group == "org.jetbrains.kotlinx" &&
                    requested.name.startsWith("kotlinx-datetime") -> "0.6.1"
                requested.group == "com.google.protobuf" &&
                    requested.name != "protobuf-gradle-plugin" -> "4.29.3"
                requested.group == "com.google.code.gson" && requested.name == "gson" -> "2.11.0"
                requested.group == "androidx.compose.material" &&
                    requested.name.startsWith("material-icons-extended") -> "1.7.6"
                requested.group == "net.jqwik" && requested.name.startsWith("jqwik") -> "1.9.2"
                requested.group ==
                    "com.google.android.apps.common.testing.accessibility.framework" -> "4.1.1"
                requested.group == "androidx.test.espresso" &&
                    requested.name == "espresso-accessibility" -> "3.6.1"
                else -> null
            }
            if (pinned != null) {
                useVersion(pinned)
                because("Base-file future families are intentionally aligned")
            }
        }
    }
}

// Native classifiers have no ordinary JVM/Android variant, so resolve them in
// a dedicated configuration. All libraries resolve in correctly attributed JVM
// or Android fixture configurations (never one ambiguous mixed-platform graph).
val nativeClassifiers by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

fun readTsv(path: String): List<Map<String, String>> {
    val lines = file(path).readLines().filter { it.isNotBlank() }
    val headings = lines.first().split('\t')
    return lines.drop(1).map { line -> headings.zip(line.split('\t')).toMap() }
}

readTsv("NATIVE_CLASSIFIERS.tsv").forEach { row ->
    dependencies.add(
        nativeClassifiers.name,
        "${row.getValue("group")}:${row.getValue("artifact")}:${row.getValue("version")}:" +
            "${row.getValue("classifier")}@${row.getValue("extension")}",
    )
}

tasks.register("resolveFutureCoordinates") {
    group = "verification"
    description = "Resolves both pinned native protoc classifiers."
    inputs.files(nativeClassifiers)
    doLast {
        val nativeFiles = nativeClassifiers.files
        check(nativeFiles.map { it.name }.toSet().containsAll(
            setOf(
                "protoc-4.29.3-linux-x86_64.exe",
                "protoc-4.29.3-linux-aarch_64.exe",
            ),
        )) { "Both pinned Linux protoc classifiers must resolve: ${nativeFiles.map { it.name }}" }
        nativeFiles.forEach { executable ->
            check(executable.length() > 1_000_000) { "protoc artifact is not real: $executable" }
        }
        println("Resolved ${nativeFiles.size} pinned protoc executables")
    }
}

val dependencyReport = layout.projectDirectory.file("DEPENDENCY_REPORT.tsv")
val versionSelectionReport = layout.projectDirectory.file("VERSION_SELECTION_REPORT.tsv")
tasks.register("generateDependencyReports") {
    group = "verification"
    description = "Writes deterministic selected-component and version-selection reports."
    outputs.files(dependencyReport, versionSelectionReport)
    outputs.upToDateWhen { false }
    doLast {
        val targets = listOf(
            Triple(project, "nativeClassifiers", true),
            Triple(project(":jvm"), "runtimeClasspath", true),
            Triple(project(":jvm"), "testRuntimeClasspath", true),
            Triple(project(":android"), "debugRuntimeClasspath", true),
            Triple(project(":android"), "releaseRuntimeClasspath", true),
            Triple(project(":android"), "debugUnitTestRuntimeClasspath", true),
            Triple(project(":android"), "debugAndroidTestRuntimeClasspath", true),
            Triple(project(":roborazzi"), "debugRuntimeClasspath", true),
            Triple(project(":roborazzi"), "debugUnitTestRuntimeClasspath", true),
        )
        val selectedRows = sortedSetOf<String>()
        val selectionRows = sortedSetOf<String>()
        targets.forEach { (owner, configurationName, required) ->
            val configuration = owner.configurations.findByName(configurationName)
            check(configuration != null || !required) {
                "Required report configuration is absent: ${owner.path}:$configurationName"
            }
            configuration ?: return@forEach
            val result = configuration.incoming.resolutionResult
            result.allComponents.forEach components@{ component ->
                val module = component.moduleVersion ?: return@components
                val reason = component.selectionReason.descriptions
                    .joinToString(" | ") { it.description.replace('\t', ' ') }
                selectedRows += listOf(
                    owner.path,
                    configurationName,
                    module.group,
                    module.name,
                    module.version,
                    reason,
                ).joinToString("\t")
            }
            result.allDependencies.filterIsInstance<ResolvedDependencyResult>().forEach edges@{ edge ->
                val selected = edge.selected.moduleVersion ?: return@edges
                val reason = edge.selected.selectionReason.descriptions
                    .joinToString(" | ") { it.description.replace('\t', ' ') }
                selectionRows += listOf(
                    owner.path,
                    configurationName,
                    edge.requested.displayName.replace('\t', ' '),
                    "${selected.group}:${selected.name}:${selected.version}",
                    edge.selected.selectionReason.isConflictResolution.toString(),
                    reason,
                ).joinToString("\t")
            }
        }
        dependencyReport.asFile.writeText(
            "project\tconfiguration\tgroup\tartifact\tversion\tselection_reason\n" +
                selectedRows.joinToString("\n", postfix = "\n"),
        )
        versionSelectionReport.asFile.writeText(
            "project\tconfiguration\trequested\tselected\tconflict_resolution\tselection_reason\n" +
                selectionRows.joinToString("\n", postfix = "\n"),
        )
        check(dependencyReport.asFile.readText().contains("net.bytebuddy\tbyte-buddy\t"))
        check(dependencyReport.asFile.readText().contains("com.squareup.okhttp3\tokhttp\t"))
        println("Wrote ${selectedRows.size} selected components and ${selectionRows.size} dependency edges")
    }
}
