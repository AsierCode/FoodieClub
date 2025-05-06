package com.example.foodieclub.data.model // Revisa tu paquete

// No podemos usar herencia directa en data classes para JSON fácilmente,
// así que definimos todos los campos.
data class PerfilPrivadoDto(
    // Campos públicos
    val firebaseUid: String?,
    val nombreMostrado: String?,
    val fotoUrl: String?,
    val fechaRegistro: String?,
    val numeroRecetas: Int? = 0,
    val recetasPublicadas: List<RecetaDto>? = emptyList(),
    // Campos privados añadidos
    val email: String?,
    val recetasLikeadas: List<RecetaDto>? = emptyList(),
    val recetasGuardadas: List<RecetaDto>? = emptyList()
)