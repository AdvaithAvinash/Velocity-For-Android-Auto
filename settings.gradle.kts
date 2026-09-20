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
        // NewPipeExtractor (the YouTube engine Velocity's player is built on) is published on JitPack.
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Velocity"
include(":app")
