package com.example.foodieclub.data.model // Revisa tu paquete

// No necesita Parcelable si solo es para Retrofit/Compose State
data class UsuarioDto(
    val firebaseUid: String?, // Nullable por si acaso
    val email: String?,
    val nombreMostrado: String?,
    val fotoUrl: String?
)