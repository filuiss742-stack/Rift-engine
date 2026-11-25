plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.rift.spike.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rift.spike.android"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":gdx-spike:core"))
    implementation("com.badlogicgames.gdx:gdx-backend-android:1.14.2")
}
