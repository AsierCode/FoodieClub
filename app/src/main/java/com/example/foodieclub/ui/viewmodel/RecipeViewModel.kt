package com.example.foodieclub.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieclub.data.model.ComentarioDto
import com.example.foodieclub.data.model.RecetaDto
import com.example.foodieclub.data.network.ApiService
import com.example.foodieclub.data.network.RetrofitClient
import com.example.foodieclub.data.preferences.RecipeHistoryManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID

// --- INICIO DE DEFINICIONES DE ESTADO ---
// (Asegúrate de que estas sean las ÚNICAS definiciones y estén donde deben estar,
//  y que RecipeDetailState tiene postCommentError)


// ASEGÚRATE DE QUE ESTA ES LA ÚNICA DEFINICIÓN Y QUE INCLUYE postCommentError
data class RecipeDetailState(
    val isLoading: Boolean = false,
    val recipe: RecetaDto? = null,
    val comments: List<ComentarioDto> = emptyList(),
    val errorMessage: String? = null,
    val postCommentError: Boolean = false // ¡CRUCIAL!
)

sealed interface CommentActionResult {
    val targetRecipeId: Long
    data class Loading(override val targetRecipeId: Long) : CommentActionResult
    data class LoadingDelete(override val targetRecipeId: Long, val commentId: Long) : CommentActionResult
    data class SuccessPost(val newComment: ComentarioDto, override val targetRecipeId: Long) : CommentActionResult
    data class SuccessDelete(val deletedCommentId: Long, override val targetRecipeId: Long) : CommentActionResult
    data class Error(
        val message: String,
        override val targetRecipeId: Long,
        val commentId: Long? = null,
        val isDeleteError: Boolean = false
    ) : CommentActionResult
}
// --- FIN DE DEFINICIONES DE ESTADO ---


@OptIn(ExperimentalCoroutinesApi::class)
class RecipeViewModel(application: Application) : AndroidViewModel(application) {

    private val recipeHistoryManager = RecipeHistoryManager(application)
    private var remoteConfigInstance: FirebaseRemoteConfig? = null
    private val apiService: ApiService = RetrofitClient.instance
    private val storageRef: StorageReference = FirebaseStorage.getInstance().reference

    // --- Estados que este ViewModel maneja ---
    private val _recipeListState = MutableStateFlow<RecipeListState>(RecipeListState.Loading)
    val recipeListState: StateFlow<RecipeListState> = _recipeListState.asStateFlow()

    private val _createRecipeState = MutableStateFlow<CreateRecipeState>(CreateRecipeState.Idle)
    val createRecipeState: StateFlow<CreateRecipeState> = _createRecipeState.asStateFlow()

    private val _likedRecipeIds = MutableStateFlow<Set<Long>>(emptySet())
    val likedRecipeIds: StateFlow<Set<Long>> = _likedRecipeIds.asStateFlow()

    private val _savedRecipeIds = MutableStateFlow<Set<Long>>(emptySet())
    val savedRecipeIds: StateFlow<Set<Long>> = _savedRecipeIds.asStateFlow()

    private val _recipeHistory = MutableStateFlow<List<RecetaDto>>(emptyList())
    val recipeHistory: StateFlow<List<RecetaDto>> = _recipeHistory.asStateFlow()

    private val _featuredRecipeId = MutableStateFlow<Long>(-1L)
    val featuredRecipeId: StateFlow<Long> = _featuredRecipeId.asStateFlow()

    private val _commentActionResult = MutableStateFlow<CommentActionResult?>(null)
    val commentActionResult: StateFlow<CommentActionResult?> = _commentActionResult.asStateFlow()

    var idToken: String? = null
        set(value) {
            val changed = field != value
            field = value
            if (changed && value != null) {  }
            else if (changed && value == null) {clearUserSpecificData() }
        }

    init {
        loadRecipes()
        observeRecipeHistoryIdsAndUpdateState()
    }

    // --- FUNCIÓN PARA OBTENER UID DEL USUARIO ACTUAL ---
    fun getCurrentUserUid(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }
    // ----------------------------------------------------

    // --- Remote Config ---
    fun setRemoteConfigInstance(instance: FirebaseRemoteConfig) {
        remoteConfigInstance = instance
        readFeaturedRecipeId()
    }

    private fun readFeaturedRecipeId() {
        remoteConfigInstance?.let { config ->
            val id = config.getLong("featured_recipe_id")
            _featuredRecipeId.value = id
        } ?: run {
            _featuredRecipeId.value = -1L
        }
    }

    // --- Historial ---
    private fun observeRecipeHistoryIdsAndUpdateState() {
        recipeHistoryManager.recipeHistoryIdsFlow
            .map { ids: List<Long> ->
                if (ids.isEmpty()) {
                    emptyList<RecetaDto>()
                } else {
                    val currentLoadedRecipes = (_recipeListState.value as? RecipeListState.Success)?.recipes ?: emptyList()
                    ids.mapNotNull { historyId -> currentLoadedRecipes.find { recipe -> recipe.id == historyId } }
                }
            }
            .catch { e: Throwable ->
                emit(emptyList<RecetaDto>())
            }
            .onEach { recipes ->
                _recipeHistory.value = recipes
            }
            .launchIn(viewModelScope)
    }

    fun clearRecipeHistory() {
        viewModelScope.launch {
            recipeHistoryManager.clearHistory()
        }
    }

    // --- Carga de Datos ---
    fun loadRecipes() {
        _recipeListState.value = RecipeListState.Loading
        viewModelScope.launch {
            try {
                val recipes = apiService.getAllRecetas()
                _recipeListState.value = RecipeListState.Success(recipes.shuffled(), isSearchResult = false) // Shuffle
                idToken?.let { loadUserInteractions() }
            } catch (e: Exception) {
                handleApiError("loadRecipes", e) { msg ->
                    _recipeListState.value = RecipeListState.Error(msg, isSearchError = false)
                }
            }
        }
    }

    fun searchRecipes(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            loadRecipes()
            return
        }
        _recipeListState.value = RecipeListState.Loading
        viewModelScope.launch {
            try {
                val searchResults = apiService.searchRecipes(trimmedQuery)
                _recipeListState.value = RecipeListState.Success(searchResults, isSearchResult = true)
                idToken?.let { loadUserInteractions() }
            } catch (e: Exception) {
                handleApiError("searchRecipes", e) { msg ->
                    _recipeListState.value = RecipeListState.Error(msg, isSearchError = true)
                }
            }
        }
    }

    // --- Creación de Receta ---
    private suspend fun uploadImageToStorage(imageUri: Uri): String {
        val filename = "${UUID.randomUUID()}.jpg"
        val imageRef = storageRef.child("recipe_images/$filename")
        return withContext(Dispatchers.IO) {
            try {
                imageRef.putFile(imageUri).await()
                val downloadUrl = imageRef.downloadUrl.await()
                downloadUrl.toString()
            } catch (e: Exception) {
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
            try {
                val imageUrl = uploadImageToStorage(imageUri)
                val recipeDataToSend = mapOf(
                    "titulo" to title, "descripcion" to description, "ingredientes" to ingredients,
                    "pasos" to steps, "tiempoPreparacion" to prepTime, "numRaciones" to portions,
                    "imagenUrl" to imageUrl
                ).filterValues { it != null }
                val createdRecipe = apiService.createRecipe("Bearer $currentToken", recipeDataToSend)
                _createRecipeState.value = CreateRecipeState.Success("¡Receta '${createdRecipe.titulo}' creada!")
                loadRecipes()
            } catch (e: Exception) {
                handleApiError("uploadImageAndCreateRecipe", e) { msg -> _createRecipeState.value = CreateRecipeState.Error(msg) }
            }
        }
    }

    fun resetCreateState() { _createRecipeState.value = CreateRecipeState.Idle }

    // --- Interacciones ---
    fun loadUserInteractions() {
        val token = idToken ?: run { return }
        val bearerToken = "Bearer $token"
        viewModelScope.launch {
            try {
                coroutineScope {
                    val likedDeferred = async { apiService.getLikedRecipes(bearerToken) }
                    val savedDeferred = async { apiService.getSavedRecipes(bearerToken) }
                    val likedResult = kotlin.runCatching { likedDeferred.await() }
                    val savedResult = kotlin.runCatching { savedDeferred.await() }
                    likedResult.onSuccess { likedRecipes ->
                        _likedRecipeIds.value = likedRecipes.mapNotNull { it.id }.toSet()
                    }.onFailure { e -> Log.e("RecipeViewModelAPI", "loadUserInteractions: Error cargando LIKES", e) }
                    savedResult.onSuccess { savedRecipes ->
                        _savedRecipeIds.value = savedRecipes.mapNotNull { it.id }.toSet()
                    }.onFailure { e -> Log.e("RecipeViewModelAPI", "loadUserInteractions: Error cargando GUARDADOS", e) }
                }
            } catch (e: Exception) {
            }
        }
    }

    fun toggleLike(recipeId: Long) {
        val token = idToken ?: run { Log.e("RVM_Like", "No token"); return }
        val bearerToken = "Bearer $token"
        val isCurrentlyLiked = _likedRecipeIds.value.contains(recipeId)

        _likedRecipeIds.update { if (isCurrentlyLiked) it - recipeId else it + recipeId }
        updateRecipeCountersInList(recipeId, isLiked = !isCurrentlyLiked)

        viewModelScope.launch {
            try {
                val response = if (isCurrentlyLiked) apiService.unlikeRecipe(bearerToken, recipeId) else apiService.likeRecipe(bearerToken, recipeId)
                if (!response.isSuccessful) throw HttpException(response)
            } catch (e: Exception) {
                handleApiError("toggleLike", e) {}
                _likedRecipeIds.update { if (!isCurrentlyLiked) it - recipeId else it + recipeId }
                updateRecipeCountersInList(recipeId, isLiked = isCurrentlyLiked)
            }
        }
    }

    fun toggleSave(recipeId: Long) {
        val token = idToken ?: run { Log.e("RVM_Save", "No token"); return }
        val bearerToken = "Bearer $token"
        val isCurrentlySaved = _savedRecipeIds.value.contains(recipeId)

        _savedRecipeIds.update { if (isCurrentlySaved) it - recipeId else it + recipeId }

        viewModelScope.launch {
            try {
                val response = if (isCurrentlySaved) apiService.unsaveRecipe(bearerToken, recipeId) else apiService.saveRecipe(bearerToken, recipeId)
                if (!response.isSuccessful) throw HttpException(response)
            } catch (e: Exception) {
                handleApiError("toggleSave", e) {}
                _savedRecipeIds.update { if (!isCurrentlySaved) it - recipeId else it + recipeId }
            }
        }
    }

    fun postComment(recipeId: Long, commentText: String) {
        val token = idToken ?: run {
            _commentActionResult.value = CommentActionResult.Error("Debes iniciar sesión para comentar.", recipeId)
            return
        }
        if (commentText.isBlank()) {
            _commentActionResult.value = CommentActionResult.Error("El comentario no puede estar vacío.", recipeId)
            return
        }
        _commentActionResult.value = CommentActionResult.Loading(recipeId)
        viewModelScope.launch {
            try {
                val commentData = mapOf("texto" to commentText)
                val newComment = apiService.postComment("Bearer $token", recipeId, commentData)
                _commentActionResult.value = CommentActionResult.SuccessPost(newComment, recipeId)
            } catch (e: Exception) {
                handleApiError("postComment", e) { msg ->
                    _commentActionResult.value = CommentActionResult.Error(msg, recipeId)
                }
            }
        }
    }

    fun deleteComment(recipeId: Long, commentId: Long) {
        val token = idToken ?: run {
            _commentActionResult.value = CommentActionResult.Error("Debes iniciar sesión.", recipeId, commentId)
            return
        }
        _commentActionResult.value = CommentActionResult.LoadingDelete(recipeId, commentId)
        viewModelScope.launch {
            try {
                val response = apiService.deleteComment("Bearer $token", recipeId, commentId)
                if (!response.isSuccessful) throw HttpException(response)
                _commentActionResult.value = CommentActionResult.SuccessDelete(commentId, recipeId)
            } catch (e: Exception) {
                handleApiError("deleteComment", e) { msg ->
                    _commentActionResult.value = CommentActionResult.Error(msg, recipeId, commentId, isDeleteError = true)
                }
            }
        }
    }

    fun clearCommentActionResult() {
        _commentActionResult.value = null
    }

    // --- Helpers ---
    private fun updateRecipeCountersInList(recipeId: Long, isLiked: Boolean?) {
        _recipeListState.update { listState ->
            if (listState is RecipeListState.Success) {
                listState.copy(recipes = listState.recipes.map { recipe ->
                    if (recipe.id == recipeId) {
                        var currentLikes = recipe.likesCount ?: 0
                        if (isLiked != null) {
                            currentLikes = if (isLiked) currentLikes + 1 else (currentLikes - 1).coerceAtLeast(0)
                        }
                        recipe.copy(likesCount = currentLikes)
                    } else recipe
                })
            } else listState
        }
    }

    private fun handleApiError(functionName: String, e: Exception, onErrorStateUpdate: (String) -> Unit) {
        val errorMsg = when (e) {
            is IOException -> "Error de Red. Revisa tu conexión."
            is HttpException -> "Error del Servidor (${e.code()}). Inténtalo más tarde."
            else -> "Error inesperado: ${e.localizedMessage?.take(100)}"
        }
        onErrorStateUpdate(errorMsg)
    }

    fun clearUserSpecificData() {
        _likedRecipeIds.value = emptySet()
        _savedRecipeIds.value = emptySet()
        _createRecipeState.value = CreateRecipeState.Idle
        _commentActionResult.value = null
    }
}