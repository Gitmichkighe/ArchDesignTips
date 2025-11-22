pluginManagement {
    repositories {
        google()          // 👈 Required for Firebase
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()          // 👈 Required for Firebase
        mavenCentral()
    }
}

rootProject.name = "AppPrototype"
include(":app")
