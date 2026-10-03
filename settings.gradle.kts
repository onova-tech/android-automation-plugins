pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "android-automation-plugins"

// The engine the phone runs, from github.com/onova-tech/android-automation-app (git submodule).
// agp validates, builds and replay-tests plugins with exactly this code.
includeBuild("engine/core")

include(":agp")
