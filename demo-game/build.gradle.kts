plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":engine"))
    testImplementation("junit:junit:4.13.2")
}
