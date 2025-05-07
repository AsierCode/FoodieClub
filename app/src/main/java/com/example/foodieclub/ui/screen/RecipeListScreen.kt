@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class) // Añadir ExperimentalComposeUiApi

package com.example.foodieclub.ui.screen // Revisa tu paquete

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions // Para acción del teclado
import androidx.compose.foundation.text.KeyboardOptions // Para acción del teclado
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable // Para guardar estado de búsqueda
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager // Para quitar foco
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController // Para ocultar teclado
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction // Para acción del teclado
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.foodieclub.R
import com.example.foodieclub.data.model.RecetaDto
import com.example.foodieclub.data.model.UsuarioDto
import com.example.foodieclub.ui.theme.FoodieClubTheme
import com.example.foodieclub.ui.viewmodel.RecipeListState
import com.example.foodieclub.ui.viewmodel.RecipeViewModel

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun RecipeListScreen(
    recipeViewModel: RecipeViewModel = viewModel(),
    onSignOutClick: () -> Unit,
    onRecipeClick: (Long) -> Unit,
    onAddRecipeClick: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToMyProfile: () -> Unit
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val listState by recipeViewModel.recipeListState.collectAsStateWithLifecycle(lifecycle = lifecycle)
    val likedIds by recipeViewModel.likedRecipeIds.collectAsStateWithLifecycle(lifecycle = lifecycle)
    val savedIds by recipeViewModel.savedRecipeIds.collectAsStateWithLifecycle(lifecycle = lifecycle)

    // --- Estado para la búsqueda ---
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    // -----------------------------

    // Efecto para cargar interacciones si el token cambia mientras vemos la lista
    LaunchedEffect(recipeViewModel.idToken, listState) {
        // Cargar interacciones si hay token y la lista está visible (Success)
        if (recipeViewModel.idToken != null && listState is RecipeListState.Success) {
            Log.d("RecipeListScreen", "Token/Lista detectado, recargando interacciones.")
            recipeViewModel.loadUserInteractions()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                            placeholder = { Text("Buscar recetas...") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                Log.d("SearchAction", "Buscando por teclado: $searchQuery")
                                recipeViewModel.searchRecipes(searchQuery)
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }),
                            colors = TextFieldDefaults.colors( // Material 3
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent, // Añadido por si acaso
                                // Puedes descomentar esto para quitar la línea indicadora si prefieres
                                // focusedIndicatorColor = Color.Transparent,
                                // unfocusedIndicatorColor = Color.Transparent,
                                // disabledIndicatorColor = Color.Transparent,
                                // errorIndicatorColor = Color.Transparent
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge // Para que coincida aprox con el título
                        )
                    } else {
                        Text("FoodieClub Recetas")
                    }
                },
                actions = {
                    if (isSearchActive) {
                        // Botón Limpiar/Cerrar Búsqueda
                        IconButton(onClick = {
                            Log.d("SearchAction", "Limpiando/Cerrando búsqueda")
                            if (searchQuery.isNotEmpty()) {
                                searchQuery = "" // Limpiar texto
                                recipeViewModel.loadRecipes() // Volver a cargar todo
                            }
                            isSearchActive = false // Ocultar campo
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cerrar búsqueda")
                        }
                    } else {
                        // Botón Activar Búsqueda
                        IconButton(onClick = {
                            Log.d("SearchAction", "Activando búsqueda")
                            isSearchActive = true
                            // Podríamos pedir foco aquí si quisiéramos
                            // focusManager.requestFocus(/*...necesitaría FocusRequester...*/)
                        }) {
                            Icon(Icons.Filled.Search, contentDescription = "Buscar")
                        }
                    }
                    // Botones existentes
                    IconButton(onClick = onNavigateToMyProfile) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Mi Perfil")
                    }
                    IconButton(onClick = onSignOutClick) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar Sesión")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer // Si tuvieras nav icon
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
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                is RecipeListState.Success -> {
                    // Mostrar mensaje si es resultado de búsqueda Y no hay recetas
                    if (currentState.isSearchResult && currentState.recipes.isEmpty()) {
                        EmptyStateMessage("No se encontraron recetas para \"$searchQuery\"")
                    }
                    // Mostrar lista vacía normal si NO es búsqueda y no hay recetas
                    else if (!currentState.isSearchResult && currentState.recipes.isEmpty()) {
                        EmptyStateMessage("Aún no hay recetas publicadas.\n¡Sé el primero en añadir una!")
                    }
                    // Mostrar la lista si hay recetas (sea búsqueda o no)
                    else {
                        RecipeList(
                            recipes = currentState.recipes,
                            likedRecipeIds = likedIds,
                            savedRecipeIds = savedIds,
                            onItemClick = onRecipeClick,
                            onLikeClick = { recipeId -> recipeViewModel.toggleLike(recipeId) },
                            onSaveClick = { recipeId -> recipeViewModel.toggleSave(recipeId) },
                            onNavigateToProfile = onNavigateToProfile
                        )
                    }
                }
                is RecipeListState.Error -> {
                    val errorTitle = if (currentState.isSearchError) "Error en la búsqueda" else "Error al cargar recetas"
                    val buttonAction = {
                        if (currentState.isSearchError) recipeViewModel.searchRecipes(searchQuery)
                        else recipeViewModel.loadRecipes()
                    }
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(painter = painterResource(id = R.drawable.ic_broken_image_background), contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(errorTitle, style = MaterialTheme.typography.titleMedium , color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(currentState.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = buttonAction) { Text("Reintentar") }
                    }
                }
            }
        }
    }
}

// --- RecipeList Composable ---
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
    // Ya no necesitamos el estado vacío aquí, se maneja en el when de RecipeListScreen
    // if (recipes.isEmpty()) { ... }

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
            } else { Log.w("RecipeList", "Receta encontrada con ID nulo: ${recipe.titulo}") }
        }
    }
}

// --- RecipeListItem Composable (Asegúrate que esté definido aquí o importado) ---
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
    Log.d("RecipeListItemRecomposition", "Item ID: ${recipe.id}, isLiked: $isLiked, isSaved: $isSaved")
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column {
            if (!recipe.imagenUrl.isNullOrBlank()) {
                AsyncImage( model = recipe.imagenUrl, contentDescription = "Imagen de ${recipe.titulo}", modifier = Modifier.fillMaxWidth().height(180.dp), contentScale = ContentScale.Crop, placeholder = painterResource(id = R.drawable.ic_launcher_background), error = painterResource(id = R.drawable.ic_broken_image_background))
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Icon(painter = painterResource(id = R.drawable.ic_launcher_foreground), contentDescription = "Sin imagen", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(recipe.titulo ?: "Sin título", style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Text(recipe.descripcion ?: "Sin descripción", style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    val authorName = recipe.usuario?.nombreMostrado ?: recipe.usuario?.email ?: "Anónimo"
                    val authorUid = recipe.usuario?.firebaseUid
                    Text( text = "Por: $authorName", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp).clickable(enabled = authorUid != null) { authorUid?.let(onNavigateToProfile) }, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (authorUid != null) MaterialTheme.colorScheme.primary else LocalContentColor.current)
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


// --- Helper de estado vacío ---
@Composable
fun EmptyStateMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// --- Previews ---
@Preview(showBackground = true, name = "Recipe List Screen - Success")
@Composable
fun RecipeListScreenSuccessPreview() {
    val sampleUser = UsuarioDto("uid1","test@user.com", "Asier Preview", null)
    val sampleUser2 = UsuarioDto("uid2","otro@user.com", "Otro User", null)
    val sampleRecipes = listOf(
        RecetaDto(1L,"Lentejas de la Abuela (Preview)","Un plato clásico lleno de sabor.", "Lentejas\nVerduras", "1. Cocer...", 60, 4, sampleUser2, null, "2024-01-01", "2024-01-01", 15, 5),
        RecetaDto(2L,"Tarta de Queso Fácil (Preview)","Sin horno, ¡increíblemente cremosa!", "Queso\nNata\nGalletas", "1. Base\n2. Relleno", 20, 8, sampleUser, "https://via.placeholder.com/600x400.png?text=Tarta+Queso", "2024-01-02", "2024-01-03", 132, 45)
    )
    val sampleLikedIds = remember { setOf(2L) }
    val sampleSavedIds = remember { setOf(1L) }

    FoodieClubTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Preview Recetas") }, actions = {
                IconButton({}){ Icon(Icons.Filled.Search, "")} // Icono Búsqueda
                IconButton({}){ Icon(Icons.Filled.AccountCircle, "")}
                IconButton({}){ Icon(Icons.AutoMirrored.Filled.ExitToApp, "")}
            })},
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
            Icon(painter = painterResource(id = R.drawable.ic_broken_image_background), contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Error al cargar recetas:", style = MaterialTheme.typography.titleMedium , color = MaterialTheme.colorScheme.error)
            Text("Error de red simulado", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { /* No-op */ }) { Text("Reintentar") }
        }
    }
}

@Preview(showBackground = true, name = "Recipe List - Success (Empty Search)")
@Composable
fun RecipeListSuccessEmptySearchPreview() {
    FoodieClubTheme {
        EmptyStateMessage("No se encontraron recetas para \"Búsqueda Vacía\"")
    }
}

@Preview(showBackground = true, name = "Recipe List - Success (Empty Initial)")
@Composable
fun RecipeListSuccessEmptyInitialPreview() {
    FoodieClubTheme {
        EmptyStateMessage("Aún no hay recetas publicadas.\n¡Sé el primero en añadir una!")
    }
}