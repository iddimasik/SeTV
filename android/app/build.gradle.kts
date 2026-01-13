plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
}

android {
    namespace = "rus.setv"
    compileSdk = 36

    defaultConfig {
        applicationId = "rus.setv"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // ───── AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)

    // ───── Leanback (Android TV)
    implementation(libs.androidx.leanback)

    // ───── Lifecycle + Coroutines
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // ───── UI
    implementation(libs.material)

    // ───── Filament (у тебя уже есть)
    implementation(libs.filament.android)

    // ───── Networking (ОЧЕНЬ ВАЖНО)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    implementation(libs.logging.interceptor)

    // ───── Coroutines
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.glide)
}
