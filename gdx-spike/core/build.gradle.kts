plugins { id("org.jetbrains.kotlin.jvm") }

kotlin { jvmToolchain(17) }

dependencies {
    api("com.badlogicgames.gdx:gdx:1.14.2")
    testImplementation("junit:junit:4.13.2")
}
