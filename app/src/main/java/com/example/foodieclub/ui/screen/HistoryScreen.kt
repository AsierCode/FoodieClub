// En ui/screen/HistoryScreen.kt
package com.example.foodieclub.ui.screen

import android.R.attr.top
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material3.*
import androidx.compose.material3.BottomAppBarDefaults.windowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel // Importa si usas viewModel() aquí
import com.example.foodieclub.ui.viewmodel.RecipeViewModel // Importa el ViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    // Asume que recibes la instancia correcta de RecipeViewModel desde AppNavHost
    recipeViewModel: RecipeViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToRecipeDetail: (Long) -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val historyRecipes by recipeViewModel.recipeHistory.collectAsState()
    val likedIds by recipeViewModel.likedRecipeIds.collectAsState()
    val savedIds by recipeViewModel.savedRecipeIds.collectAsState()

    var showClearConfirmationDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },                windowInsets = WindowInsets(top = 2.dp) // <-- CAMBIO APLICADO AQUÍ
                ,
                        actions = {
                    if (historyRecipes.isNotEmpty()) {
                        IconButton(onClick = { showClearConfirmationDialog = true }) {
                            Icon(Icons.Default.ClearAll, contentDescription = "Borrar historial")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (historyRecipes.isEmpty()) {
                // Puedes tener un Composable EmptyState dedicado o usar este Box
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Aún no has visto ninguna receta.\n¡Explora y aparecerán aquí!",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(historyRecipes, key = { it.id ?: it.hashCode() }) { recipe ->
                        val isLiked = likedIds.contains(recipe.id)
                        val isSaved = savedIds.contains(recipe.id)
                        // Llama a RecipeListItem pasando isFeatured = false
                        RecipeListItem(
                            recipe = recipe,
                            isLiked = isLiked,
                            isSaved = isSaved,
                            isFeatured = false, // <-- CORRECCIÓN: Pasar valor para isFeatured
                            onClick = { recipe.id?.let { onNavigateToRecipeDetail(it) } },
                            onLikeClick = { recipe.id?.let { recipeViewModel.toggleLike(it) } },
                            onSaveClick = { recipe.id?.let { recipeViewModel.toggleSave(it) } },
                            onNavigateToProfile = onNavigateToProfile
                        )
                    }
                }
            }
        }
    }

    // Diálogo de confirmación
    if (showClearConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmationDialog = false },
            title = { Text("Confirmar Borrado") },
            text = { Text("¿Estás seguro de que quieres borrar todo tu historial de recetas vistas?") },
            confirmButton = {
                Button(
                    onClick = {
                        recipeViewModel.clearRecipeHistory()
                        showClearConfirmationDialog = false
                    }
                ) {
                    Text("Borrar")
                }
            },
            dismissButton = {
                Button(onClick = { showClearConfirmationDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// Si tienes EmptyStateMessage como un Composable reutilizable en otro archivo,
// asegúrate de importarlo. Si no, puedes usar el Box como está arriba.