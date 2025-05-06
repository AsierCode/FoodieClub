package com.example.foodieclub.ui.screen // O tu paquete correcto

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.foodieclub.R
import com.example.foodieclub.data.model.PerfilPrivadoDto
import com.example.foodieclub.data.model.RecetaDto
import com.example.foodieclub.ui.theme.FoodieClubTheme
import com.example.foodieclub.ui.viewmodel.MyProfileState
import com.example.foodieclub.ui.viewmodel.RecipeViewModel
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import com.example.foodieclub.data.model.UsuarioDto

// --- IMPORTAR EL ITEM DE OTRO SITIO O UNO COMPARTIDO ---
// Asegúrate de importar RecipeListItem (el completo) si no está ya importado
// import com.example.foodieclub.ui.screen.RecipeListItem
// Y también SimpleRecipeListItem si lo vas a usar desde ProfileScreen
// import com.example.foodieclub.ui.screen.SimpleRecipeListItem // O desde donde esté definido


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(
    viewModel: RecipeViewModel,
    onNavigateToRecipeDetail: (Long) -> Unit,
    onSignOutClick: () -> Unit
) {
    val myProfileState by viewModel.myProfileState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel.idToken) {
        if (viewModel.idToken != null) {
            viewModel.loadMyProfile()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil") },
                actions = {
                    IconButton(onClick = onSignOutClick) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar Sesión")
                    }
                    // IconButton(onClick = { /* TODO */ }) {
                    //     Icon(Icons.Filled.Edit, contentDescription = "Editar Perfil")
                    // }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when (val state = myProfileState) {
                is MyProfileState.Loading -> { CircularProgressIndicator() }
                is MyProfileState.Error -> { /* ... (Error UI sin cambios) ... */
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Error al cargar tu perfil", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                        Text(state.message, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadMyProfile() }) {
                            Text("Reintentar")
                        }
                    }
                }
                is MyProfileState.Success -> {
                    MyProfileContent(
                        profile = state.profile,
                        viewModel = viewModel,
                        onNavigateToRecipeDetail = onNavigateToRecipeDetail
                    )
                }
            }
        }
    }
}

@Composable
fun MyProfileContent(
    profile: PerfilPrivadoDto,
    viewModel: RecipeViewModel,
    onNavigateToRecipeDetail: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        MyProfileHeader(profile = profile)
        Spacer(modifier = Modifier.height(16.dp))
        MyProfileTabs(
            viewModel = viewModel,
            profileData = profile,
            onNavigateToRecipeDetail = onNavigateToRecipeDetail
        )
    }
}

@Composable
fun MyProfileHeader(profile: PerfilPrivadoDto) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(profile.fotoUrl)
                .crossfade(true)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_broken_image_background)
                .fallback(R.drawable.ic_launcher_background)
                .build(),
            contentDescription = "Mi foto de perfil",
            modifier = Modifier.size(100.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = profile.nombreMostrado ?: "Nombre no disponible", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(text = profile.email ?: "Email no disponible", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "Miembro desde: ${profile.fechaRegistro?.take(10) ?: "-"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("${profile.numeroRecetas ?: 0} Recetas")
            Text("${profile.recetasLikeadas?.size ?: 0} Likes")
            Text("${profile.recetasGuardadas?.size ?: 0} Guardados")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MyProfileTabs(
    viewModel: RecipeViewModel,
    profileData: PerfilPrivadoDto,
    onNavigateToRecipeDetail: (Long) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Mis Recetas", "Me Gusta", "Guardados")

    Column {
        PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(text = title) }
                )
            }
        }
        key(selectedTabIndex) {
            when (selectedTabIndex) {
                0 -> TabContentMyRecipes(
                    recipes = profileData.recetasPublicadas ?: emptyList(),
                    onRecipeClick = onNavigateToRecipeDetail
                )
                1 -> TabContentLikedRecipes(
                    recipes = profileData.recetasLikeadas ?: emptyList(),
                    viewModel = viewModel,
                    onRecipeClick = onNavigateToRecipeDetail
                )
                2 -> TabContentSavedRecipes(
                    recipes = profileData.recetasGuardadas ?: emptyList(),
                    viewModel = viewModel,
                    onRecipeClick = onNavigateToRecipeDetail
                )
            }
        }
    }
}

@Composable
fun TabContentMyRecipes(
    recipes: List<RecetaDto>,
    onRecipeClick: (Long) -> Unit
) {
    if (recipes.isEmpty()) {
        EmptyStateMessage("Aún no has publicado recetas.")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(recipes, key = { it.id ?: it.hashCode() }) { recipe ->
                if (recipe.id != null) {
                    // --- USA EL ITEM QUE QUIERAS AQUÍ ---
                    // Opción A: Usar el SimpleRecipeListItem (si está importado/accesible desde ProfileScreen o un archivo común)
                    // SimpleRecipeListItem(recipe = recipe, onClick = { onRecipeClick(recipe.id) })

                    // Opción B: Usar el RecipeListItem más completo (si está importado/accesible desde RecipeListScreen o un archivo común)
                    // Nota: isLiked/isSaved no son relevantes aquí, podemos poner false o calcularlos si quieres consistencia visual
                    RecipeListItem(
                        recipe = recipe,
                        isLiked = false, // No relevante en "Mis Recetas"
                        isSaved = false, // No relevante en "Mis Recetas"
                        onClick = { onRecipeClick(recipe.id) },
                        onLikeClick = {}, // No acción aquí
                        onSaveClick = {}, // No acción aquí
                        onNavigateToProfile = {} // No acción aquí
                    )
                }
            }
        }
    }
}

@Composable
fun TabContentLikedRecipes(
    recipes: List<RecetaDto>,
    viewModel: RecipeViewModel,
    onRecipeClick: (Long) -> Unit
) {
    val likedIds by viewModel.likedRecipeIds.collectAsStateWithLifecycle()
    val savedIds by viewModel.savedRecipeIds.collectAsStateWithLifecycle()

    if (recipes.isEmpty()) {
        EmptyStateMessage("Aún no te ha gustado ninguna receta.")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(recipes, key = { it.id ?: it.hashCode() }) { recipe ->
                if (recipe.id != null) {
                    RecipeListItem( // Usar el completo
                        recipe = recipe,
                        isLiked = likedIds.contains(recipe.id),
                        isSaved = savedIds.contains(recipe.id),
                        onClick = { onRecipeClick(recipe.id) },
                        onLikeClick = { viewModel.toggleLike(recipe.id) },
                        onSaveClick = { viewModel.toggleSave(recipe.id) },
                        onNavigateToProfile = { userId -> /* TODO: Navegar al perfil del autor si userId existe */ }
                    )
                }
            }
        }
    }
}

@Composable
fun TabContentSavedRecipes(
    recipes: List<RecetaDto>,
    viewModel: RecipeViewModel,
    onRecipeClick: (Long) -> Unit
) {
    val likedIds by viewModel.likedRecipeIds.collectAsStateWithLifecycle()
    val savedIds by viewModel.savedRecipeIds.collectAsStateWithLifecycle()

    if (recipes.isEmpty()) {
        EmptyStateMessage("No has guardado ninguna receta todavía.")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(recipes, key = { it.id ?: it.hashCode() }) { recipe ->
                if (recipe.id != null) {
                    RecipeListItem( // Usar el completo
                        recipe = recipe,
                        isLiked = likedIds.contains(recipe.id),
                        isSaved = savedIds.contains(recipe.id),
                        onClick = { onRecipeClick(recipe.id) },
                        onLikeClick = { viewModel.toggleLike(recipe.id) },
                        onSaveClick = { viewModel.toggleSave(recipe.id) },
                        onNavigateToProfile = { userId -> /* TODO: Navegar al perfil del autor si userId existe */ }
                    )
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

@Preview(showBackground = true, name="MyProfileScreen Preview")
@Composable
fun MyProfileScreenPreview() {
    val sampleUser = UsuarioDto("uid1", "preview@user.com", "Usuario Preview", null)
    val samplePrivateProfile = PerfilPrivadoDto(firebaseUid = "test_uid_private", nombreMostrado = "Asier Logueado", fotoUrl = null, fechaRegistro = "2023-10-26", numeroRecetas = 1, recetasPublicadas = listOf(RecetaDto(101L, "Mi Tortilla", "Jugosa y deliciosa", "Huevos\nPatatas", "1...", 30, 2, null, null, "2024-01-01", "2024-01-02", 5, 2)), email = "asier@foodieclub.com", recetasLikeadas = listOf(RecetaDto(201L, "Tarta de Queso de Otro", "Increíblemente cremosa!", "Queso\nNata", "1...", 20, 8, sampleUser, "https://via.placeholder.com/600x400.png?text=Tarta+Queso", "2024-01-03", "2024-01-04", 132, 45)), recetasGuardadas = listOf(RecetaDto(201L, "Tarta de Queso de Otro", "Increíblemente cremosa!", "Queso\nNata", "1...", 20, 8, sampleUser, "https://via.placeholder.com/600x400.png?text=Tarta+Queso", "2024-01-03", "2024-01-04", 132, 45)))
    val dummyViewModel = RecipeViewModel()

    FoodieClubTheme {
        MyProfileContent(
            profile = samplePrivateProfile,
            viewModel = dummyViewModel,
            onNavigateToRecipeDetail = {}
        )
    }
}

// --- ELIMINADO SimpleRecipeListItem de aquí ---
// Debes asegurarte de importar RecipeListItem (o SimpleRecipeListItem si prefieres)
// desde donde esté definido (ej: RecipeListScreen.kt o un archivo común)