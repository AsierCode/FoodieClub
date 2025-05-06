package com.example.foodieclub.ui.viewmodel // O el paquete donde lo creaste (ej. ui.state)

// --- IMPORTS NECESARIOS PARA LOS DATOS DENTRO DE LOS ESTADOS ---
import com.example.foodieclub.data.model.ComentarioDto
import com.example.foodieclub.data.model.PerfilPrivadoDto
import com.example.foodieclub.data.model.PerfilPublicoDto
import com.example.foodieclub.data.model.RecetaDto

// --- Definiciones de Estados de UI ---

/** Estados posibles para la pantalla que muestra la lista principal de recetas */
sealed class RecipeListState {
    object Loading : RecipeListState()                      // Estado de carga inicial o al refrescar
    data class Success(val recipes: List<RecetaDto>) : RecipeListState() // Estado de éxito con la lista de recetas
    data class Error(val message: String) : RecipeListState()    // Estado de error con un mensaje descriptivo
}

/** Estados posibles para la pantalla de creación de recetas */
sealed class CreateRecipeState {
    object Idle : CreateRecipeState()      // Estado inicial, esperando acción o después de éxito/error reseteado
    object Loading : CreateRecipeState()   // Estado mientras se sube la imagen o se llama a la API
    data class Success(val message: String) : CreateRecipeState() // Estado de éxito con mensaje de confirmación
    data class Error(val message: String) : CreateRecipeState()    // Estado de error con mensaje descriptivo
}

/** Estado para la pantalla de detalle de una receta específica */
data class RecipeDetailState(
    val isLoading: Boolean = true,              // Indica si se están cargando los detalles/comentarios
    val recipe: RecetaDto? = null,              // El DTO de la receta cargada (null si aún no carga o error)
    val comments: List<ComentarioDto> = emptyList(), // La lista de comentarios asociada
    val errorMessage: String? = null            // Mensaje de error si la carga falla
    // Nota: isLikedByUser e isSavedByUser se manejan observando StateFlows separados en el ViewModel/UI
)

/** Estados posibles para la pantalla de perfil público de un usuario */
sealed class PublicProfileState {
    object Loading : PublicProfileState()
    data class Success(val profile: PerfilPublicoDto) : PublicProfileState() // Contiene el DTO del perfil público
    data class Error(val message: String) : PublicProfileState()
}

/** Estados posibles para la pantalla del perfil privado del usuario actual */
sealed class MyProfileState {
    object Loading : MyProfileState()
    data class Success(val profile: PerfilPrivadoDto) : MyProfileState() // Contiene el DTO del perfil privado
    data class Error(val message: String) : MyProfileState()
}