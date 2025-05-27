package com.example.foodieclub.ui.navigation

import android.app.Application
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.example.foodieclub.data.preferences.RecipeHistoryManager
import com.example.foodieclub.ui.screen.ArticleDetailScreen
import com.example.foodieclub.ui.screen.CreateRecipeScreen
import com.example.foodieclub.ui.screen.HistoryScreen
import com.example.foodieclub.ui.screen.LoginScreen
import com.example.foodieclub.ui.screen.MyProfileScreen
import com.example.foodieclub.ui.screen.NewsScreen
import com.example.foodieclub.ui.screen.ProfileScreen
import com.example.foodieclub.ui.screen.RecipeDetailScreen
import com.example.foodieclub.ui.screen.RecipeListScreen
import com.example.foodieclub.ui.screen.RegisterScreen
import com.example.foodieclub.ui.screen.SettingsScreen
import com.example.foodieclub.ui.screen.ShoppingListScreen // <-- IMPORTA LA NUEVA PANTALLA
import com.example.foodieclub.ui.viewmodel.AuthState
import com.example.foodieclub.ui.viewmodel.AuthViewModel
import com.example.foodieclub.ui.viewmodel.ProfileViewModel
import com.example.foodieclub.ui.viewmodel.RecipeDetailViewModel
import com.example.foodieclub.ui.viewmodel.RecipeDetailViewModelFactory
import com.example.foodieclub.ui.viewmodel.RecipeViewModel
import com.example.foodieclub.ui.viewmodel.SettingsViewModel
import com.example.foodieclub.ui.viewmodel.provideProfileViewModelFactory // Asumiendo que esta función existe y está definida

// --- GRAFO DE NAVEGACIÓN PRINCIPAL ---
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    recipeViewModel: RecipeViewModel, // ViewModel principal para listas, interacciones globales, etc.
    profileViewModel: ProfileViewModel, // ViewModel para el perfil del usuario actual
    settingsViewModel: SettingsViewModel, // ViewModel para ajustes
    onSignOut: () -> Unit
) {
    // Factory para ProfileViewModel de perfiles públicos
    val profileViewModelFactory = provideProfileViewModelFactory()
    // Contexto de aplicación para instanciar RecipeHistoryManager y pasar a factories
    val application = LocalContext.current.applicationContext as Application

    NavHost(
        navController = navController,
        startDestination = Screen.RecipeList.route,
        modifier = modifier,
        route = Screen.MainRoot.route,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        // --- Pantallas Principales (BottomBar) ---
        composable(Screen.RecipeList.route) {
            RecipeListScreen(
                recipeViewModel = recipeViewModel,
                onRecipeClick = { recipeId -> navController.navigate(Screen.RecipeDetail.createRoute(recipeId)) },
                onAddRecipeClick = { navController.navigate(Screen.CreateRecipe.route) },
                onNavigateToProfile = { userId -> navController.navigate(Screen.UserProfile.createRoute(userId)) },
                onSignOutClick = onSignOut
            )
        }
        composable(Screen.News.route) {
            NewsScreen(
                // NewsViewModel puede ser instanciado aquí si es específico de esta pantalla
                // newsViewModel = viewModel(),
                onNavigateToArticleDetail = { articleId ->
                    navController.navigate(Screen.ArticleDetail.createRoute(articleId))
                }
            )
        }

        // --- NUEVO DESTINO PARA LA LISTA DE LA COMPRA ---
        composable(Screen.ShoppingList.route) {
            ShoppingListScreen() // El ShoppingListViewModel se obtendrá con viewModel() por defecto dentro de la pantalla
        }
        // -------------------------------------------------

        composable(Screen.MyProfile.route) {
            MyProfileScreen(
                profileViewModel = profileViewModel, // ViewModel del perfil del usuario actual
                recipeViewModel = recipeViewModel, // Para listas de recetas likeadas/guardadas
                onNavigateToRecipeDetail = { recipeId -> navController.navigate(Screen.RecipeDetail.createRoute(recipeId)) },
                onNavigateToProfile = { userId -> navController.navigate(Screen.UserProfile.createRoute(userId)) },
                onSignOutClick = onSignOut,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                settingsViewModel = settingsViewModel,
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateUp = { navController.popBackStack() }
            )
        }

        // --- Otras Pantallas ---
        composable(Screen.CreateRecipe.route) {
            CreateRecipeScreen(
                recipeViewModel = recipeViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                recipeViewModel = recipeViewModel, // El RecipeViewModel principal maneja el historial
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRecipeDetail = { recipeId ->
                    navController.navigate(Screen.RecipeDetail.createRoute(recipeId))
                },
                onNavigateToProfile = { userId ->
                    navController.navigate(Screen.UserProfile.createRoute(userId))
                }
            )
        }
        composable(Screen.ArticleDetail.routeWithArgs,
            arguments = listOf(navArgument(Screen.ArticleDetail.ARG_ARTICLE_ID) { type = NavType.StringType }),
            // Transiciones
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(350)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it/3 }, animationSpec = tween(350)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it/3 }, animationSpec = tween(350)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(350)) }
        ) { backStackEntry ->
            val articleId = backStackEntry.arguments?.getString(Screen.ArticleDetail.ARG_ARTICLE_ID)
            if (articleId != null && articleId.isNotBlank()) {
                ArticleDetailScreen(
                    articleId = articleId,
                    // ArticleDetailViewModel se crea dentro de la pantalla con su key si se necesita
                    // o se puede usar una factory si es necesario
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                navController.popBackStack() // O manejar error/redirigir
            }
        }
        composable(Screen.RecipeDetail.routeWithArgs,
            arguments = listOf(navArgument(Screen.RecipeDetail.ARG_RECIPE_ID) { type = NavType.LongType }),
            // Transiciones
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(350)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it/3 }, animationSpec = tween(350)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it/3 }, animationSpec = tween(350)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(350)) }
        ) { backStackEntry ->
            val recipeIdArg = backStackEntry.arguments?.getLong(Screen.RecipeDetail.ARG_RECIPE_ID)
            if (recipeIdArg != null) {
                // Instanciar RecipeHistoryManager una vez aquí o pasarlo desde un nivel superior si es un singleton
                val recipeHistoryManager = remember { RecipeHistoryManager(application) }
                val factory = RecipeDetailViewModelFactory(application, recipeIdArg, recipeHistoryManager, recipeViewModel)
                val detailViewModel: RecipeDetailViewModel = viewModel(factory = factory, key = "recipe_detail_vm_$recipeIdArg")

                RecipeDetailScreen(
                    // recipeId ya no se pasa directamente, el ViewModel lo tiene
                    recipeDetailViewModel = detailViewModel,
                    mainRecipeViewModel = recipeViewModel, // Para observar likedIds, savedIds
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToProfile = { userId -> navController.navigate(Screen.UserProfile.createRoute(userId)) }
                )
            } else {
                navController.popBackStack() // O manejar error/redirigir
            }
        }
        composable(Screen.UserProfile.routeWithArgs,
            arguments = listOf(navArgument(Screen.UserProfile.ARG_USER_ID) { type = NavType.StringType }),
            // Transiciones
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(350)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it/3 }, animationSpec = tween(350)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it/3 }, animationSpec = tween(350)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(350)) }
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString(Screen.UserProfile.ARG_USER_ID)
            if (userId != null && userId.isNotBlank()) {
                val userProfileViewModel: ProfileViewModel = viewModel(key = "user_profile_vm_$userId", factory = profileViewModelFactory)
                ProfileScreen(
                    firebaseUid = userId,
                    viewModel = userProfileViewModel, // ViewModel específico para este perfil público
                    recipeViewModel = recipeViewModel, // Para likes/saves en las recetas de este perfil
                    onNavigateToRecipeDetail = { recipeId -> navController.navigate(Screen.RecipeDetail.createRoute(recipeId)) },
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                navController.popBackStack() // O manejar error/redirigir
            }
        }
    }
}

// --- GRAFO DE NAVEGACIÓN PARA AUTENTICACIÓN ---
fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    onLoginSuccess: () -> Unit
) {
    navigation(
        startDestination = Screen.Login.route,
        route = Screen.AuthRoot.route
    ) {
        composable(Screen.Login.route) {
            val authViewModel: AuthViewModel = viewModel() // Asume que AuthViewModel no necesita factory o la obtiene por defecto
            val authState by authViewModel.authState.collectAsState()
            LaunchedEffect(authState) {
                if (authState is AuthState.Authenticated) {
                    onLoginSuccess()
                    // authViewModel.resetAuthState() // Considera si resetear aquí o en el destino
                }
            }
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = { /* Ya no se necesita aquí, manejado por LaunchedEffect */ },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }
        composable(Screen.Register.route) {
            val authViewModel: AuthViewModel = viewModel()
            val authState by authViewModel.authState.collectAsState()
            // No es necesario LaunchedEffect aquí si el registro no navega automáticamente al mismo sitio que el login
            // o si la navegación se maneja de otra forma.
            RegisterScreen(
                authViewModel = authViewModel,
                onRegisterSuccess = {
                    // Podrías querer navegar al login o directamente a home si el registro también autentica
                    // Por ahora, lo dejamos para que el usuario inicie sesión manualmente después del registro
                    // o para que navegues desde el RegisterScreen si el AuthState cambia a Authenticated.
                    // Ejemplo: navController.popBackStack() para volver al Login
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
    }
}