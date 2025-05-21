package com.example.foodieclub.data.model

import com.google.gson.annotations.SerializedName

// Si usas Gson, no necesitas @Serializable, pero es buena práctica definir la estructura.
//@Serializable // Descomenta si usas Kotlinx Serialization
data class ParsedIngredient(
    @SerializedName("name") val name: String? = null,
    @SerializedName("quantity") val quantity: Double? = null,
    @SerializedName("unit") val unit: String? = null,
    @SerializedName("notes") val notes: String? = null
)