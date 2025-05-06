@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.foodieclub.ui.screen // Revisa tu paquete

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.foodieclub.R
import com.example.foodieclub.data.model.RecetaDto
import com.example.foodieclub.data.model.UsuarioDto
import com.example.foodieclub.ui.theme.FoodieClubTheme
import com.example.foodieclub.ui.viewmodel.RecipeListState
import com.example.foodieclub.ui.viewmodel.RecipeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(
    recipeViewModel: RecipeViewModel = viewModel(),
    onSignOutClick: () -> Unit,
    onRecipeClick: (Long) -> Unit,
    onAddRecipeClick: () -> Unit
) {
    val listState by recipeViewModel.recipeListState
    val likedIds by recipeViewModel.likedRecipeIds.collectAsState()
    val savedIds by recipeViewModel.savedRecipeIds.collectAsState()

    // Cargar interacciones del usuario cuando el token esté disponible
    LaunchedEffect(recipeViewModel.idToken) {
        if (recipeViewModel.idToken != null) {
            recipeViewModel.loadUserInteractions()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FoodieClub Recetas") },
                actions = {
                    IconButton(onClick = onSignOutClick) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar Sesión")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRecipeClick) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir Receta")
            }
        }
    ) { paddingValues ->

        Box(modifier = Modifier.padding(paddingValues)) {
            when (val currentState = listState) {
                is RecipeListState.Loading -> {
                    // --- Estado de Carga ---
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is RecipeListState.Success -> {
                    // --- Estado de Éxito ---
                    RecipeList(
                        recipes = currentState.recipes,
                        likedRecipeIds = likedIds, // Pasar Set de IDs
                        savedRecipeIds = savedIds, // Pasar Set de IDs
                        onItemClick = onRecipeClick,
                        onLikeClick = { recipeId -> recipeViewModel.toggleLike(recipeId) },
                        onSaveClick = { recipeId -> recipeViewModel.toggleSave(recipeId) }
                    )
                }
                is RecipeListState.Error -> {
                    // --- Estado de Error ---
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground), // Reemplaza con un icono de error si tienes
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Error al cargar recetas:", style = MaterialTheme.typography.titleMedium , color = MaterialTheme.colorScheme.error)
                        Text(currentState.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { recipeViewModel.loadRecipes() }) {
                            Text("Reintentar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeList(
    recipes: List<RecetaDto>,
    likedRecipeIds: Set<Long>,
    savedRecipeIds: Set<Long>,
    onItemClick: (Long) -> Unit,
    onLikeClick: (Long) -> Unit,
    onSaveClick: (Long) -> Unit
) {
    if (recipes.isEmpty()) {
        // --- Estado Lista Vacía ---
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground), // Reemplaza con icono apropiado
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = LocalContentColor.current.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(16.dp))
                Text("Aún no hay recetas publicadas.\n¡Sé el primero en añadir una!",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        // --- Lista con Recetas ---
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp), // Añadir padding horizontal
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(recipes, key = { recipe -> recipe.id ?: recipe.hashCode() }) { recipe ->
                if (recipe.id != null) {
                    val isLiked = likedRecipeIds.contains(recipe.id)
                    val isSaved = savedRecipeIds.contains(recipe.id)
                    RecipeListItem(
                        recipe = recipe,
                        isLiked = isLiked,
                        isSaved = isSaved,
                        onClick = { onItemClick(recipe.id) },
                        onLikeClick = { onLikeClick(recipe.id) },
                        onSaveClick = { onSaveClick(recipe.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListItem(
    recipe: RecetaDto,
    isLiked: Boolean,
    isSaved: Boolean,
    onClick: () -> Unit,
    onLikeClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium // Bordes redondeados
    ) {
        Column { // Quitamos padding aquí para que la imagen ocupe todo el ancho
            // Imagen (ocupa todo el ancho superior)
            if (!recipe.imagenUrl.isNullOrBlank()) {
                AsyncImage(
                    model = recipe.imagenUrl,
                    contentDescription = "Imagen de ${recipe.titulo}",
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.ic_launcher_background), // Un fondo simple mientras carga
                    error = painterResource(id = R.drawable.ic_launcher_background) // Lo mismo si hay error
                )
            } else {
                // Placeholder si no hay imagen
                Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground), // Icono genérico comida
                        contentDescription = "Sin imagen",
                        modifier = Modifier.size(48.dp).align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Contenido de texto e interacciones con padding
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(recipe.titulo ?: "Sin título", style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    recipe.descripcion ?: "Sin descripción",
                    style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis // 2 líneas en lista
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Por: ${recipe.usuario?.nombreMostrado ?: recipe.usuario?.email ?: "Anónimo"}",
                        style = MaterialTheme.typography.labelMedium // Un poco más grande
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onSaveClick, modifier = Modifier.size(40.dp)) { // Tamaño consistente
                            Icon(
                                imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (isSaved) "Quitar Guardado" else "Guardar",
                                tint = if (isSaved) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                        IconButton(onClick = onLikeClick, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isLiked) "Quitar Like" else "Dar Like",
                                tint = if (isLiked) Color.Red else LocalContentColor.current
                            )
                        }
                        // Contador de Likes más prominente
                        Text(
                            "${recipe.likesCount ?: 0}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 2.dp) // Pequeño espacio
                        )
                    }
                }
            }
        }
    }
}

// --- Previews ---

@Preview(showBackground = true, name = "Recipe List Screen - Success")
@Composable
fun RecipeListScreenSuccessPreview() {
    val sampleUser = UsuarioDto("uid1","test@user.com", "Asier Preview", null)
    val sampleRecipes = listOf(
        RecetaDto(1L,"Lentejas de la Abuela (Preview)","Un plato clásico lleno de sabor.", "Lentejas\nVerduras", "1. Cocer...", 60, 4, sampleUser, null, "","", 15, 8),
        RecetaDto(2L,"Tarta de Queso Fácil (Preview)","Sin horno, ¡increíblemente cremosa!", "Queso\nNata\nGalletas", "1. Base\n2. Relleno", 20, 8, sampleUser, "https://via.placeholder.com/600x400.png?text=Tarta+Queso", "","", 132, 45)
    )
    val sampleLikedIds = remember { setOf(2L) } // Like en la segunda
    val sampleSavedIds = remember { setOf(1L) } // Guardada la primera

    FoodieClubTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Preview Recetas") }, actions = { IconButton({}){ Icon(Icons.AutoMirrored.Filled.ExitToApp, "")}})},
            floatingActionButton = { FloatingActionButton(onClick = {}) { Icon(Icons.Filled.Add, "")} }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                RecipeList(
                    recipes = sampleRecipes,
                    likedRecipeIds = sampleLikedIds,
                    savedRecipeIds = sampleSavedIds,
                    onItemClick = { Log.d("Preview", "Click Receta $it") },
                    onLikeClick = { Log.d("Preview", "Click Like $it") },
                    onSaveClick = { Log.d("Preview", "Click Guardar $it") }
                )
            }
        }
    }
}


@Preview(showBackground = true, name = "Recipe List - Loading")
@Composable
fun RecipeListLoadingPreview() {
    FoodieClubTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
            Text("Cargando recetas...", modifier = Modifier.padding(top = 60.dp))
        }
    }
}

@Preview(showBackground = true, name = "Recipe List - Error")
@Composable
fun RecipeListErrorPreview() {
    FoodieClubTheme {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground), // Reemplaza icono error
                contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Error al cargar recetas:", style = MaterialTheme.typography.titleMedium , color = MaterialTheme.colorScheme.error)
            Text("Error de red simulado", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { /* No-op */ }) { Text("Reintentar") }
        }
    }
}

@Preview(showBackground = true, name = "Recipe List - Success (Empty)")
@Composable
fun RecipeListSuccessEmptyPreview() {
    FoodieClubTheme {
        RecipeList(recipes = emptyList(), likedRecipeIds = emptySet(), savedRecipeIds = emptySet(), onItemClick = {}, onLikeClick = {}, onSaveClick = {})
    }
}