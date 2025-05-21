package com.example.foodieclub.data.model

import com.google.gson.annotations.SerializedName

data class PerfilPrivadoDto(
    @SerializedName("email") val email: String?,
    @SerializedName("nombreMostrado") val nombreMostrado: String?,
    @SerializedName("fotoUrl") val fotoUrl: String?,
    @SerializedName("fechaRegistro") val fechaRegistro: String?, // Asumiendo que es un String desde la API
    @SerializedName("recetasPublicadas") val recetasPublicadas: List<RecetaDto>?,
    @SerializedName("recetasLikeadas") val recetasLikeadas: List<RecetaDto>?,
    @SerializedName("recetasGuardadas") val recetasGuardadas: List<RecetaDto>?,
    @SerializedName("numeroRecetas") val numeroRecetas: Int?, // Si este campo viene del JSON
    // ... cualquier otro campo que venga de tu API para el perfil privado ...
    // Si numeroLikes y numeroGuardados vienen del API también, añádelos:
    // @SerializedName("numeroLikes") val numeroLikes: Int?,
    // @SerializedName("numeroGuardados") val numeroGuardados: Int?,
)