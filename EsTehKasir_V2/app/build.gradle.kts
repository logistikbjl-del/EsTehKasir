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
        versionCode = 2
        versionName = "2.0"
    }
}
