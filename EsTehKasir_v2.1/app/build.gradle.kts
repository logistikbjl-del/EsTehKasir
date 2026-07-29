plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.estehkasir"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.estehkasir"
        minSdk = 24
        targetSdk = 35
        versionCode = 21
        versionName = "2.1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
