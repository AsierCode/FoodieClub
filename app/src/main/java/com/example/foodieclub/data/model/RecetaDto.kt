package com.example.foodieclub.data.model // Revisa tu paquete

// Asegúrate que los nombres de campo coinciden EXACTAMENTE con el JSON de la API
data class RecetaDto(
    val id: Long?, // Nullable por si acaso
    val titulo: String?,
    val descripcion: String?,
    val ingredientes: String?, // Mantenemos como String
    val pasos: String?,       // Mantenemos como String
    val tiempoPreparacion: Int?,
    val numRaciones: Int?,
    val usuario: UsuarioDto?, // Referencia al DTO de Usuario
    val imagenUrl: String?,
    // Para las fechas, recibirlas como String es lo más simple con Gson.
    // Podríamos añadir lógica de parseo después si necesitamos objetos Date/LocalDateTime.
    val fechaCreacion: String?,
    val fechaActualizacion: String?,
    val likesCount: Int? = 0, // Valor por defecto si es null
    val guardadosCount: Int? = 0 // Valor por defecto si es null
)