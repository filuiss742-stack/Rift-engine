plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace="com.rift.engine";compileSdk=35
 defaultConfig {
  applicationId="com.rift.engine.demo";minSdk=23;targetSdk=35;versionCode=1;versionName="0.2.0"
  testInstrumentationRunner="androidx.test.runner.AndroidJUnitRunner"
 }
 buildTypes { release { isMinifyEnabled=false } }
 compileOptions { sourceCompatibility=JavaVersion.VERSION_17;targetCompatibility=JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget="17" }
}
dependencies {
 implementation(project(":engine"));implementation(project(":platform-android"));implementation(project(":demo-game"))
 androidTestImplementation("androidx.test:runner:1.6.2")
 androidTestImplementation("androidx.test:core:1.6.1")
 androidTestImplementation("androidx.test.ext:junit:1.2.1")
 androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
