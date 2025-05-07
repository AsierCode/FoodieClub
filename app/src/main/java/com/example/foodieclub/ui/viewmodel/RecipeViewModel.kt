package com.example.foodieclub.ui.viewmodel // Revisa tu paquete

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// --- Imports de Estados (Ahora con parámetros extra en RecipeListState) ---
import com.example.foodieclub.ui.viewmodel.RecipeListState
import com.example.foodieclub.ui.viewmodel.CreateRecipeState
import com.example.foodieclub.ui.viewmodel.RecipeDetailState
// --- Imports de DTOs ---
import com.example.foodieclub.data.model.ComentarioDto
import com.example.foodieclub.data.model.RecetaDto
// --- Imports de Red ---
import com.example.foodieclub.data.network.ApiService
import com.example.foodieclub.data.network.RetrofitClient
// --- Imports de Firebase ---
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
// --- Imports de Coroutines y otros ---
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.util.*

class RecipeViewModel : ViewModel() {

    // --- Estados Observables ---
    private val _recipeListState = MutableStateFlow<RecipeListState>(RecipeListState.Loading)
    val recipeListState: StateFlow<RecipeListState> = _recipeListState.asStateFlow()

    private val _createRecipeState = MutableStateFlow<CreateRecipeState>(CreateRecipeState.Idle)
    val createRecipeState: StateFlow<CreateRecipeState> = _createRecipeState.asStateFlow()

    private val _recipeDetailState = MutableStateFlow(RecipeDetailState())
    val recipeDetailState: StateFlow<RecipeDetailState> = _recipeDetailState.asStateFlow()

    private val _likedRecipeIds = MutableStateFlow<Set<Long>>(emptySet())
    val likedRecipeIds: StateFlow<Set<Long>> = _likedRecipeIds.asStateFlow()

    private val _savedRecipeIds = MutableStateFlow<Set<Long>>(emptySet())
    val savedRecipeIds: StateFlow<Set<Long>> = _savedRecipeIds.asStateFlow()

    // --- Token y Referencias ---
    var idToken: String? = null
        set(value) {
            val changed = field != value
            field = value
            if (changed && value != null) {
                Log.d("RecipeViewModel", "Token actualizado. Llamando a loadUserInteractions.")
                loadUserInteractions()
            } else if (changed && value == null) {
                Log.d("RecipeViewModel", "Token limpiado.")
                clearUserSpecificData()
            }
        }
    private val storageRef: StorageReference = FirebaseStorage.getInstance().reference
    private val apiService: ApiService = RetrofitClient.instance

    init {
        Log.d("RecipeViewModelInit", "RecipeViewModel inicializado.")
        loadRecipes()
    }

    // --- Funciones Públicas ---

    fun loadRecipes() {
        _recipeListState.value = RecipeListState.Loading
        viewModelScope.launch {
            Log.d("RecipeViewModelAPI", "loadRecipes: Iniciando carga...")
            try {
                val recipes = apiService.getAllRecetas()
                Log.i("RecipeViewModelAPI", "loadRecipes: Éxito - ${recipes.size} recetas.")
                // --- CORREGIDO: Pasar isSearchResult ---
                _recipeListState.value = RecipeListState.Success(recipes, isSearchResult = false)
                idToken?.let { loadUserInteractions() }
            } catch (e: Exception) {
                handleApiError("loadRecipes", e) { msg ->
                    // --- CORREGIDO: Pasar isSearchError ---
                    _recipeListState.value = RecipeListState.Error(msg, isSearchError = false)
                }
            }
        }
    }

    fun searchRecipes(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            Log.d("RecipeViewModelAPI", "searchRecipes: Query vacía, llamando a loadRecipes().")
            loadRecipes()
            return
        }

        _recipeListState.value = RecipeListState.Loading
        Log.d("RecipeViewModelAPI", "searchRecipes: Iniciando búsqueda para '$trimmedQuery'")
        viewModelScope.launch {
            try {
                val searchResults = apiService.searchRecipes(trimmedQuery)
                Log.i("RecipeViewModelAPI", "searchRecipes: Éxito - ${searchResults.size} resultados para '$trimmedQuery'.")
                // --- CORREGIDO: Pasar isSearchResult ---
                _recipeListState.value = RecipeListState.Success(searchResults, isSearchResult = true)
                idToken?.let { loadUserInteractions() }
            } catch (e: Exception) {
                handleApiError("searchRecipes", e) { msg ->
                    // --- CORREGIDO: Pasar isSearchError ---
                    _recipeListState.value = RecipeListState.Error(msg, isSearchError = true)
                }
            }
        }
    }

    private suspend fun uploadImageToStorage(imageUri: Uri): String {
        val filename = "${UUID.randomUUID()}.jpg"
        val imageRef = storageRef.child("recipe_images/$filename")
        Log.d("StorageUpload", "Subiendo a: ${imageRef.path}")
        return withContext(Dispatchers.IO) {
            try {
                imageRef.putFile(imageUri).await()
                val downloadUrl = imageRef.downloadUrl.await()
                Log.i("StorageUpload", "URL obtenida: $downloadUrl")
                downloadUrl.toString()
            } catch (e: Exception) {
                Log.e("StorageUpload", "Error Storage", e)
                throw IOException("Fallo al subir imagen: ${e.message}", e)
            }
        }
    }

    fun uploadImageAndCreateRecipe(
        imageUri: Uri, title: String, description: String, ingredients: String,
        steps: String, prepTime: Int?, portions: Int?
    ) {
        val currentToken = idToken ?: run { _createRecipeState.value = CreateRecipeState.Error("No autenticado"); return }
        _createRecipeState.value = CreateRecipeState.Loading
        viewModelScope.launch {
            Log.d("RecipeViewModelAPI", "uploadImageAndCreateRecipe: Iniciando...")
            try {
                val imageUrl = uploadImageToStorage(imageUri)
                val recipeDataToSend = mapOf(
                    "titulo" to title, "descripcion" to description, "ingredientes" to ingredients,
                    "pasos" to steps, "tiempoPreparacion" to prepTime, "numRaciones" to portions,
                    "imagenUrl" to imageUrl
                ).filterValues { it != null }
                Log.d("RecipeViewModelAPI", "createRecipe: Llamando API...")
                val createdRecipe = apiService.createRecipe("Bearer $currentToken", recipeDataToSend)
                Log.i("RecipeViewModelAPI", "createRecipe: Éxito - ${createdRecipe.titulo}")
                _createRecipeState.value = CreateRecipeState.Success("¡Receta '${createdRecipe.titulo}' creada!")
                loadRecipes() // Refrescar lista
            } catch (e: Exception) {
                handleApiError("uploadImageAndCreateRecipe", e) { msg -> _createRecipeState.value = CreateRecipeState.Error(msg) }
            }
        }
    }

    fun resetCreateState() { _createRecipeState.value = CreateRecipeState.Idle }

    fun loadUserInteractions() {
        val token = idToken ?: run { Log.w("RecipeViewModel", "loadUserInteractions llamado sin token."); return }
        val bearerToken = "Bearer $token"
        Log.d("RecipeViewModelAPI", "loadUserInteractions: Iniciando...")
        viewModelScope.launch {
            try {
                coroutineScope {
                    val likedDeferred = async { apiService.getLikedRecipes(bearerToken) }
                    val savedDeferred = async { apiService.getSavedRecipes(bearerToken) }
                    var likedSet = _likedRecipeIds.value // Mantener valor anterior por defecto
                    var savedSet = _savedRecipeIds.value // Mantener valor anterior por defecto
                    try {
                        likedSet = likedDeferred.await().mapNotNull { it.id }.toSet()
                        Log.d("RecipeViewModelAPI", "loadUserInteractions: Likes cargados (${likedSet.size})")
                    } catch (e:Exception) { Log.e("RecipeViewModelAPI", "loadUserInteractions: Error cargando LIKES", e)}
                    try {
                        savedSet = savedDeferred.await().mapNotNull { it.id }.toSet()
                        Log.d("RecipeViewModelAPI", "loadUserInteractions: Guardados cargados (${savedSet.size})")
                    } catch (e:Exception) { Log.e("RecipeViewModelAPI", "loadUserInteractions: Error cargando GUARDADOS", e)}
                    _likedRecipeIds.value = likedSet
                    _savedRecipeIds.value = savedSet
                    Log.d("RecipeViewModelAPI", "loadUserInteractions: StateFlows actualizados.")
                }
            } catch (e: Exception) {
                Log.e("RecipeViewModelAPI", "loadUserInteractions: Error en coroutineScope", e)
            }
        }
    }

    fun loadRecipeDetail(recipeId: Long) {
        _recipeDetailState.update { it.copy(isLoading = true, recipe = null, comments = emptyList(), errorMessage = null) }
        Log.d("RecipeViewModelAPI", "loadRecipeDetail: Iniciando para ID $recipeId")
        viewModelScope.launch {
            try {
                val recipe = apiService.getRecetaById(recipeId)
                Log.d("RecipeViewModelAPI", "loadRecipeDetail: Receta obtenida OK.")
                val comments = try { apiService.getComments(recipeId) } catch (e: Exception) { Log.e("RecipeViewModelAPI", "Error cargando comentarios para $recipeId", e); emptyList() }
                Log.d("RecipeViewModelAPI", "loadRecipeDetail: Comentarios obtenidos (${comments.size}).")
                idToken?.let { loadUserInteractions() }
                _recipeDetailState.update { it.copy(isLoading = false, recipe = recipe, comments = comments) }
            } catch (e: Exception) {
                handleApiError("loadRecipeDetail", e) { msg -> _recipeDetailState.update { it.copy(isLoading = false, errorMessage = msg) } }
            }
        }
    }

    fun toggleLike(recipeId: Long) {
        val token = idToken ?: run { Log.e("RecipeViewModel", "toggleLike sin token!"); return }
        val bearerToken = "Bearer $token"
        val isCurrentlyLiked = _likedRecipeIds.value.contains(recipeId)
        Log.d("RecipeViewModelAPI", "toggleLike: ID $recipeId, Era: $isCurrentlyLiked")
        _likedRecipeIds.update { if (isCurrentlyLiked) it - recipeId else it + recipeId }
        updateRecipeCounters(recipeId, isLiked = !isCurrentlyLiked)
        viewModelScope.launch {
            try {
                val response = if (isCurrentlyLiked) apiService.unlikeRecipe(bearerToken, recipeId) else apiService.likeRecipe(bearerToken, recipeId)
                if (!response.isSuccessful) throw HttpException(response)
                Log.i("RecipeViewModelAPI", "toggleLike: API OK para ID $recipeId")
            } catch (e: Exception) {
                handleApiError("toggleLike", e) { /* Log ya hecho */ }
                Log.w("RecipeViewModelAPI", "toggleLike: Revertiendo UI para ID $recipeId")
                _likedRecipeIds.update { if (!isCurrentlyLiked) it - recipeId else it + recipeId }
                updateRecipeCounters(recipeId, isLiked = isCurrentlyLiked)
            }
        }
    }

    fun toggleSave(recipeId: Long) {
        val token = idToken ?: run { Log.e("RecipeViewModel", "toggleSave sin token!"); return }
        val bearerToken = "Bearer $token"
        val isCurrentlySaved = _savedRecipeIds.value.contains(recipeId)
        Log.d("RecipeViewModelAPI", "toggleSave: ID $recipeId, Era: $isCurrentlySaved")
        _savedRecipeIds.update { if (isCurrentlySaved) it - recipeId else it + recipeId }
        updateRecipeCounters(recipeId, isSaved = !isCurrentlySaved)
        viewModelScope.launch {
            try {
                val response = if (isCurrentlySaved) apiService.unsaveRecipe(bearerToken, recipeId) else apiService.saveRecipe(bearerToken, recipeId)
                if (!response.isSuccessful) throw HttpException(response)
                Log.i("RecipeViewModelAPI", "toggleSave: API OK para ID $recipeId")
            } catch (e: Exception) {
                handleApiError("toggleSave", e) { /* Log ya hecho */ }
                Log.w("RecipeViewModelAPI", "toggleSave: Revertiendo UI para ID $recipeId")
                _savedRecipeIds.update { if (!isCurrentlySaved) it - recipeId else it + recipeId }
                updateRecipeCounters(recipeId, isSaved = isCurrentlySaved)
            }
        }
    }

    fun postComment(recipeId: Long, commentText: String) {
        val token = idToken ?: run { Log.w("RecipeViewModel", "postComment sin token!"); return }
        val bearerToken = "Bearer $token"
        if (commentText.isBlank()) return
        Log.d("RecipeViewModelAPI", "postComment: Iniciando para ID $recipeId")
        viewModelScope.launch {
            try {
                val commentData = mapOf("texto" to commentText)
                val newComment = apiService.postComment(bearerToken, recipeId, commentData)
                Log.i("RecipeViewModelAPI", "postComment: Éxito, ID ${newComment.id}")
                _recipeDetailState.update {
                    if (it.recipe?.id == recipeId) it.copy(comments = it.comments + newComment) else it
                }
            } catch (e: Exception) { handleApiError("postComment", e) { /* TODO: Notificar error UI */ } }
        }
    }

    fun deleteComment(recipeId: Long, commentId: Long) {
        val token = idToken ?: run { Log.w("RecipeViewModel", "deleteComment sin token!"); return }
        val bearerToken = "Bearer $token"
        Log.d("RecipeViewModelAPI", "deleteComment: Iniciando para ID $commentId en receta $recipeId")
        val originalComments = _recipeDetailState.value.comments
        _recipeDetailState.update {
            if (it.recipe?.id == recipeId) it.copy(comments = it.comments.filterNot { c -> c.id == commentId }) else it
        }
        viewModelScope.launch {
            try {
                val response = apiService.deleteComment(bearerToken, recipeId, commentId)
                if (!response.isSuccessful) throw HttpException(response)
                Log.i("RecipeViewModelAPI", "deleteComment: Éxito para ID $commentId")
            } catch (e: Exception) {
                handleApiError("deleteComment", e) { /* Log ya hecho */ }
                Log.w("RecipeViewModelAPI", "deleteComment: Revertiendo UI para ID $commentId")
                _recipeDetailState.update {
                    if (it.recipe?.id == recipeId) it.copy(comments = originalComments) else it
                }
                // TODO: Notificar error UI
            }
        }
    }

    // --- Helpers ---
    private fun updateRecipeCounters(recipeId: Long, isLiked: Boolean? = null, isSaved: Boolean? = null) {
        _recipeListState.update { listState ->
            if (listState is RecipeListState.Success) {
                listState.copy(recipes = listState.recipes.map { recipe ->
                    if (recipe.id == recipeId) updateSingleRecipeCounters(recipe, isLiked, isSaved) else recipe
                })
            } else listState
        }
        if (_recipeDetailState.value.recipe?.id == recipeId) {
            _recipeDetailState.update { detailState ->
                detailState.recipe?.let { detailState.copy(recipe = updateSingleRecipeCounters(it, isLiked, isSaved)) } ?: detailState
            }
        }
    }

    private fun updateSingleRecipeCounters(recipe: RecetaDto, isLiked: Boolean?, isSaved: Boolean?): RecetaDto {
        val currentLikes = recipe.likesCount ?: 0
        val currentSaves = recipe.guardadosCount ?: 0
        val likeChange = if (isLiked == true) 1 else if (isLiked == false) -1 else 0
        val saveChange = if (isSaved == true) 1 else if (isSaved == false) -1 else 0
        return recipe.copy(
            likesCount = (currentLikes + likeChange).coerceAtLeast(0),
            guardadosCount = (currentSaves + saveChange).coerceAtLeast(0)
        )
    }

    private fun handleApiError(functionName: String, e: Exception, onErrorStateUpdate: (String) -> Unit) {
        val errorMsg = when(e) {
            is IOException -> "Error de Red. Revisa tu conexión."
            is HttpException -> "Error del Servidor (${e.code()}). Inténtalo más tarde."
            else -> "Error inesperado (${e::class.java.simpleName})."
        }
        val finalMessage = errorMsg
        Log.e("ViewModelAPIError", "Error en '$functionName': ${e.message}", e)
        onErrorStateUpdate(finalMessage)
    }

    fun clearUserSpecificData() {
        Log.d("RecipeViewModel", "Limpiando datos de usuario")
        _likedRecipeIds.value = emptySet()
        _savedRecipeIds.value = emptySet()
        _createRecipeState.value = CreateRecipeState.Idle
        _recipeDetailState.value = RecipeDetailState()
    }
}