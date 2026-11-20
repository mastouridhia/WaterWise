import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Secrets (API keys) are read from local.properties, which is git-ignored,
// so they never end up in the public GitHub repository.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.waterwise.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.waterwise.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "PERENUAL_API_KEY",
            "\"${localProps.getProperty("PERENUAL_API_KEY", "")}\""
        )
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.fragment)
    implementation(libs.recyclerview)
    implementation(libs.swiperefreshlayout)

    // MVVM: ViewModel + LiveData
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    // Local database (SQLite through Room)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // Networking for the external APIs (Perenual, Open-Meteo)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // Background daily watering check + notifications
    implementation(libs.work.runtime)

    // Image loading (plant photos)
    implementation(libs.glide)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
