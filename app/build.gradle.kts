plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 sourceSets.getByName("main").assets.srcDir("../demo-game/src/main/resources")
 namespace="com.rift.engine";compileSdk=35
 defaultConfig { applicationId="com.rift.engine.demo";minSdk=23;targetSdk=35;versionCode=1;versionName="0.2.0" }
 buildTypes { release { isMinifyEnabled=false } }
 compileOptions { sourceCompatibility=JavaVersion.VERSION_17;targetCompatibility=JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget="17" }
}
dependencies { implementation(project(":engine"));implementation(project(":platform-android"));implementation(project(":demo-game")) }
