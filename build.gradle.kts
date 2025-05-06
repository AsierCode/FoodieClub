// build.gradle.kts (Nivel raíz del proyecto)

// Define los plugins que estarán disponibles para los módulos
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.gms.services) apply false
}