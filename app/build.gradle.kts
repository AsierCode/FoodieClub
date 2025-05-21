import java.util.Properties
import java.io.FileInputStream
import java.io.File // Import necesario para File(storeFileProp)

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.services)
    id("com.google.firebase.crashlytics")
    // alias(libs.plugins.kotlinx.serialization) // Si lo usas
}

// Leer propiedades de local.properties (para API Keys)
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists() && localPropertiesFile.isFile) {
    try {
        FileInputStream(localPropertiesFile).use { localProperties.load(it) }
    } catch (e: Exception) {
        System.err.println("Advertencia: Error al cargar local.properties: ${e.message}")
    }
} else {
    System.err.println("Advertencia: local.properties no encontrado. API Keys no se configurarán desde allí.")
}

android {
    namespace = "com.example.foodieclub"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.foodieclub"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        buildConfigField("String", "GEMINI_API_KEY", "\"${localProperties.getProperty("GEMINI_API_KEY") ?: ""}\"")
        buildConfigField("String", "SPOONACULAR_API_KEY", "\"${localProperties.getProperty("SPOONACULAR_API_KEY") ?: ""}\"")
    }

    signingConfigs {
        create("release") {
            // Leer propiedades de firma desde gradle.properties
            val storeFileProp = project.findProperty("FOODIECLUB_RELEASE_STORE_FILE")?.toString()
            val storePasswordProp = project.findProperty("FOODIECLUB_RELEASE_STORE_PASSWORD")?.toString()
            val keyAliasProp = project.findProperty("FOODIECLUB_RELEASE_KEY_ALIAS")?.toString()
            val keyPasswordProp = project.findProperty("FOODIECLUB_RELEASE_KEY_PASSWORD")?.toString()

            // Solo configurar si todas las propiedades necesarias están presentes y el archivo existe
            if (storeFileProp != null && storePasswordProp != null && keyAliasProp != null && keyPasswordProp != null && File(storeFileProp).exists()) {
                storeFile = File(storeFileProp)
                storePassword = storePasswordProp
                keyAlias = keyAliasProp
                keyPassword = keyPasswordProp
                println("INFO: Usando configuración de firma 'release' desde gradle.properties.")
            } else {
                println("ADVERTENCIA: No se encontró la configuración completa para la firma 'release' en gradle.properties o el keystore no existe.")
                println("ADVERTENCIA: El build de RELEASE se generará SIN FIRMA o con la firma de DEBUG por defecto si no se corrige.")
                // Puedes optar por fallar el build aquí si es un release crítico:
                // throw GradleException("Configuración de firma para 'release' incompleta o keystore no encontrado.")
                // O permitir que se firme con la clave de debug (no recomendado para releases reales a tiendas)
                // Para TFC, si solo quieres generar el APK y no tienes keystore, puedes comentar la asignación de signingConfig abajo.
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release") // Asignar la configuración de firma
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            // applicationIDSuffix ".debug" // Opcional
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
        buildConfig = true
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    // Core & Jetpack
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    // Shimmer Effect
    implementation("com.valentinilk.shimmer:compose-shimmer:1.2.0")

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Coil
    implementation(libs.coil.compose)

    // Firebase & Google
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.config)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.ui.auth)
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    implementation(libs.play.services.auth)
    implementation(libs.play.services.identity)

    // Google AI (Gemini)
    implementation(libs.google.ai.generativeai)

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${libs.versions.kotlinCoroutines.get()}")

    // Retrofit & Networking
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}