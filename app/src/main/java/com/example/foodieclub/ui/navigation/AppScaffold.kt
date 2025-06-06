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
import com.example.foodieclub.ui.viewmodel.ProfileViewModel
import com.example.foodieclub.ui.viewmodel.RecipeViewModel
import com.example.foodieclub.ui.viewmodel.SettingsViewModel // <-- IMPORT NUEVO

// --- Composable para la Barra de Navegación Inferior ---
@Composable
fun AppBottomNavigationBar(navController: NavHostController) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        // Usar la lista actualizada desde Navigation.kt
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

// --- Composable Principal que configura el Scaffold con BottomBar y NavHost ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    recipeViewModel: RecipeViewModel,
    profileViewModel: ProfileViewModel,
    settingsViewModel: SettingsViewModel, // <-- AÑADIDO: Recibir SettingsViewModel
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { AppBottomNavigationBar(navController = navController) }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            recipeViewModel = recipeViewModel,
            profileViewModel = profileViewModel,
            settingsViewModel = settingsViewModel, // <-- AÑADIDO: Pasar SettingsViewModel a AppNavHost
            onSignOut = onSignOut
        )
    }
}