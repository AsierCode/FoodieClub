package com.example.foodieclub.data.network

import com.example.foodieclub.data.model.SpoonacularAnalyzeRequest
import com.example.foodieclub.data.model.SpoonacularNutritionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface SpoonacularApiService {
    @POST("recipes/analyze") // Endpoint para analizar ingredientes
    suspend fun analyzeRecipeIngredients(
        @Query("apiKey") apiKey: String,
        @Query("includeNutrition") includeNutrition: Boolean = true, // Para asegurar que devuelve la nutrición
        // @Query("language") language: String = "es", // Opcional, puedes añadirlo si lo necesitas
        @Body request: SpoonacularAnalyzeRequest // El cuerpo de la petición con la lista de ingredientes
    ): Response<SpoonacularNutritionResponse> // Devuelve la respuesta completa para verificar éxito
}