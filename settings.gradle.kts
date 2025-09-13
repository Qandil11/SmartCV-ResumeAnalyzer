pluginManagement {
    plugins {
        id("com.android.application") version "8.6.0" apply false
        id("com.android.library") version "8.6.0" apply false
        kotlin("android") version "2.0.20" apply false
        kotlin("multiplatform") version "2.0.20" apply false
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
    }

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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AIResumeAnalyzer"
include(":app")
include(":app:lib")
include(":shared")
