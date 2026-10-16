plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "so.dhaqaaq.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "so.dhaqaaq.app"
        minSdk = 26          // sendUssdRequest wuxuu u baahan yahay Android 8.0+
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
