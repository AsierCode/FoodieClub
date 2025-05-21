package com.example.foodieclub.data.model // Revisa tu paquete

import com.google.gson.annotations.SerializedName

// Data class para representar un comentario recibido de la API
data class ComentarioDto(
    @SerializedName("id") val id: Long?,
    @SerializedName("texto") val texto: String?,
    @SerializedName("fechaCreacion") val fechaCreacion: String?, // La API devuelve esto como String formateado
    @SerializedName("usuario") val usuario: UsuarioDto?
)