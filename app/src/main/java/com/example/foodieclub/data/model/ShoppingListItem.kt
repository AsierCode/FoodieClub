// En com/example/foodieclub/data/model/ShoppingListItem.kt
package com.example.foodieclub.data.model

import com.google.firebase.Timestamp // Importante para el timestamp
import java.util.UUID

data class ShoppingListItem(
    val id: String = UUID.randomUUID().toString(), // Genera ID por defecto
    val name: String = "",
    var isPurchased: Boolean = false,
    val addedAt: Timestamp = Timestamp.now() // Timestamp por defecto al crear
) {
    // Constructor sin argumentos necesario para la deserialización de Firestore si lees objetos directamente.
    // Si solo lees mapas y los conviertes, no es estrictamente necesario, pero es buena práctica.
    constructor() : this("", "", false, Timestamp.now())
}