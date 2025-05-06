package com.example.foodieclub.ui.viewmodel // Revisa tu paquete

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// --- Imports de Estados (asume que están en este mismo paquete o en ui.state) ---
import com.example.foodieclub.ui.viewmodel.RecipeListState
import com.example.foodieclub.ui.viewmodel.CreateRecipeState
import com.example.foodieclub.ui.viewmodel.RecipeDetailState
import com.example.foodieclub.ui.viewmodel.PublicProfileState
import com.example.foodieclub.ui.viewmodel.MyProfileState
// --- Imports de DTOs (del paquete data.model) ---
import com.example.foodieclub.data.model.ComentarioDto
import com.example.foodieclub.data.model.RecetaDto
import com.example.foodieclub.data.model.PerfilPrivadoDto
import com.example.foodieclub.data.model.PerfilPublicoDto
// --- Imports de Red ---
import com.example.foodieclub.data.network.ApiService
import com.example.foodieclub.data.network.RetrofitClient
// --- Imports de Firebase ---
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
// --- Imports de Coroutines y otros ---
import kotlinx.coroutines.Dispatchers
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
    private val _recipeListState = mutableStateOf<RecipeListState>(RecipeListState.Loading)
    val recipeListState: State<RecipeListState> = _recipeListState

    private val _createRecipeState = mutableStateOf<CreateRecipeState>(CreateRecipeState.Idle)
    val createRecipeState: State<CreateRecipeState> = _createRecipeState

    private val _recipeDetailState = MutableStateFlow(RecipeDetailState())
    val recipeDetailState: StateFlow<RecipeDetailState> = _recipeDetailState.asStateFlow()

    private val _likedRecipeIds = MutableStateFlow<Set<Long>>(emptySet())
    val likedRecipeIds: StateFlow<Set<Long>> = _likedRecipeIds.asStateFlow()

    private val _savedRecipeIds = MutableStateFlow<Set<Long>>(emptySet())
    val savedRecipeIds: StateFlow<Set<Long>> = _savedRecipeIds.asStateFlow()

    private val _publicProfileState = mutableStateOf<PublicProfileState>(PublicProfileState.Loading)
    val publicProfileState: State<PublicProfileState> = _publicProfileState

    private val _myProfileState = mutableStateOf<MyProfileState>(MyProfileState.Loading)
    val myProfileState: State<MyProfileState> = _myProfileState

    // --- Token y Referencias ---
    var idToken: String? = null
        set(value) {
            val changed = field != value
            field = value
            if (changed && value != null) {
                Log.d("ViewModel", "Token actualizado, cargando interacciones.")
                loadUserInteractions()
            } else if (changed && value == null) {
                Log.d("ViewModel", "Token limpiado, reseteando interacciones.")
                _likedRecipeIds.value = emptySet()
                _savedRecipeIds.value = emptySet()
            }
        }
    private val storageRef: StorageReference = FirebaseStorage.getInstance().reference
    private val apiService: ApiService = RetrofitClient.instance

    init {
        Log.d("ViewModelInit", "RecipeViewModel inicializado.")
        loadRecipes()
    }

    // --- Funciones Públicas ---

    fun loadRecipes() {
        _recipeListState.value = RecipeListState.Loading
        viewModelScope.launch {
            try {
                Log.d("ViewModelAPI", "loadRecipes: Llamando API...")
                val recipes = apiService.getAllRecetas()
                _recipeListState.value = RecipeListState.Success(recipes)
                Log.i("ViewModelAPI", "loadRecipes: Éxito - ${recipes.size} recetas cargadas.")
            } catch (e: Exception) {
                handleApiError("loadRecipes", e) { msg -> _recipeListState.value = RecipeListState.Error(msg) }
            }
        }
    }

    // --- Función PRIVADA para subir imagen a Storage ---
    private suspend fun uploadImageToStorage(imageUri: Uri): String {
        // Generar nombre único para el archivo en Storage
        val filename = "${UUID.randomUUID()}.jpg"
        // Crear referencia al lugar donde se guardará (carpeta recipe_images)
        val imageRef: StorageReference = storageRef.child("recipe_images/$filename")

        Log.d("StorageUpload", "Intentando subir a: ${imageRef.path}")

        // Ejecutar la subida en un hilo de I/O
        return withContext(Dispatchers.IO) {
            try {
                // Subir el archivo desde la URI local y esperar a que termine
                imageRef.putFile(imageUri).await()
                Log.d("StorageUpload", "Subida completada. Obteniendo URL de descarga...")

                // Obtener la URL pública de descarga del archivo subido y esperar
                val downloadUrl = imageRef.downloadUrl.await()
                Log.i("StorageUpload", "URL de descarga obtenida con éxito.")

                // Devolver la URL como String
                downloadUrl.toString()
            } catch (e: Exception) {
                // Capturar cualquier error durante la subida o la obtención de la URL
                Log.e("StorageUpload", "Error en la operación de Firebase Storage", e)
                // Relanzar como IOException para que sea manejado por el bloque catch externo
                throw IOException("Fallo al subir la imagen a Firebase Storage: ${e.message}", e)
            }
        }
    }


    fun uploadImageAndCreateRecipe(
        imageUri: Uri, title: String, description: String, ingredients: String,
        steps: String, prepTime: Int?, portions: Int?
    ) {
        val currentToken = idToken ?: run { _createRecipeState.value = CreateRecipeState.Error("No autenticado"); return }
        _createRecipeState.value = CreateRecipeState.Loading
        Log.d("ViewModelCreate", "Iniciando creación para '$title'")
        viewModelScope.launch {
            var imageUrl: String? = null
            try {
                imageUrl = uploadImageToStorage(imageUri)
                val recipeDataToSend = mapOf(
                    "titulo" to title, "descripcion" to description, "ingredientes" to ingredients,
                    "pasos" to steps, "tiempoPreparacion" to prepTime, "numRaciones" to portions,
                    "imagenUrl" to imageUrl
                ).filterValues { it != null }
                Log.d("ViewModelCreate", "Llamando API createRecipe...")
                val createdRecipe = apiService.createRecipe("Bearer $currentToken", recipeDataToSend)
                Log.i("ViewModelCreate", "Receta creada OK: ${createdRecipe.titulo}")
                _createRecipeState.value = CreateRecipeState.Success("¡Receta '${createdRecipe.titulo}' creada!")
                loadRecipes() // Actualizar la lista principal
            } catch (e: Exception) {
                handleApiError("uploadImageAndCreateRecipe", e) { msg -> _createRecipeState.value = CreateRecipeState.Error(msg) }
                // TODO: Borrar imagen si API falló
            }
        }
    }

    fun resetCreateState() { _createRecipeState.value = CreateRecipeState.Idle }

    fun loadUserInteractions() {
        val token = idToken ?: return
        val bearerToken = "Bearer $token"
        Log.d("ViewModelInteraction", "Cargando interacciones...")
        viewModelScope.launch {
            try {
                val liked = apiService.getLikedRecipes(bearerToken)
                _likedRecipeIds.value = liked.mapNotNull { it.id }.toSet()
            } catch (e: Exception) { Log.e("ViewModelInteraction", "Error cargando likes", e) }
            try {
                val saved = apiService.getSavedRecipes(bearerToken)
                _savedRecipeIds.value = saved.mapNotNull { it.id }.toSet()
            } catch (e: Exception) { Log.e("ViewModelInteraction", "Error cargando guardados", e) }
            Log.d("ViewModelInteraction", "Interacciones cargadas. Likes: ${_likedRecipeIds.value.size}, Guardados: ${_savedRecipeIds.value.size}")
        }
    }

    fun loadRecipeDetail(recipeId: Long) {
        _recipeDetailState.update { RecipeDetailState(isLoading = true) }
        Log.d("ViewModelDetail", "Cargando detalle receta ID $recipeId")
        viewModelScope.launch {
            try {
                val recipe = apiService.getRecetaById(recipeId)
                val comments = apiService.getComments(recipeId)
                _recipeDetailState.value = RecipeDetailState(isLoading = false, recipe = recipe, comments = comments)
            } catch (e: Exception) {
                handleApiError("loadRecipeDetail", e) { msg -> _recipeDetailState.value = RecipeDetailState(isLoading = false, errorMessage = msg) }
            }
        }
    }

    fun toggleLike(recipeId: Long) {
        val token = idToken ?: run { Log.e("ViewModelInteraction", "toggleLike sin token!"); return }
        val bearerToken = "Bearer $token"
        val isCurrentlyLiked = _likedRecipeIds.value.contains(recipeId)
        Log.d("ViewModelInteraction", "Toggle Like ID: $recipeId. Actual: $isCurrentlyLiked")
        _likedRecipeIds.update { if (isCurrentlyLiked) it - recipeId else it + recipeId } // Optimista
        updateRecipeListCounter(recipeId, isLiked = !isCurrentlyLiked)
        viewModelScope.launch {
            try {
                val response = if (isCurrentlyLiked) apiService.unlikeRecipe(bearerToken, recipeId) else apiService.likeRecipe(bearerToken, recipeId)
                if (!response.isSuccessful) {
                    Log.e("ViewModelInteraction", "Error API Like/Unlike: ${response.code()}. Revirtiendo UI.")
                    _likedRecipeIds.update { if (!isCurrentlyLiked) it - recipeId else it + recipeId }
                    updateRecipeListCounter(recipeId, isLiked = isCurrentlyLiked)
                    // TODO: Notificar error UI
                } else { Log.i("ViewModelInteraction", "Like/Unlike API OK para $recipeId") }
            } catch (e: Exception) {
                Log.e("ViewModelInteraction", "Excepción Like/Unlike", e)
                _likedRecipeIds.update { if (!isCurrentlyLiked) it - recipeId else it + recipeId }
                updateRecipeListCounter(recipeId, isLiked = isCurrentlyLiked)
                // TODO: Notificar error UI
            }
        }
    }

    fun toggleSave(recipeId: Long) {
        val token = idToken ?: run { Log.e("ViewModelInteraction", "toggleSave sin token!"); return }
        val bearerToken = "Bearer $token"
        val isCurrentlySaved = _savedRecipeIds.value.contains(recipeId)
        Log.d("ViewModelInteraction", "Toggle Save ID: $recipeId. Actual: $isCurrentlySaved")
        _savedRecipeIds.update { if (isCurrentlySaved) it - recipeId else it + recipeId } // Optimista
        updateRecipeListCounter(recipeId, isSaved = !isCurrentlySaved)
        viewModelScope.launch {
            try {
                val response = if (isCurrentlySaved) apiService.unsaveRecipe(bearerToken, recipeId) else apiService.saveRecipe(bearerToken, recipeId)
                if (!response.isSuccessful) {
                    Log.e("ViewModelInteraction", "Error API Save/Unsave: ${response.code()}. Revirtiendo UI.")
                    _savedRecipeIds.update { if (!isCurrentlySaved) it - recipeId else it + recipeId }
                    updateRecipeListCounter(recipeId, isSaved = isCurrentlySaved)
                    // TODO: Notificar error
                } else { Log.i("ViewModelInteraction", "Save/Unsave API OK para $recipeId") }
            } catch (e: Exception) {
                Log.e("ViewModelInteraction", "Excepción Save/Unsave", e)
                _savedRecipeIds.update { if (!isCurrentlySaved) it - recipeId else it + recipeId }
                updateRecipeListCounter(recipeId, isSaved = isCurrentlySaved)
                // TODO: Notificar error
            }
        }
    }

    fun postComment(recipeId: Long, commentText: String) {
        val token = idToken ?: run { Log.w("ViewModelComment", "Token nulo al postear"); return }
        val bearerToken = "Bearer $token"
        if (commentText.isBlank()) return
        Log.d("ViewModelComment", "Posteando comentario en $recipeId")
        viewModelScope.launch {
            try {
                val commentData = mapOf("texto" to commentText)
                val newComment = apiService.postComment(bearerToken, recipeId, commentData)
                Log.i("ViewModelComment", "Comentario posteado ID: ${newComment.id}")
                _recipeDetailState.update {
                    if (it.recipe?.id == recipeId) it.copy(comments = it.comments + newComment) else it
                }
            } catch (e: Exception) { handleApiError("postComment", e){ Log.e("ViewModelComment", it) /* TODO */ } }
        }
    }

    fun deleteComment(recipeId: Long, commentId: Long) {
        val token = idToken ?: run { Log.w("ViewModelComment", "Token nulo al borrar"); return }
        val bearerToken = "Bearer $token"
        Log.d("ViewModelComment", "Borrando comentario $commentId de $recipeId")
        viewModelScope.launch {
            try {
                val response = apiService.deleteComment(bearerToken, recipeId, commentId)
                if(response.isSuccessful) {
                    Log.i("ViewModelComment", "Comentario $commentId borrado OK")
                    _recipeDetailState.update {
                        if (it.recipe?.id == recipeId) it.copy(comments = it.comments.filterNot { c -> c.id == commentId }) else it
                    }
                } else { Log.e("ViewModelComment", "Error API borrando: ${response.code()}") /* TODO */}
            } catch (e: Exception) { handleApiError("deleteComment", e){ Log.e("ViewModelComment", it) /* TODO */ } }
        }
    }

    // --- Cargar Perfiles ---
    fun loadPublicProfile(firebaseUid: String) {
        _publicProfileState.value = PublicProfileState.Loading
        Log.d("ViewModelProfile", "Cargando perfil público UID: $firebaseUid")
        viewModelScope.launch {
            try {
                val profile = apiService.getPublicProfile(firebaseUid)
                _publicProfileState.value = PublicProfileState.Success(profile)
            } catch (e: Exception) {
                handleApiError("loadPublicProfile", e) { msg -> _publicProfileState.value = PublicProfileState.Error(msg) }
            }
        }
    }

    fun loadMyProfile() {
        val token = idToken ?: run { _myProfileState.value = MyProfileState.Error("No autenticado"); return }
        _myProfileState.value = MyProfileState.Loading
        Log.d("ViewModelProfile", "Cargando mi perfil privado...")
        viewModelScope.launch {
            try {
                val profile = apiService.getMyProfile("Bearer $token")
                _myProfileState.value = MyProfileState.Success(profile)
                // Actualizar sets locales
                _likedRecipeIds.value = profile.recetasLikeadas?.mapNotNull { it.id }?.toSet() ?: emptySet()
                _savedRecipeIds.value = profile.recetasGuardadas?.mapNotNull { it.id }?.toSet() ?: emptySet()
            } catch (e: Exception) {
                handleApiError("loadMyProfile", e) { msg -> _myProfileState.value = MyProfileState.Error(msg) }
            }
        }
    }

    // --- Helpers ---
    private fun updateRecipeListCounter(recipeId: Long, isLiked: Boolean? = null, isSaved: Boolean? = null) {
        val currentState = _recipeListState.value
        if (currentState is RecipeListState.Success) {
            val updatedList = currentState.recipes.map { recipe ->
                if (recipe.id == recipeId) {
                    val newLikes = if (isLiked != null) ((recipe.likesCount ?: 0) + (if (isLiked) 1 else -1)).coerceAtLeast(0) else recipe.likesCount
                    val newSaves = if (isSaved != null) ((recipe.guardadosCount ?: 0) + (if (isSaved) 1 else -1)).coerceAtLeast(0) else recipe.guardadosCount
                    recipe.copy(likesCount = newLikes, guardadosCount = newSaves)
                } else { recipe }
            }
            _recipeListState.value = RecipeListState.Success(updatedList)
        }
    }

    private fun handleApiError(functionName: String, e: Exception, onErrorState: (String) -> Unit) {
        val errorMsg = when(e) {
            is IOException -> "Error de Red"
            is HttpException -> "Error del Servidor (${e.code()})"
            else -> "Error inesperado"
        }
        val detailedMessage = "$errorMsg: ${e.localizedMessage}"
        Log.e("ViewModelAPIError", "Error en '$functionName': $detailedMessage", e)
        onErrorState(detailedMessage)
    }
}