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

rootProject.name = "RiftEngine"
include(
    ":engine",
    ":platform-android",
    ":platform-desktop",
    ":demo-game",
    ":app",
    ":desktop-app",
    ":tools:project-validator",
    ":gdx-spike:core",
    ":gdx-spike:android",
    ":gdx-spike:desktop"
)

project(":gdx-spike:core").projectDir = file("gdx-spike/core")
project(":gdx-spike:android").projectDir = file("gdx-spike/android")
project(":gdx-spike:desktop").projectDir = file("gdx-spike/desktop")
