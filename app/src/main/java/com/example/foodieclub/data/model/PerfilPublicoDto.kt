package com.example.foodieclub.data.model

import com.google.gson.annotations.SerializedName

data class PerfilPublicoDto(
    @SerializedName("firebaseUid") val firebaseUid: String?,
    @SerializedName("nombreMostrado") val nombreMostrado: String?,
    @SerializedName("fotoUrl") val fotoUrl: String?,
    @SerializedName("fechaRegistro") val fechaRegistro: String?,
    @SerializedName("recetasPublicadas") val recetasPublicadas: List<RecetaDto>?,
    @SerializedName("numeroRecetas") val numeroRecetas: Int?
    // ... cualquier otro campo público ...
)