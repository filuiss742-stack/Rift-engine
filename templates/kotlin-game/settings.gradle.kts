pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "RiftKotlinGame"

includeBuild("../../engine") {
    dependencySubstitution {
        substitute(module("com.rift.engine:engine")).using(project(":"))
    }
}
