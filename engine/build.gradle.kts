plugins {
 id("org.jetbrains.kotlin.jvm") version "2.0.21"
 id("maven-publish")
}

group = "com.rift.engine"
version = "0.5.0-SNAPSHOT"

if (project == rootProject) {
    repositories {
        mavenCentral()
    }
}
kotlin {
 jvmToolchain(17)
 sourceSets { main { kotlin.srcDir("src/main/java") }; test { kotlin.srcDir("src/test/java") } }
}
dependencies { testImplementation("junit:junit:4.13.2") }


publishing {
 publications {
  create<MavenPublication>("engine") {
   from(components["java"])
   artifactId = "engine"
  }
 }
}
