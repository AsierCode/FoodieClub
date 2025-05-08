@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.example.foodieclub.ui.screen // Revisa tu paquete

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.* // Incluye ArrowBack, Close, Search, etc.
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
    onNavigateToMyProfile: () -> Unit // Aunque se quite de la TopAppBar, el NavHost lo necesita
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val listState by recipeViewModel.recipeListState.collectAsStateWithLifecycle(lifecycle = lifecycle)
    val likedIds by recipeViewModel.likedRecipeIds.collectAsStateWithLifecycle(lifecycle = lifecycle)
    val savedIds by recipeViewModel.savedRecipeIds.collectAsStateWithLifecycle(lifecycle = lifecycle)

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(recipeViewModel.idToken, listState) {
        if (recipeViewModel.idToken != null && listState is RecipeListState.Success) {
            Log.d("RecipeListScreen", "Token/Lista detectado, recargando interacciones.")
            recipeViewModel.loadUserInteractions()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedVisibility(visible = !isSearchActive, enter = fadeIn(), exit = fadeOut()) {
                        Text("FoodieClub")
                    }
                },
                actions = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f).padding(start = 0.dp, end = 8.dp),
                            placeholder = { Text("Buscar...", style = MaterialTheme.typography.bodyMedium) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                recipeViewModel.searchRecipes(searchQuery); keyboardController?.hide(); focusManager.clearFocus()
                            }),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Limpiar")
                                    }
                                }
                            }
                        )
                        IconButton(onClick = {
                            isSearchActive = false; if (searchQuery.isNotEmpty()) { searchQuery = ""; recipeViewModel.loadRecipes() }
                            keyboardController?.hide(); focusManager.clearFocus()
                        }) { Icon(Icons.Filled.Close, contentDescription = "Cerrar búsqueda") }
                    } else {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Filled.Search, contentDescription = "Buscar")
                        }
                    }
                    // --- BOTÓN MI PERFIL ELIMINADO DE AQUÍ ---
                    IconButton(onClick = onSignOutClick) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar Sesión")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface, // Un color de superficie sutil
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp), // Color con elevación al hacer scroll
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                navigationIcon = {
                    if (isSearchActive) {
                        IconButton(onClick = {
                            isSearchActive = false; if (searchQuery.isNotEmpty()) { searchQuery = ""; recipeViewModel.loadRecipes() }
                            keyboardController?.hide(); focusManager.clearFocus()
                        }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Cancelar búsqueda") }
                    }
                    // else { // Icono de Hamburguesa si tuvieras un Drawer
                    //    IconButton(onClick = { /* Abrir Drawer */ }) { Icon(Icons.Filled.Menu, "Menú") }
                    // }
                }
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
                is RecipeListState.Loading -> { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                is RecipeListState.Success -> {
                    if (currentState.isSearchResult && currentState.recipes.isEmpty()) { EmptyStateMessage("No se encontraron recetas para \"$searchQuery\"") }
                    else if (!currentState.isSearchResult && currentState.recipes.isEmpty()) { EmptyStateMessage("Aún no hay recetas publicadas.\n¡Sé el primero en añadir una!") }
                    else {
                        RecipeList(recipes = currentState.recipes, likedRecipeIds = likedIds, savedRecipeIds = savedIds, onItemClick = onRecipeClick, onLikeClick = { recipeViewModel.toggleLike(it) }, onSaveClick = { recipeViewModel.toggleSave(it) }, onNavigateToProfile = onNavigateToProfile)
                    }
                }
                is RecipeListState.Error -> {
                    val errorTitle = if (currentState.isSearchError) "Error en la búsqueda" else "Error al cargar recetas"
                    val buttonAction = { if (currentState.isSearchError) recipeViewModel.searchRecipes(searchQuery) else recipeViewModel.loadRecipes() }
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painter = painterResource(id = R.drawable.ic_broken_image_background), contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp)); Text(errorTitle, style = MaterialTheme.typography.titleMedium , color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(4.dp)); Text(currentState.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp)); Button(onClick = buttonAction) { Text("Reintentar") }
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeList(recipes: List<RecetaDto>, likedRecipeIds: Set<Long>, savedRecipeIds: Set<Long>, onItemClick: (Long) -> Unit, onLikeClick: (Long) -> Unit, onSaveClick: (Long) -> Unit, onNavigateToProfile: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(all = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(recipes, key = { recipe -> recipe.id ?: recipe.hashCode() }) { recipe ->
            if (recipe.id != null) {
                val isLiked = likedRecipeIds.contains(recipe.id)
                val isSaved = savedRecipeIds.contains(recipe.id)
                RecipeListItem(recipe = recipe, isLiked = isLiked, isSaved = isSaved, onClick = { onItemClick(recipe.id) }, onLikeClick = { onLikeClick(recipe.id) }, onSaveClick = { onSaveClick(recipe.id) }, onNavigateToProfile = onNavigateToProfile)
            } else { Log.w("RecipeList", "Receta encontrada con ID nulo: ${recipe.titulo}") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListItem(recipe: RecetaDto, isLiked: Boolean, isSaved: Boolean, onClick: () -> Unit, onLikeClick: () -> Unit, onSaveClick: () -> Unit, onNavigateToProfile: (String) -> Unit) {
    Log.d("RecipeListItemRecomposition", "Item ID: ${recipe.id}, isLiked: $isLiked, isSaved: $isSaved")
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 4.dp), shape = MaterialTheme.shapes.medium) {
        Column {
            if (!recipe.imagenUrl.isNullOrBlank()) {
                AsyncImage(model = recipe.imagenUrl, contentDescription = "Imagen de ${recipe.titulo}", modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)), contentScale = ContentScale.Crop, placeholder = painterResource(id = R.drawable.ic_launcher_background), error = painterResource(id = R.drawable.ic_broken_image_background))
            } else {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Filled.RestaurantMenu, contentDescription = "Sin imagen", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(text = recipe.titulo ?: "Receta sin título", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(6.dp))
                val authorName = recipe.usuario?.nombreMostrado ?: recipe.usuario?.email ?: "Anónimo"; val authorUid = recipe.usuario?.firebaseUid
                if (authorUid != null) {
                    Text(text = "Por: $authorName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.clickable { onNavigateToProfile(authorUid) })
                    Spacer(modifier = Modifier.height(10.dp))
                } else { Spacer(modifier = Modifier.height(10.dp)) }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = if ((recipe.likesCount ?: 0) > 0) Arrangement.SpaceBetween else Arrangement.End) {
                    if ((recipe.likesCount ?: 0) > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Favorite, contentDescription = "Likes", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp)); Text(text = "${recipe.likesCount ?: 0}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    } else { Spacer(Modifier.weight(1f)) }
                    Row {
                        IconButton(onClick = onSaveClick, modifier = Modifier.size(36.dp)) { Icon(imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = if (isSaved) "Quitar Guardado" else "Guardar", tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                        IconButton(onClick = onLikeClick, modifier = Modifier.size(36.dp)) { Icon(imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, contentDescription = if (isLiked) "Quitar Like" else "Dar Like", tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(showBackground = true, name = "Recipe List Screen - Success")
@Composable
fun RecipeListScreenSuccessPreview() {
    val sampleUser = UsuarioDto("uid1","test@user.com", "Asier Preview", null)
    val sampleUser2 = UsuarioDto("uid2","otro@user.com", "Otro User", null)
    val sampleRecipes = listOf(
        RecetaDto(1L,"Lentejas de la Abuela con Chorizo y Verduras Frescas de la Huerta","Un plato clásico lleno de sabor y tradición familiar, perfecto para los días fríos.", "Lentejas\nVerduras", "1. Cocer...", 60, 4, sampleUser2, null, "2024-01-01", "2024-01-01", 15, 5),
        RecetaDto(2L,"Tarta de Queso Fácil y Rápida (Sin Horno)","Increíblemente cremosa y deliciosa, ideal para sorprender.", "Queso\nNata\nGalletas", "1. Base\n2. Relleno", 20, 8, sampleUser, "https://via.placeholder.com/600x400.png?text=Tarta+Queso", "2024-01-02", "2024-01-03", 132, 0)
    )
    val sampleLikedIds = setOf(2L)
    val sampleSavedIds = setOf(1L)

    FoodieClubTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Preview Recetas") }, actions = {
                IconButton({}){ Icon(Icons.Filled.Search, contentDescription ="Buscar Preview")}
                // IconButton({}){ Icon(Icons.Filled.AccountCircle, contentDescription ="Mi Perfil Preview")} // Eliminado de TopBar
                IconButton({}){ Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription ="Salir Preview")}
            })},
            floatingActionButton = { FloatingActionButton(onClick = {}) { Icon(Icons.Filled.Add, contentDescription = "Añadir Preview")} }
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

//@Preview(showBackground = true, name = "Recipe List Screen - Success")
//@Composable
//fun RecipeListScreenSuccessPreview() {
//    val sampleUser = UsuarioDto("uid1","test@user.com", "Asier Preview", null)
//    val sampleUser2 = UsuarioDto("uid2","otro@user.com", "Otro User", null)
//    val sampleRecipes = listOf(
//        RecetaDto(1L,"Lentejas de la Abuela con Chorizo y Verduras Frescas de la Huerta","Un plato clásico lleno de sabor y tradición familiar, perfecto para los días fríos.", "Lentejas\nVerduras", "1. Cocer...", 60, 4, sampleUser2, null, "2024-01-01", "2024-01-01", 15, 5),
//        RecetaDto(2L,"Tarta de Queso Fácil y Rápida (Sin Horno)","Increíblemente cremosa y deliciosa, ideal para sorprender.", "Queso\nNata\nGalletas", "1. Base\n2. Relleno", 20, 8, sampleUser, "https://via.placeholder.com/600x400.png?text=Tarta+Queso", "2024-01-02", "2024-01-03", 132, 0)
//    )
//    val sampleLikedIds = setOf(2L) // Para previews, no necesitamos remember si los datos son estáticos
//    val sampleSavedIds = setOf(1L)
//
//    FoodieClubTheme {
//        Scaffold(
//            topBar = { TopAppBar(title = { Text("Preview Recetas") }, actions = {
//                IconButton({}){ Icon(Icons.Filled.Search, contentDescription ="Buscar Preview")}
//                IconButton({}){ Icon(Icons.Filled.AccountCircle, contentDescription ="Mi Perfil Preview")}
//                IconButton({}){ Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription ="Salir Preview")}
//            })},
//            floatingActionButton = { FloatingActionButton(onClick = {}) { Icon(Icons.Filled.Add, contentDescription = "Añadir Preview")} }
//        ) { paddingValues ->
//            Box(modifier = Modifier.padding(paddingValues)) {
//                RecipeList(
//                    recipes = sampleRecipes,
//                    likedRecipeIds = sampleLikedIds,
//                    savedRecipeIds = sampleSavedIds,
//                    onItemClick = { Log.d("Preview", "Click Receta $it") },
//                    onLikeClick = { Log.d("Preview", "Click Like $it") },
//                    onSaveClick = { Log.d("Preview", "Click Guardar $it") },
//                    onNavigateToProfile = { userId -> Log.d("Preview", "Click Perfil $userId") }
//                )
//            }
//        }
//    }
//}

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