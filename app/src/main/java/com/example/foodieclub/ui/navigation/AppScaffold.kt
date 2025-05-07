package com.example.foodieclub.ui.navigation // O tu paquete preferido

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.foodieclub.ui.viewmodel.ProfileViewModel // Importar ProfileViewModel
import com.example.foodieclub.ui.viewmodel.RecipeViewModel // Importar RecipeViewModel

// --- Composable para la Barra de Navegación Inferior ---
@Composable
fun AppBottomNavigationBar(navController: NavHostController) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        bottomNavItems.forEach { screen ->
            NavigationBarItem(
                icon = {
                    screen.icon?.let { icon ->
                        Icon(icon, contentDescription = screen.title)
                    }
                },
                label = {
                    screen.title?.let { title ->
                        Text(title)
                    }
                },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

// --- Composable Principal que configura el Scaffold ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    // --- RECIBE LAS INSTANCIAS DE VIEWMODEL DESDE MainActivity ---
    recipeViewModel: RecipeViewModel,
    profileViewModel: ProfileViewModel, // Para "Mi Perfil"
    // ---------------------------------------------------------
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { AppBottomNavigationBar(navController = navController) }
    ) { innerPadding ->
        // Pasa las instancias de ViewModel y el callback onSignOut al NavHost
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            recipeViewModel = recipeViewModel,     // Pasar RecipeVM
            profileViewModel = profileViewModel,   // Pasar ProfileVM (para Mi Perfil)
            onSignOut = onSignOut
        )
    }
}