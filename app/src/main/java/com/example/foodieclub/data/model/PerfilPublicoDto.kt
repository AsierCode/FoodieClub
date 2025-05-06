package com.example.foodieclub.data.model // Revisa tu paquete

data class PerfilPublicoDto(
    val firebaseUid: String?,
    val nombreMostrado: String?,
    val fotoUrl: String?,
    val fechaRegistro: String?, // Recibir como String
    val numeroRecetas: Int? = 0,
    val recetasPublicadas: List<RecetaDto>? = emptyList()
)