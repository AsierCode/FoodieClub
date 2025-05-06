package com.example.foodieclub.data.model // Revisa tu paquete

// Importamos UsuarioDto si no está en el mismo paquete
// import com.example.foodieclub.data.model.UsuarioDto

// Data class para representar un comentario recibido de la API
data class ComentarioDto(
    val id: Long?, // ID del comentario en la BD
    val texto: String?, // El contenido del comentario
    val fechaCreacion: String?, // La fecha como String (simplifica deserialización)
    val usuario: UsuarioDto? // DTO del usuario que escribió el comentario
)