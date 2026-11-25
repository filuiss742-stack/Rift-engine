plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(project(":gdx-spike:core"))
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:1.14.2")
    runtimeOnly("com.badlogicgames.gdx:gdx-platform:1.14.2:natives-desktop")
}

application {
    mainClass.set("com.rift.spike.desktop.MainKt")
}
