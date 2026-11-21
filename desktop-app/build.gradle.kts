plugins { id("org.jetbrains.kotlin.jvm"); application }
kotlin { jvmToolchain(17) }
dependencies {
 implementation(project(":engine")); implementation(project(":platform-desktop")); implementation(project(":demo-game"))
}
application { mainClass.set("com.rift.engine.desktop.MainKt") }
