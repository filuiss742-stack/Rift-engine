plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
}

group = "com.example.game"
version = "0.1.0"

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("com.rift.engine:engine:0.5.0-SNAPSHOT")
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
}
