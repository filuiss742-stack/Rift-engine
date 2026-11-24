plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    application
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    testImplementation("junit:junit:4.13.2")
}

application {
    mainClass.set("com.rift.tools.projectvalidator.MainKt")
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}
