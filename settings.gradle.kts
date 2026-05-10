pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Tankobun"

include(":app")
include(":core:model")
include(":core:network")
include(":core:anilist")
include(":core:database")
include(":core:extensions")
include(":core:reader")
include(":core:downloads")
include(":core:sync")
