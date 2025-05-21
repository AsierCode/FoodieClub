package com.example.foodieclub.data.model

import com.google.gson.annotations.SerializedName

data class RecetaDto(
    @SerializedName("id") val id: Long?,
    @SerializedName("titulo") val titulo: String?,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("ingredientes") val ingredientes: String?,
    @SerializedName("pasos") val pasos: String?,
    @SerializedName("tiempoPreparacion") val tiempoPreparacion: Int?,
    @SerializedName("numRaciones") val numRaciones: Int?,
    @SerializedName("usuario") val usuario: UsuarioDto?,
    @SerializedName("imagenUrl") val imagenUrl: String?,
    @SerializedName("fechaCreacion") val fechaCreacion: String?,
    @SerializedName("fechaActualizacion") val fechaActualizacion: String?,
    @SerializedName("likesCount") val likesCount: Int? = 0,
    @SerializedName("guardadosCount") val guardadosCount: Int? = 0
)