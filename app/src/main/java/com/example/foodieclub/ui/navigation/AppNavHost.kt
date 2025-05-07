package com.example.foodieclub.ui.navigation // O tu paquete preferido

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider // Necesario para la Factory
import androidx.lifecycle.viewmodel.compose.viewModel // Para obtener ViewModels
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
// Importa TODAS tus clases Screen definidas en Navigation.kt
import com.example.foodieclub.ui.navigation.Screen
// Importa tus pantallas
import com.example.foodieclub.ui.screen.*
// Importa tus ViewModels y la Factory
import com.example.foodieclub.ui.viewmodel.RecipeViewModel
import com.example.foodieclub.ui.viewmodel.ProfileViewModel
import com.example.foodieclub.ui.viewmodel.provideProfileViewModelFactory // Tu helper para la factory

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    // --- RECIBE LAS INSTANCIAS DE VIEWMODEL ---
    recipeViewModel: RecipeViewModel,
    profileViewModel: ProfileViewModel, // Esta es la instancia para "Mi Perfil"
    // ----------------------------------------
    onSignOut: () -> Unit
) {
    // La factory para ProfileViewModel solo se necesita para crear instancias
    // para perfiles de OTROS usuarios (ProfileScreen).
    val profileViewModelFactory = provideProfileViewModelFactory()

    NavHost(
        navController = navController,
        startDestination = Screen.RecipeList.route,
        modifier = modifier
    ) {
        // --- Pantallas Principales ---

        composable(Screen.RecipeList.route) {
            RecipeListScreen(
                recipeViewModel = recipeViewModel, // Usar la instancia recibida
                onRecipeClick = { recipeId -> navController.navigate(Screen.RecipeDetail.createRoute(recipeId)) },
                onAddRecipeClick = { navController.navigate(Screen.CreateRecipe.route) },
                onNavigateToProfile = { userId -> navController.navigate(Screen.UserProfile.createRoute(userId)) },
                onNavigateToMyProfile = { navController.navigate(Screen.MyProfile.route) },
                onSignOutClick = onSignOut
            )
        }

        composable(Screen.MyProfile.route) {
            MyProfileScreen(
                profileViewModel = profileViewModel, // <-- USAR LA INSTANCIA RECIBIDA DE MainActivity
                recipeViewModel = recipeViewModel,   // Usar la instancia recibida
                onNavigateToRecipeDetail = { recipeId -> navController.navigate(Screen.RecipeDetail.createRoute(recipeId)) },
                onNavigateToProfile = { userId -> navController.navigate(Screen.UserProfile.createRoute(userId)) },
                onSignOutClick = onSignOut,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- Otras Pantallas ---

        composable(Screen.CreateRecipe.route) {
            CreateRecipeScreen(
                recipeViewModel = recipeViewModel, // Usar la instancia recibida
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.RecipeDetail.route,
            arguments = listOf(navArgument("recipeId") { type = NavType.LongType })
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getLong("recipeId")
            if (recipeId != null) {
                RecipeDetailScreen(
                    recipeId = recipeId,
                    recipeViewModel = recipeViewModel, // Usar la instancia recibida
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToProfile = { userId -> navController.navigate(Screen.UserProfile.createRoute(userId)) }
                )
            } else { navController.popBackStack() }
        }

        composable(
            route = Screen.UserProfile.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            if (userId != null) {
                // Para perfiles públicos de OTROS usuarios, crear una instancia específica de ProfileViewModel
                val userProfileViewModel: ProfileViewModel = viewModel(
                    key = "user_profile_vm_$userId", // Key única basada en userId
                    factory = profileViewModelFactory // Usar la factory
                )
                ProfileScreen(
                    firebaseUid = userId,
                    viewModel = userProfileViewModel, // Pasar esta instancia específica
                    onNavigateToRecipeDetail = { recipeId -> navController.navigate(Screen.RecipeDetail.createRoute(recipeId)) },
                    onNavigateBack = { navController.popBackStack() }
                )
            } else { navController.popBackStack() }
        }
    }
}