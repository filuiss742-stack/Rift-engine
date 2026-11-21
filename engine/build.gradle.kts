plugins { id("org.jetbrains.kotlin.jvm") }
kotlin {
 jvmToolchain(17)
 sourceSets { main { kotlin.srcDir("src/main/java") }; test { kotlin.srcDir("src/test/java") } }
}
dependencies { testImplementation("junit:junit:4.13.2") }
