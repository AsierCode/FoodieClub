package com.example.foodieclub.ui.navigation // O tu paquete preferido

import androidx.compose.material.icons.Icons
// Importa solo los iconos que realmente vas a usar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AccountCircle
// Si decides añadir "Crear" a la barra, necesitarás AddCircle u otro
// import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Define todas las posibles rutas/destinos de navegación en la aplicación.
 * Cada objeto representa una pantalla o un grupo de pantallas.
 */
sealed class Screen(
    val route: String,          // Identificador único de la ruta (usado por NavController)
    val title: String? = null,  // Título opcional (para TopAppBar o ítems de BottomBar)
    val icon: ImageVector? = null // Icono opcional (para ítems de BottomBar)
) {
    // --- Pantallas que no suelen estar en la Bottom Navigation Bar ---
    object AuthFlow : Screen("auth_flow_screen") // Podría ser un NavGraph anidado para el flujo de autenticación
    object AuthNeeded : Screen("auth_needed_screen") // Pantalla que indica que se requiere login

    // --- Pantallas Principales (potenciales para Bottom Navigation Bar) ---
    object RecipeList : Screen(
        route = "recipe_list",
        title = "Inicio",
        icon = Icons.Filled.Home
    )
    object Search : Screen( // Si decides tener una pestaña de búsqueda dedicada
        route = "search",
        title = "Buscar",
        icon = Icons.Filled.Search
    )
    object CreateRecipe : Screen(
        route = "create_recipe",
        // title = "Crear", // Título e icono si va en la BottomBar
        // icon = Icons.Filled.AddCircle
    )
    object MyProfile : Screen(
        route = "my_profile",
        title = "Mi Perfil",
        icon = Icons.Filled.AccountCircle
    )

    // --- Pantallas de Detalle o Secundarias (se navega a ellas con argumentos) ---
    object RecipeDetail : Screen("recipe_detail/{recipeId}") {
        // Función helper para construir la ruta con el argumento
        fun createRoute(recipeId: Long) = "recipe_detail/$recipeId"
    }
    object UserProfile : Screen("user_profile/{userId}") { // Para perfiles públicos de otros usuarios
        // Función helper para construir la ruta con el argumento
        fun createRoute(userId: String) = "user_profile/$userId"
    }

    // Puedes añadir más pantallas aquí según las necesites (ej: EditProfile, Settings, etc.)
}

// --- Lista de ítems que aparecerán en la Bottom Navigation Bar ---
// Define aquí qué pantallas de la sealed class Screen quieres en la barra inferior.
val bottomNavItems = listOf(
    Screen.RecipeList,
    // Screen.Search, // Descomenta si quieres "Buscar" como una pestaña principal
    // Screen.CreateRecipe, // Descomenta si quieres "Crear" como una pestaña (menos común, suele ser FAB)
    Screen.MyProfile
)