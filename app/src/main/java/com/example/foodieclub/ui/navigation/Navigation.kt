package com.example.foodieclub.ui.navigation // O tu paquete preferido

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle // No usado directamente en BottomBar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Article // Icono para Noticias
import androidx.compose.material.icons.outlined.ListAlt // Icono alternativo para Noticias
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Define todas las posibles rutas/destinos de navegación en la aplicación.
 */
sealed class Screen(
    val route: String,
    val title: String? = null, // Título para BottomBar o TopAppBar
    val icon: ImageVector? = null // Icono para BottomBar
) {
    // --- Grafo de Autenticación ---
    object AuthRoot : Screen("auth_graph")
    object Login : Screen("login_screen", "Iniciar Sesión")
    object Register : Screen("register_screen", "Registro")

    // --- Grafo Principal (Accesible desde Bottom Bar) ---
    object MainRoot : Screen("main_graph")
    object RecipeList : Screen("recipe_list_screen", "Inicio", Icons.Filled.Home)
    object News : Screen("news_screen", "Noticias", Icons.Outlined.Article) // <-- NOTICIAS EN BOTTOM BAR
    object MyProfile : Screen("my_profile_screen", "Mi Perfil", Icons.Filled.AccountCircle)
    object Settings : Screen("settings_screen", "Ajustes", Icons.Filled.Settings)

    // --- Pantallas Secundarias (No directamente en Bottom Bar) ---
    object CreateRecipe : Screen("create_recipe_screen", "Crear Receta") // Añadido título
    object History : Screen("history_screen", "Historial") // Añadido título
    object ArticleDetail : Screen("article_detail_screen/{articleId}") { // Detalle Artículo
        const val ARG_ARTICLE_ID = "articleId"
        val routeWithArgs = "article_detail_screen/{$ARG_ARTICLE_ID}"
        fun createRoute(articleId: String) = "article_detail_screen/$articleId"
    }
    object RecipeDetail : Screen("recipe_detail_screen/{recipeId}") { // Detalle Receta
        const val ARG_RECIPE_ID = "recipeId"
        val routeWithArgs = "recipe_detail_screen/{$ARG_RECIPE_ID}"
        fun createRoute(recipeId: Long) = "recipe_detail_screen/$recipeId"
    }
    object UserProfile : Screen("user_profile_screen/{userId}") { // Perfil Público
        const val ARG_USER_ID = "userId"
        val routeWithArgs = "user_profile_screen/{$ARG_USER_ID}"
        fun createRoute(userId: String) = "user_profile_screen/$userId"
    }
}

// --- Lista de ítems para la Bottom Navigation Bar ---
// El orden aquí define el orden en la UI
val bottomNavItems = listOf(
    Screen.RecipeList,  // Inicio
    Screen.News,        // Noticias
    Screen.MyProfile,   // Mi Perfil
    Screen.Settings     // Ajustes
)