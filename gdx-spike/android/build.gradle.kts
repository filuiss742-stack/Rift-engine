plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val gdxNatives by configurations.creating

android {
    namespace = "com.rift.spike.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rift.spike.android"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        ndk { abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86_64") }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    sourceSets["main"].jniLibs.srcDir(layout.buildDirectory.dir("generated/gdxJniLibs"))
}

dependencies {
    implementation(project(":gdx-spike:core"))
    implementation("com.badlogicgames.gdx:gdx-backend-android:1.14.2")
    gdxNatives("com.badlogicgames.gdx:gdx-platform:1.14.2:natives-armeabi-v7a")
    gdxNatives("com.badlogicgames.gdx:gdx-platform:1.14.2:natives-arm64-v8a")
    gdxNatives("com.badlogicgames.gdx:gdx-platform:1.14.2:natives-x86_64")
}

val copyGdxNatives by tasks.registering(Copy::class) {
    from(gdxNatives.map { zipTree(it) })
    include("jni/**/*.so")
    eachFile { path = path.substringAfter("jni/") }
    includeEmptyDirs = false
    into(layout.buildDirectory.dir("generated/gdxJniLibs"))
}

tasks.named("preBuild").configure {
    dependsOn(copyGdxNatives)
}
