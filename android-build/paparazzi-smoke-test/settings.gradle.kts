pluginManagement {
    repositories {
        val offlineRepository = providers.gradleProperty("baseFileMaven").orNull
        if (offlineRepository != null) {
            maven(url = uri(offlineRepository))
        } else {
            google()
            mavenCentral()
            gradlePluginPortal()
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        val offlineRepository = providers.gradleProperty("baseFileMaven").orNull
        if (offlineRepository != null) {
            maven(url = uri(offlineRepository))
        } else {
            google()
            mavenCentral()
        }
    }
}

rootProject.name = "BaseFilePaparazziSmokeTest"
include(":screenshot")
