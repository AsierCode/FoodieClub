package com.example.foodieclub.ui.screen

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodieclub.data.model.PerfilPrivadoDto
import com.example.foodieclub.data.model.RecetaDto
import com.example.foodieclub.data.model.UsuarioDto
import com.example.foodieclub.ui.common.UserProfileHeader
import com.example.foodieclub.ui.theme.FoodieClubTheme
import com.example.foodieclub.ui.viewmodel.MyProfileState
import com.example.foodieclub.ui.viewmodel.ProfileViewModel
import com.example.foodieclub.ui.viewmodel.RecipeViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(
    profileViewModel: ProfileViewModel,
    recipeViewModel: RecipeViewModel,
    onNavigateToRecipeDetail: (Long) -> Unit,
    onSignOutClick: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val myProfileState by profileViewModel.myProfileState.collectAsStateWithLifecycle()
    val currentTokenInProfileVM by rememberUpdatedState(profileViewModel.idToken)
    var showSignOutDialog by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(currentTokenInProfileVM) {
        if (currentTokenInProfileVM != null && myProfileState !is MyProfileState.Success) {
            profileViewModel.loadMyProfile()
        } else if (currentTokenInProfileVM == null && myProfileState !is MyProfileState.Loading) {
            profileViewModel.clearMyProfileState()
        }
    }

    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            Log.d("PullToRefresh", "MyProfileScreen: Refrescando perfil...")
            profileViewModel.loadMyProfile()
        }
    }

    LaunchedEffect(myProfileState) {
        if (myProfileState !is MyProfileState.Loading && pullToRefreshState.isRefreshing) {
            pullToRefreshState.endRefresh()
            Log.d("PullToRefresh", "MyProfileScreen: Refresco del perfil finalizado.")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showSignOutDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar Sesión")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                windowInsets = WindowInsets(top = 0.dp)
            )
        }
    ) { paddingValuesScaffold ->
        Box( // Este Box es el que tendrá el nestedScroll y el PullToRefreshContainer
            modifier = Modifier
                .padding(paddingValuesScaffold)
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
                .fillMaxSize()
        ) {
            // El contenido principal (Header y Tabs/Pager) va en una Column
            Column(modifier = Modifier.fillMaxSize()) {
                when (val state = myProfileState) {
                    is MyProfileState.Loading -> {
                        if (!pullToRefreshState.isRefreshing) { // Mostrar solo si no es por pull-to-refresh
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        // Si está refrescando, el PullToRefreshContainer se encargará del indicador visual
                    }
                    is MyProfileState.Error -> {
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Error al cargar tu perfil: ${state.message}\nInténtalo de nuevo más tarde.",
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = { profileViewModel.loadMyProfile() }) {
                                    Text("Reintentar")
                                }
                            }
                        }
                    }
                    is MyProfileState.Success -> {
                        MyProfileContent(
                            profile = state.profile,
                            recipeViewModel = recipeViewModel,
                            onNavigateToRecipeDetail = onNavigateToRecipeDetail,
                            onNavigateToProfile = onNavigateToProfile
                            // No pasamos pullToRefreshState aquí, se maneja en el Box padre
                        )
                    }
                }
            }

            // Contenedor del indicador de PullToRefresh (se superpone al contenido)
            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                // Personalizar el indicador para que no sea una "bola negra" inicial
                // El contenedor por defecto es una Surface, podemos hacerla transparente
                // cuando no está activa, o usar un color de fondo más sutil.
                // La forma más fácil es que el indicador solo sea visible cuando isRefreshing.
                // El PullToRefreshContainer ya hace esto por defecto con su contenido.
                // Si el problema es el fondo del contenedor antes de tirar, podrías
                // envolver el CircularProgressIndicator en tu propio Composable y controlar su visibilidad
                // o el containerColor del PullToRefreshContainer.
                // Por ahora, confiaremos en la implementación por defecto de M3.
                // Si sigue viéndose mal, podemos personalizar el 'indicator' slot.
                containerColor = if (pullToRefreshState.isRefreshing) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Cierre de Sesión") },
            text = { Text("¿Estás seguro de que quieres cerrar sesión?") },
            confirmButton = {
                TextButton(onClick = { showSignOutDialog = false; onSignOutClick() }) {
                    Text("Cerrar Sesión", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun MyProfileContent(
    profile: PerfilPrivadoDto,
    recipeViewModel: RecipeViewModel,
    onNavigateToRecipeDetail: (Long) -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    // Esta Column organiza el Header y las Tabs. El scroll principal se maneja por el nestedScroll del Box padre.
    Column(modifier = Modifier.fillMaxSize()) {
        UserProfileHeader(
            photoUrl = profile.fotoUrl,
            displayName = profile.nombreMostrado ?: profile.email,
            memberSince = profile.fechaRegistro,
            recipeCount = profile.numeroRecetas,
            likeCount = profile.recetasLikeadas?.size,
            savedCount = profile.recetasGuardadas?.size
        )
        // El Spacer aquí podría ser opcional si MyProfileTabs ya tiene padding superior.
        // Spacer(modifier = Modifier.height(8.dp))
        MyProfileTabs(
            recipeViewModel = recipeViewModel,
            profileData = profile,
            onNavigateToRecipeDetail = onNavigateToRecipeDetail,
            onNavigateToProfile = onNavigateToProfile
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MyProfileTabs(
    recipeViewModel: RecipeViewModel,
    profileData: PerfilPrivadoDto,
    onNavigateToRecipeDetail: (Long) -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val tabs = listOf("Mis Recetas", "Me Gusta", "Guardados")
    val pagerState = rememberPagerState { tabs.size }
    val coroutineScope = rememberCoroutineScope()

    Column(Modifier.fillMaxWidth()) {
        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
//            indicator = { tabPositions ->
//                if (pagerState.currentPage < tabPositions.size) {
//                    TabRowDefaults.PrimaryIndicator(
//                        modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage])
//                    )
//                }
//            },
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // Es importante que el Pager tenga un peso para que sepa cuánto espacio ocupar
            key = { index -> tabs[index] }
        ) { pageIndex ->
            // El contenido de cada página del Pager (las LazyColumn) permitirá el scroll
            // que será interceptado por el nestedScroll del Box padre.
            when (pageIndex) {
                0 -> TabContentMyRecipes(recipes = profileData.recetasPublicadas ?: emptyList(), recipeViewModel = recipeViewModel, onRecipeClick = onNavigateToRecipeDetail, onNavigateToProfile = onNavigateToProfile)
                1 -> TabContentLikedRecipes(recipes = profileData.recetasLikeadas ?: emptyList(), recipeViewModel = recipeViewModel, onRecipeClick = onNavigateToRecipeDetail, onNavigateToProfile = onNavigateToProfile)
                2 -> TabContentSavedRecipes(recipes = profileData.recetasGuardadas ?: emptyList(), recipeViewModel = recipeViewModel, onRecipeClick = onNavigateToRecipeDetail, onNavigateToProfile = onNavigateToProfile)
            }
        }
    }
}

// --- COMPOSABLES DE CONTENIDO DE TABS ---
// (Se mantienen igual, pero el Modifier.nestedScroll se aplica a su contenedor padre)
@Composable
fun TabContentMyRecipes(
    recipes: List<RecetaDto>,
    recipeViewModel: RecipeViewModel,
    onRecipeClick: (Long) -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val likedIds by recipeViewModel.likedRecipeIds.collectAsStateWithLifecycle(lifecycleOwner = lifecycleOwner)
    val savedIds by recipeViewModel.savedRecipeIds.collectAsStateWithLifecycle(lifecycleOwner = lifecycleOwner)

    if (recipes.isEmpty()) {
        EmptyStateMessage("Aún no has publicado ninguna receta.")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize() // Este fillMaxSize es para que la lista ocupe el espacio del Pager
        ) {
            items(recipes, key = { it.id ?: it.hashCode() }) { recipe ->
                if (recipe.id != null) {
                    RecipeListItem(
                        recipe = recipe,
                        isLiked = likedIds.contains(recipe.id),
                        isSaved = savedIds.contains(recipe.id),
                        isFeatured = false,
                        onClick = { onRecipeClick(recipe.id) },
                        onLikeClick = { recipeViewModel.toggleLike(recipe.id) },
                        onSaveClick = { recipeViewModel.toggleSave(recipe.id) },
                        onNavigateToProfile = { /* No op */ }
                    )
                }
            }
        }
    }
}

@Composable
fun TabContentLikedRecipes(
    recipes: List<RecetaDto>,
    recipeViewModel: RecipeViewModel,
    onRecipeClick: (Long) -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val savedIds by recipeViewModel.savedRecipeIds.collectAsStateWithLifecycle(lifecycleOwner = lifecycleOwner)

    if (recipes.isEmpty()) {
        EmptyStateMessage("Aún no te ha gustado ninguna receta.")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(recipes, key = { it.id ?: it.hashCode() }) { recipe ->
                if (recipe.id != null) {
                    RecipeListItem(
                        recipe = recipe,
                        isLiked = true,
                        isSaved = savedIds.contains(recipe.id),
                        isFeatured = false,
                        onClick = { onRecipeClick(recipe.id) },
                        onLikeClick = { recipeViewModel.toggleLike(recipe.id) },
                        onSaveClick = { recipeViewModel.toggleSave(recipe.id) },
                        onNavigateToProfile = {
                            recipe.usuario?.firebaseUid?.let { uid ->
                                onNavigateToProfile(uid)
                            }
                        }
                    )
                } else {
                }
            }
        }
    }
}

@Composable
fun TabContentSavedRecipes(
    recipes: List<RecetaDto>,
    recipeViewModel: RecipeViewModel,
    onRecipeClick: (Long) -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val likedIds by recipeViewModel.likedRecipeIds.collectAsStateWithLifecycle(lifecycleOwner = lifecycleOwner)

    if (recipes.isEmpty()) {
        EmptyStateMessage("No has guardado ninguna receta todavía.")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(recipes, key = { it.id ?: it.hashCode() }) { recipe ->
                if (recipe.id != null) {
                    RecipeListItem(
                        recipe = recipe,
                        isLiked = likedIds.contains(recipe.id),
                        isSaved = true,
                        isFeatured = false,
                        onClick = { onRecipeClick(recipe.id) },
                        onLikeClick = { recipeViewModel.toggleLike(recipe.id) },
                        onSaveClick = { recipeViewModel.toggleSave(recipe.id) },
                        onNavigateToProfile = {
                            recipe.usuario?.firebaseUid?.let { uid ->
                                onNavigateToProfile(uid)
                            }
                        }
                    )
                } else {
                }
            }
        }
    }
}

// Asume que EmptyStateMessage y UserProfileHeader están definidos o importados
