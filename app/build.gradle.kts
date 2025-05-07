// app/build.gradle.kts

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.services)
}

android {
    // ---> ASEGÚRATE DE QUE ESTE NAMESPACE SEA EL CORRECTO <---
    namespace = "com.example.foodieclub"
    compileSdk = 34 // Mantener SDK 34 (Estable)

    defaultConfig {
        // ---> ASEGÚRATE DE QUE ESTE APPLICATIONID SEA EL CORRECTO <---
        applicationId = "com.example.foodieclub"
        minSdk = 24
        targetSdk = 34 // Mantener SDK 34 (Estable)
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
    buildFeatures { compose = true }
    // composeOptions { ... } // <-- Asegúrate que este bloque sigue eliminado
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    // Core & Jetpack (Referencias a libs.versions.toml)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose) // <-- Verifica que esta línea existe
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose (Referencias a libs.versions.toml)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Coil (Referencias a libs.versions.toml)
    implementation(libs.coil.compose)
    implementation(libs.androidx.navigation.compose)

    // Firebase & Google (Referencias a libs.versions.toml)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.storage)
    implementation(libs.play.services.auth)
    implementation(libs.play.services.identity)
    implementation(libs.firebase.ui.auth)

    // Retrofit & Networking (Referencias a libs.versions.toml)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)

    // Tests (Referencias a libs.versions.toml)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}