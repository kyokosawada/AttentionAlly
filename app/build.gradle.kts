plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.googleServices)
}

android {
    namespace = "com.example.attentionally"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.attentionally"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Original working dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Additional PRD dependencies
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)

    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.view)
    implementation(libs.camera.lifecycle)

    implementation(libs.mlkit.face.detection)
    implementation(libs.mlkit.vision.common)

    implementation(libs.coroutines.android)
    implementation(libs.accompanist.permissions)

    implementation(libs.material.icons.extended)

    // Essential missing dependencies for PRD features
    implementation(libs.coil.compose)              // Image loading for avatars/profiles
    implementation(libs.datastore.preferences)     // Local storage for user prefs/session
    implementation(libs.room.runtime)              // Local database for offline/caching
    implementation(libs.room.ktx)                  // Room Kotlin extensions
    implementation(libs.kotlinx.serialization.json) // JSON serialization for data models
    implementation(libs.work.runtime.ktx)          // Background tasks for data sync
    implementation(libs.splashscreen)              // Modern splash screen API
    implementation(libs.timber)                    // Logging for debugging and research

    // Room annotation processor - using KSP instead of KAPT
    ksp(libs.room.compiler.ksp)

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}