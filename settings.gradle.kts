pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "haptiMap_watch"

include(":app")

// Core: lógica sin interfaz
include(":core:model")
include(":core:location")
include(":core:navigation")
include(":core:haptics")
include(":core:network")
include(":core:intelligence")

// Features: pantallas y flujos de usuario
include(":feature:search")
include(":feature:navigation-ui")
include(":feature:onboarding")
