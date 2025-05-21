@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.example.foodieclub.ui.screen

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.valentinilk.shimmer.shimmer

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun RecipeListScreen(
    recipeViewModel: RecipeViewModel = viewModel(),
    onSignOutClick: () -> Unit, // La acción real de cerrar sesión
    onRecipeClick: (Long) -> Unit,
    onAddRecipeClick: () -> Unit,
    onNavigateToProfile: (String) -> Unit
    // onNavigateToMyProfile ya no se usa aquí
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val listState by recipeViewModel.recipeListState.collectAsStateWithLifecycle(lifecycle = lifecycle)
    val likedIds by recipeViewModel.likedRecipeIds.collectAsStateWithLifecycle(lifecycle = lifecycle)
    val savedIds by recipeViewModel.savedRecipeIds.collectAsStateWithLifecycle(lifecycle = lifecycle)
    val featuredRecipeId by recipeViewModel.featuredRecipeId.collectAsStateWithLifecycle<Long>(
        initialValue = -1L,
        lifecycle = lifecycle
    )

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Estado para el diálogo de confirmación de cierre de sesión
    var showSignOutDialog by remember { mutableStateOf(false) }

    // Estado para PullToRefresh
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(recipeViewModel.idToken, listState) {
        if (recipeViewModel.idToken != null && listState is RecipeListState.Success) {
            recipeViewModel.loadUserInteractions()
        }
    }

    // Efecto para manejar la acción de PullToRefresh
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            recipeViewModel.loadRecipes() // Llama a tu función para cargar/recargar las recetas
        }
    }

    // Efecto para detener la animación de PullToRefresh cuando la carga finaliza
    LaunchedEffect(listState) {
        if (listState !is RecipeListState.Loading && pullToRefreshState.isRefreshing) {
            pullToRefreshState.endRefresh()
        }
    }

    Scaffold(
        topBar = {
            val searchButtonInteractionSource = remember { MutableInteractionSource() }
            val isSearchButtonPressed by searchButtonInteractionSource.collectIsPressedAsState()
            val searchButtonScale = animateFloatAsState(targetValue = if (isSearchButtonPressed) 0.9f else 1f, label = "searchButtonScale")

            val signOutButtonInteractionSource = remember { MutableInteractionSource() }
            val isSignOutButtonPressed by signOutButtonInteractionSource.collectIsPressedAsState()
            val signOutButtonScale = animateFloatAsState(targetValue = if (isSignOutButtonPressed) 0.9f else 1f, label = "signOutButtonScale")

            val closeSearchButtonInteractionSource = remember { MutableInteractionSource() }
            val isCloseSearchButtonPressed by closeSearchButtonInteractionSource.collectIsPressedAsState()
            val closeSearchButtonScale = animateFloatAsState(targetValue = if (isCloseSearchButtonPressed) 0.9f else 1f, label = "closeSearchButtonScale")

            TopAppBar(
                title = {
                    AnimatedVisibility(visible = !isSearchActive, enter = fadeIn(), exit = fadeOut()) {
                        Text("FoodieClub")
                    }
                },
                actions = {
                    if (isSearchActive) {
                        Row(
                            modifier = Modifier.weight(1f).padding(end = 0.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Buscar...", style = MaterialTheme.typography.bodyMedium) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    recipeViewModel.searchRecipes(searchQuery); keyboardController?.hide(); focusManager.clearFocus()
                                }),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    cursorColor = MaterialTheme.colorScheme.primary,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                        }
                        IconButton(
                            onClick = {
                                isSearchActive = false; if (searchQuery.isNotEmpty()) { searchQuery = ""; recipeViewModel.loadRecipes() }
                                keyboardController?.hide(); focusManager.clearFocus()
                            },
                            interactionSource = closeSearchButtonInteractionSource,
                            modifier = Modifier.scale(closeSearchButtonScale.value)
                        ) { Icon(Icons.Filled.Close, contentDescription = "Cerrar búsqueda") }
                    } else {
                        IconButton(
                            onClick = { isSearchActive = true },
                            interactionSource = searchButtonInteractionSource,
                            modifier = Modifier.scale(searchButtonScale.value)
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = "Buscar")
                        }
//                        IconButton(
//                            onClick = { showSignOutDialog = true }, // <-- MOSTRAR DIÁLOGO
//                            interactionSource = signOutButtonInteractionSource,
//                            modifier = Modifier.scale(signOutButtonScale.value)
//                        ) {
//                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar Sesión")
//                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                windowInsets = WindowInsets(top = 0.dp)
            )
        },
        floatingActionButton = {
            val fabInteractionSource = remember { MutableInteractionSource() }
            val isFabPressed by fabInteractionSource.collectIsPressedAsState()
            val fabScale = animateFloatAsState(targetValue = if (isFabPressed) 0.95f else 1f, label = "fabScale")

            FloatingActionButton(
                onClick = onAddRecipeClick,
                interactionSource = fabInteractionSource,
                modifier = Modifier.scale(fabScale.value)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir Receta")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues) // Aplicar padding del Scaffold
                .nestedScroll(pullToRefreshState.nestedScrollConnection) // Conectar para PullToRefresh
                .fillMaxSize()
        ) {
            when (val currentState = listState) {
                is RecipeListState.Loading -> {
                    // Mostrar shimmer solo si no estamos en medio de un pull-to-refresh
                    // El indicador de pull-to-refresh se muestra a través de PullToRefreshContainer
                    if (!pullToRefreshState.isRefreshing) {
                        RecipeListShimmerLoading()
                    }
                }
                is RecipeListState.Success -> {
                    if (currentState.isSearchResult && currentState.recipes.isEmpty()) {
                        EmptyStateMessage("No se encontraron recetas para \"$searchQuery\"")
                    } else if (!currentState.isSearchResult && currentState.recipes.isEmpty()) {
                        EmptyStateMessage("Aún no hay recetas publicadas.\n¡Sé el primero en añadir una!")
                    } else {
                        RecipeList(
                            recipes = currentState.recipes,
                            likedRecipeIds = likedIds,
                            savedRecipeIds = savedIds,
                            featuredRecipeId = featuredRecipeId,
                            onItemClick = onRecipeClick,
                            onLikeClick = { recipeViewModel.toggleLike(it) },
                            onSaveClick = { recipeViewModel.toggleSave(it) },
                            onNavigateToProfile = onNavigateToProfile
                        )
                    }
                }
                is RecipeListState.Error -> {
                    val errorTitle = if (currentState.isSearchError) "Error en la búsqueda" else "Error al cargar recetas"
                    val buttonAction = { if (currentState.isSearchError) recipeViewModel.searchRecipes(searchQuery) else recipeViewModel.loadRecipes() }
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(painter = painterResource(id = R.drawable.ic_broken_image_background), contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(errorTitle, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(currentState.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = buttonAction) { Text("Reintentar") }
                    }
                }
            }

            // Contenedor para el indicador de PullToRefresh
            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    // Diálogo de confirmación para cerrar sesión
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Confirmar Cierre de Sesión") },
            text = { Text("¿Estás seguro de que quieres cerrar sesión?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        onSignOutClick() // Llama a la acción real de cerrar sesión
                    }
                ) {
                    Text("Cerrar Sesión", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun RecipeList(
    recipes: List<RecetaDto>,
    likedRecipeIds: Set<Long>,
    savedRecipeIds: Set<Long>,
    featuredRecipeId: Long,
    onItemClick: (Long) -> Unit,
    onLikeClick: (Long) -> Unit,
    onSaveClick: (Long) -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(recipes, key = { recipe -> recipe.id ?: recipe.hashCode() }) { recipe ->
            if (recipe.id != null) {
                val isLiked = likedRecipeIds.contains(recipe.id)
                val isSaved = savedRecipeIds.contains(recipe.id)
                val isFeatured = recipe.id == featuredRecipeId
                RecipeListItem(
                    recipe = recipe,
                    isLiked = isLiked,
                    isSaved = isSaved,
                    isFeatured = isFeatured, // Pasar flag
                    onClick = { onItemClick(recipe.id) },
                    onLikeClick = { onLikeClick(recipe.id) },
                    onSaveClick = { onSaveClick(recipe.id) },
                    onNavigateToProfile = onNavigateToProfile
                )
            } else {
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
    isFeatured: Boolean, // Recibe flag
    onClick: () -> Unit,
    onLikeClick: () -> Unit,
    onSaveClick: () -> Unit,
    onNavigateToProfile: (String) -> Unit
) {

    val cardInteractionSource = remember { MutableInteractionSource() }
    val isCardPressed by cardInteractionSource.collectIsPressedAsState()
    val cardElevation by animateDpAsState(
        targetValue = if (isCardPressed) 8.dp else 2.dp,
        label = "cardElevation"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        shape = MaterialTheme.shapes.medium,
        interactionSource = cardInteractionSource
    ) {
        Column {
            Box {
                if (!recipe.imagenUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = recipe.imagenUrl,
                        contentDescription = "Imagen de ${recipe.titulo}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.ic_launcher_background),
                        error = painterResource(id = R.drawable.ic_broken_image_background)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RestaurantMenu,
                            contentDescription = "Sin imagen",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                if (isFeatured) { // Mostrar icono si es destacado
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Receta Destacada",
                        tint = Color(0xFFFFC107),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(28.dp)
                            .background(
                                MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
                                RoundedCornerShape(percent = 25)
                            )
                            .padding(3.dp)
                    )
                }
            } // Fin Box imagen

            Column( // Columna texto y botones
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = recipe.titulo ?: "Receta sin título",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                val authorName = recipe.usuario?.nombreMostrado ?: recipe.usuario?.email ?: "Anónimo"
                val authorUid = recipe.usuario?.firebaseUid

                if (authorUid != null) {
                    val authorInteractionSource = remember { MutableInteractionSource() }
                    val isAuthorPressed by authorInteractionSource.collectIsPressedAsState()
                    val authorScale by animateFloatAsState(
                        targetValue = if (isAuthorPressed) 0.95f else 1f,
                        label = "authorNameScale"
                    )
                    Text(
                        text = "Por: $authorName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .scale(authorScale)
                            .clickable(
                                interactionSource = authorInteractionSource,
                                indication = LocalIndication.current
                            ) { onNavigateToProfile(authorUid) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                } else {
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if ((recipe.likesCount ?: 0) > 0 || (recipe.guardadosCount ?: 0) > 0) Arrangement.SpaceBetween else Arrangement.End
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if ((recipe.likesCount ?: 0) > 0) {
                            Icon(Icons.Filled.Favorite, contentDescription = "Icono de favoritos", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "${recipe.likesCount ?: 0}", /*...*/)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                    if ((recipe.likesCount ?: 0) == 0 && (recipe.guardadosCount ?: 0) == 0) {
                        Spacer(Modifier.weight(1f))
                    }
                    Row {
                        val saveInteractionSource = remember { MutableInteractionSource() }
                        val isSavePressed by saveInteractionSource.collectIsPressedAsState()
                        val saveScale by animateFloatAsState(targetValue = if (isSavePressed) 0.9f else 1f, label = "saveButtonScale")
                        IconButton(onClick = onSaveClick, modifier = Modifier.size(36.dp).scale(saveScale), interactionSource = saveInteractionSource) {
Icon(
                                imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (isSaved) "Guardar receta" else "No guardar receta"
                            )                        }

                        val likeInteractionSource = remember { MutableInteractionSource() }
                        val isLikePressed by likeInteractionSource.collectIsPressedAsState()
                        val likeScale by animateFloatAsState(targetValue = if (isLikePressed) 0.9f else 1f, label = "likeButtonScale")
                        IconButton(onClick = onLikeClick, modifier = Modifier.size(36.dp).scale(likeScale), interactionSource = likeInteractionSource) {
Icon(
                                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isLiked) "Quitar de favoritos" else "Añadir a favoritos"
                            )                        }
                    }
                }
            }
        } // Fin Column principal Card
    } // Fin Card
}


// --- Composables de Carga, Vacío y Previews (sin cambios) ---

@Composable
fun RecipeListItemSkeleton() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column {
            Box( // Placeholder para la imagen
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.LightGray.copy(alpha = 0.6f))
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Box( // Placeholder para el título
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(20.dp)
                        .background(Color.LightGray.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box( // Placeholder para el autor
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(14.dp)
                        .background(Color.LightGray.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Box( // Placeholder para icono de guardado
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.LightGray.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box( // Placeholder para icono de like
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.LightGray.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun RecipeListShimmerLoading(itemCount: Int = 6) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().shimmer(), // Aplicar shimmer a toda la lista
        contentPadding = PaddingValues(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(itemCount) {
            RecipeListItemSkeleton()
        }
    }
}


@Composable
fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// --- Previews (sin cambios) ---
@Preview(showBackground = true, name = "Recipe List Screen - Success")
@Composable
fun RecipeListScreenSuccessPreview() {
    val sampleUser = UsuarioDto("uid1", "test@user.com", "Asier Preview", null)
    val sampleUser2 = UsuarioDto("uid2", "otro@user.com", "Otro User", null)
    val sampleRecipes = listOf(
        RecetaDto(1L, "Lentejas de la Abuela con Chorizo y Verduras Frescas de la Huerta", "Un plato clásico lleno de sabor y tradición familiar, perfecto para los días fríos.", "Lentejas\nVerduras", "1. Cocer...", 60, 4, sampleUser2, null, "2024-01-01", "2024-01-01", 15, 5),
        RecetaDto(2L, "Tarta de Queso Fácil y Rápida (Sin Horno)", "Increíblemente cremosa y deliciosa, ideal para sorprender.", "Queso\nNata\nGalletas", "1. Base\n2. Relleno", 20, 8, sampleUser, "https://via.placeholder.com/600x400.png?text=Tarta+Queso", "2024-01-02", "2024-01-03", 132, 0)
    )
    val sampleLikedIds = setOf(2L)
    val sampleSavedIds = setOf(1L)

    FoodieClubTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Preview Recetas") }, actions = {
                IconButton({}){ Icon(Icons.Filled.Search, contentDescription ="Buscar Preview")}
                IconButton({}){ Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription ="Salir Preview")}
            })},
            floatingActionButton = { FloatingActionButton(onClick = {}) { Icon(Icons.Filled.Add, contentDescription = "Añadir Preview")} }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                RecipeList(
                    recipes = sampleRecipes,
                    likedRecipeIds = sampleLikedIds,
                    savedRecipeIds = sampleSavedIds,
                    featuredRecipeId = 1L, // Destacar la primera receta en preview
                    onItemClick = { Log.d("Preview", "Click Receta $it") },
                    onLikeClick = { Log.d("Preview", "Click Like $it") },
                    onSaveClick = { Log.d("Preview", "Click Guardar $it") },
                    onNavigateToProfile = { userId -> Log.d("Preview", "Click Perfil $userId") }
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Recipe List - Loading Shimmer")
@Composable
fun RecipeListLoadingShimmerPreview() {
    FoodieClubTheme {
        RecipeListShimmerLoading(itemCount = 3)
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
            Text("Error al cargar recetas:", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
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