@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.foodieclub.ui.screen // Revisa tu paquete

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue // Necesario para la delegación 'by'
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner // <-- IMPORT NECESARIO
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
// --- Import clave para el estado ---
import androidx.lifecycle.compose.collectAsStateWithLifecycle
// ----------------------------------
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
    onAddRecipeClick: () -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    // --- Obtener Lifecycle explícitamente ---
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    // ----------------------------------------

    // --- Recolectar estado pasando el lifecycle explícitamente ---
    // Se usa 'by' para la delegación, el acceso posterior será directo (sin .value)
    val listState by recipeViewModel.recipeListState.collectAsStateWithLifecycle(lifecycle = lifecycle)
    val likedIds by recipeViewModel.likedRecipeIds.collectAsStateWithLifecycle(lifecycle = lifecycle)
    val savedIds by recipeViewModel.savedRecipeIds.collectAsStateWithLifecycle(lifecycle = lifecycle)
    // -----------------------------------------------------------


    // --- LaunchedEffects ---
    // Acceder al estado directamente (gracias a 'by')
    LaunchedEffect(listState) {
        if (listState is RecipeListState.Loading) {
            recipeViewModel.loadRecipes()
        }
    }
    LaunchedEffect(recipeViewModel.idToken) {
        if (recipeViewModel.idToken != null) {
            recipeViewModel.loadUserInteractions()
        }
    }
    // ---------------------

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
            // --- Acceder al estado directamente (gracias a 'by') ---
            when (val currentState = listState) {
                is RecipeListState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is RecipeListState.Success -> {
                    RecipeList(
                        recipes = currentState.recipes,
                        likedRecipeIds = likedIds, // Usar directamente
                        savedRecipeIds = savedIds, // Usar directamente
                        onItemClick = onRecipeClick,
                        onLikeClick = { recipeId -> recipeViewModel.toggleLike(recipeId) },
                        onSaveClick = { recipeId -> recipeViewModel.toggleSave(recipeId) },
                        onNavigateToProfile = onNavigateToProfile
                    )
                }
                is RecipeListState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_broken_image_background),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Error al cargar recetas:", style = MaterialTheme.typography.titleMedium , color = MaterialTheme.colorScheme.error)
                        Text(currentState.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
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

// --- RecipeList ---
@Composable
fun RecipeList(
    recipes: List<RecetaDto>,
    likedRecipeIds: Set<Long>,
    savedRecipeIds: Set<Long>,
    onItemClick: (Long) -> Unit,
    onLikeClick: (Long) -> Unit,
    onSaveClick: (Long) -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    if (recipes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = LocalContentColor.current.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(16.dp))
                Text("Aún no hay recetas publicadas.\n¡Sé el primero en añadir una!",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
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
                        onSaveClick = { onSaveClick(recipe.id) },
                        onNavigateToProfile = onNavigateToProfile
                    )
                } else {
                    Log.w("RecipeList", "Receta encontrada con ID nulo: ${recipe.titulo}")
                }
            }
        }
    }
}

// --- RecipeListItem ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListItem(
    recipe: RecetaDto,
    isLiked: Boolean,
    isSaved: Boolean,
    onClick: () -> Unit,
    onLikeClick: () -> Unit,
    onSaveClick: () -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column {
            if (!recipe.imagenUrl.isNullOrBlank()) {
                AsyncImage(
                    model = recipe.imagenUrl,
                    contentDescription = "Imagen de ${recipe.titulo}",
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.ic_launcher_background),
                    error = painterResource(id = R.drawable.ic_broken_image_background)
                )
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "Sin imagen",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(recipe.titulo ?: "Sin título", style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Text(recipe.descripcion ?: "Sin descripción", style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val authorName = recipe.usuario?.nombreMostrado ?: recipe.usuario?.email ?: "Anónimo"
                    val authorUid = recipe.usuario?.firebaseUid
                    Text(
                        text = "Por: $authorName",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp).clickable(enabled = authorUid != null) { authorUid?.let { uid -> onNavigateToProfile(uid) } },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (authorUid != null) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onSaveClick, modifier = Modifier.size(40.dp)) {
                            Icon(imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = if (isSaved) "Quitar Guardado" else "Guardar", tint = if (isSaved) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                        }
                        IconButton(onClick = onLikeClick, modifier = Modifier.size(40.dp)) {
                            Icon(imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, contentDescription = if (isLiked) "Quitar Like" else "Dar Like", tint = if (isLiked) Color.Red else LocalContentColor.current)
                        }
                        Text("${recipe.likesCount ?: 0}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 2.dp))
                    }
                }
            }
        }
    }
}

// --- Previews (Sin cambios) ---
@Preview(showBackground = true, name = "Recipe List Screen - Success")
@Composable
fun RecipeListScreenSuccessPreview() {
    val sampleUser = UsuarioDto(firebaseUid = "test_firebase_uid_1", email = "test@user.com", nombreMostrado = "Asier Preview", fotoUrl = null)
    val sampleUser2 = UsuarioDto(firebaseUid = "test_firebase_uid_2", email = "otro@user.com", nombreMostrado = "Otro User", fotoUrl = null)
    val sampleRecipes = listOf(
        RecetaDto(1L,"Lentejas de la Abuela (Preview)","Un plato clásico lleno de sabor.", "Lentejas\nVerduras", "1. Cocer...", 60, 4, sampleUser2, null, "2024-01-01", "2024-01-01", 15, 5),
        RecetaDto(2L,"Tarta de Queso Fácil (Preview)","Sin horno, ¡increíblemente cremosa!", "Queso\nNata\nGalletas", "1. Base\n2. Relleno", 20, 8, sampleUser, "https://via.placeholder.com/600x400.png?text=Tarta+Queso", "2024-01-02", "2024-01-03", 132, 45)
    )
    val sampleLikedIds = remember { setOf(2L) }
    val sampleSavedIds = remember { setOf(1L) }

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
                    onSaveClick = { Log.d("Preview", "Click Guardar $it") },
                    onNavigateToProfile = { userId -> Log.d("Preview", "Click Perfil $userId") }
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
                painter = painterResource(id = R.drawable.ic_broken_image_background),
                contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Error al cargar recetas:", style = MaterialTheme.typography.titleMedium , color = MaterialTheme.colorScheme.error)
            Text("Error de red simulado", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { /* No-op para preview */ }) { Text("Reintentar") }
        }
    }
}

@Preview(showBackground = true, name = "Recipe List - Success (Empty)")
@Composable
fun RecipeListSuccessEmptyPreview() {
    FoodieClubTheme {
        RecipeList(recipes = emptyList(), likedRecipeIds = emptySet(), savedRecipeIds = emptySet(), onItemClick = {}, onLikeClick = {}, onSaveClick = {}, onNavigateToProfile = {})
    }
}