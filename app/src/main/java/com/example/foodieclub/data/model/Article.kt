package com.example.foodieclub.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId // <-- AÑADE ESTE IMPORT
import java.util.Date

data class Article(
    @DocumentId val id: String = "", // <-- AÑADE ESTE CAMPO CON LA ANOTACIÓN
    val title: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val author: String? = "FoodieClub",
    val timestamp: Timestamp = Timestamp.now()
) {
    // Ajusta el constructor sin argumentos si es necesario (aunque con valores por defecto suele bastar)
    constructor() : this("", "", "", null, "FoodieClub", Timestamp.now())

    fun getDate(): Date = timestamp.toDate()
}