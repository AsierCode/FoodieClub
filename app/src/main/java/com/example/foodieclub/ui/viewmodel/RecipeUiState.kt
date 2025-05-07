package com.example.foodieclub.ui.viewmodel // O el paquete donde lo creaste (ej. ui.state)

// --- IMPORTS NECESARIOS PARA LOS DATOS DENTRO DE LOS ESTADOS ---
import com.example.foodieclub.data.model.ComentarioDto
import com.example.foodieclub.data.model.PerfilPrivadoDto
import com.example.foodieclub.data.model.PerfilPublicoDto
import com.example.foodieclub.data.model.RecetaDto

// --- Definiciones de Estados de UI ---

/** Estados posibles para la pantalla que muestra la lista principal de recetas */
sealed class RecipeListState {
    object Loading : RecipeListState()
    // --- MODIFICADO: Añadido isSearchResult ---
    data class Success(
        val recipes: List<RecetaDto>,
        val isSearchResult: Boolean = false // Indica si la lista es resultado de una búsqueda
    ) : RecipeListState()
    // --- MODIFICADO: Añadido isSearchError ---
    data class Error(
        val message: String,
        val isSearchError: Boolean = false // Indica si el error ocurrió durante una búsqueda
    ) : RecipeListState()
}

/** Estados posibles para la pantalla de creación de recetas */
sealed class CreateRecipeState {
    object Idle : CreateRecipeState()
    object Loading : CreateRecipeState()
    data class Success(val message: String) : CreateRecipeState()
    data class Error(val message: String) : CreateRecipeState()
}

/** Estado para la pantalla de detalle de una receta específica */
data class RecipeDetailState(
    val isLoading: Boolean = true,
    val recipe: RecetaDto? = null,
    val comments: List<ComentarioDto> = emptyList(),
    val errorMessage: String? = null
)

/** Estados posibles para la pantalla de perfil público de un usuario */
sealed class PublicProfileState {
    object Loading : PublicProfileState()
    data class Success(val profile: PerfilPublicoDto) : PublicProfileState()
    data class Error(val message: String) : PublicProfileState()
}