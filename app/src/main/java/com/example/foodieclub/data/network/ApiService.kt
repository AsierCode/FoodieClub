package com.example.foodieclub.data.network // Revisa tu paquete

// ---> IMPORTS COMPLETOS <---
import com.example.foodieclub.data.model.ComentarioDto
import com.example.foodieclub.data.model.PerfilPrivadoDto
import com.example.foodieclub.data.model.PerfilPublicoDto
import com.example.foodieclub.data.model.RecetaDto
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // --- Recetas ---
    @GET("recetas")
    suspend fun getAllRecetas(): List<RecetaDto>

    @GET("recetas/{id}")
    suspend fun getRecetaById(@Path("id") recetaId: Long): RecetaDto

    @GET("recetas/mis-recetas")
    suspend fun getMisRecetas(@Header("Authorization") token: String): List<RecetaDto>

    @GET("recetas/usuario/{firebaseUid}")
    suspend fun getRecetasByUsuario(@Path("firebaseUid") firebaseUid: String): List<RecetaDto>

    @POST("recetas")
    suspend fun createRecipe(
        @Header("Authorization") token: String,
        @Body recipeData: Map<String, @JvmSuppressWildcards Any?>
    ): RecetaDto

    // TODO: Definir endpoints PUT y DELETE para recetas si son necesarios

    // --- NUEVA FUNCIÓN DE BÚSQUEDA ---
    @GET("recetas/buscar") // Endpoint: /api/recetas/buscar
    suspend fun searchRecipes(
        @Query("query") searchTerm: String // Parámetro: ?query=...
    ): List<RecetaDto> // Devuelve la lista de resultados
    // ----------------------------------

    // --- Interacciones (Likes y Guardados) ---
    @POST("interacciones/like/{recetaId}")
    suspend fun likeRecipe(
        @Header("Authorization") token: String,
        @Path("recetaId") recetaId: Long
    ): Response<Unit>

    @DELETE("interacciones/like/{recetaId}")
    suspend fun unlikeRecipe(
        @Header("Authorization") token: String,
        @Path("recetaId") recetaId: Long
    ): Response<Unit>

    @GET("interacciones/likes")
    suspend fun getLikedRecipes(@Header("Authorization") token: String): List<RecetaDto>

    @POST("interacciones/guardar/{recetaId}")
    suspend fun saveRecipe(
        @Header("Authorization") token: String,
        @Path("recetaId") recetaId: Long
    ): Response<Unit>

    @DELETE("interacciones/guardar/{recetaId}")
    suspend fun unsaveRecipe(
        @Header("Authorization") token: String,
        @Path("recetaId") recetaId: Long
    ): Response<Unit>

    @GET("interacciones/guardados")
    suspend fun getSavedRecipes(@Header("Authorization") token: String): List<RecetaDto>


    // --- Comentarios ---
    @GET("recetas/{recetaId}/comentarios")
    suspend fun getComments(
        @Path("recetaId") recetaId: Long
    ): List<ComentarioDto>

    @POST("recetas/{recetaId}/comentarios")
    suspend fun postComment(
        @Header("Authorization") token: String,
        @Path("recetaId") recetaId: Long,
        @Body commentData: Map<String, String> // { "texto": "..." }
    ): ComentarioDto

    @DELETE("recetas/{recetaId}/comentarios/{comentarioId}")
    suspend fun deleteComment(
        @Header("Authorization") token: String,
        @Path("recetaId") recetaId: Long,
        @Path("comentarioId") comentarioId: Long
    ): Response<Unit>


    // --- Usuarios/Perfiles ---
    @GET("usuarios/{firebaseUid}/perfil-publico")
    suspend fun getPublicProfile(@Path("firebaseUid") firebaseUid: String): PerfilPublicoDto // Público

    @GET("usuarios/mi-perfil")
    suspend fun getMyProfile(@Header("Authorization") token: String): PerfilPrivadoDto // Privado

}