@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt") // Needed for Room annotation processing
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    id("com.google.gms.google-services") // ✅ Required for Firebase
}
android {
    namespace = "com.example.appdir"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.appdir"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        viewBinding = true // ✅ You’re using ViewBinding in ContactActivity
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }


}

dependencies {
// Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.5.1")) // Check for the latest version

  // implementation ("com.google.android.gms:play-services-ads:24.7.0")  // Use latest version

    implementation ("com.google.android.gms:play-services-ads:22.3.0")
    // Add the dependency for the Cloud Firestore library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-firestore-ktx") // For Kotlin extensions
    implementation("androidx.compose.material3:material3:1.2.1")
    // ✅ Jetpack Compose (optional)
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui:1.6.4")
    implementation("androidx.compose.material:material:1.6.4")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.4")
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    implementation ("androidx.recyclerview:recyclerview:1.3.0") // or latest version

    debugImplementation("androidx.compose.ui:ui-tooling:1.6.4")

    // ✅ Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // ✅ Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ✅ Lifecycle + ViewModel
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.1")

    // ✅ Google Play Billing
    implementation("com.android.billingclient:billing-ktx:6.2.1")

    // ✅ RecyclerView + Material Components
    implementation("androidx.recyclerview:recyclerview:1.3.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")

    // ✅ JSON Parsing
    implementation("com.google.code.gson:gson:2.10.1")
    implementation ("androidx.constraintlayout:constraintlayout:2.1.0")
    // ✅ Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    // rewarded adds
    implementation ("com.google.android.gms:play-services-ads:22.3.0")
}
