package com.example.foodieclub.ui.screen

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.foodieclub.R
import com.example.foodieclub.data.model.PerfilPrivadoDto
import com.example.foodieclub.data.model.RecetaDto
import com.example.foodieclub.data.model.UsuarioDto
import com.example.foodieclub.ui.theme.FoodieClubTheme
import com.example.foodieclub.ui.viewmodel.MyProfileState
import com.example.foodieclub.ui.viewmodel.RecipeViewModel
import com.example.foodieclub.ui.viewmodel.ProfileViewModel
import com.example.foodieclub.ui.screen.RecipeListItem // Asumiendo import
import com.example.foodieclub.ui.screen.EmptyStateMessage // Asumiendo import
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.saveable.rememberSaveable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(
    profileViewModel: ProfileViewModel,
    recipeViewModel: RecipeViewModel,
    onNavigateToRecipeDetail: (Long) -> Unit,
    onSignOutClick: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: ((String) -> Unit)? = null
) {
    val myProfileState by profileViewModel.myProfileState.collectAsStateWithLifecycle()
    val currentTokenInProfileVM by rememberUpdatedState(profileViewModel.idToken) // Para reaccionar a cambios

    // Este LaunchedEffect es un "seguro" o para reintentos desde la UI si el VM no lo hizo.
    // La carga principal se espera que la dispare el ProfileViewModel cuando recibe el token.
    LaunchedEffect(currentTokenInProfileVM, myProfileState) {
        Log.d("MyProfileScreen", "[LaunchedEffect] Eval: Token ProfileVM: ${currentTokenInProfileVM != null}, Estado MyProfile: ${myProfileState::class.java.simpleName}")

        if (currentTokenInProfileVM != null) {
            // Si tenemos token Y el estado es Loading (indica que se necesita cargar/recargar), llamar a loadMyProfile.
            // El ViewModel tiene lógica interna para no recargar si ya está cargando.
            if (myProfileState is MyProfileState.Loading) {
                Log.d("MyProfileScreen", "[LaunchedEffect] Token existe y estado es Loading. Llamando profileViewModel.loadMyProfile().")
                profileViewModel.loadMyProfile()
            }
        } else {
            // Si no hay token, el setter del token en ProfileViewModel ya debería haber puesto el estado en Error.
            // Si por alguna razón el estado aquí es Success y el token es null, es una inconsistencia.
            if (myProfileState is MyProfileState.Success) {
                Log.w("MyProfileScreen", "[LaunchedEffect] Token en ProfileVM es nulo PERO estado era Success. Reseteando via clearMyProfileState.")
                profileViewModel.clearMyProfileState() // Esto pone a Loading, y como token es null, loadMyProfile() pondrá Error.
            } else if (myProfileState !is MyProfileState.Error && myProfileState !is MyProfileState.Loading) {
                // Si no es error ni loading, pero no hay token, forzar un estado de error/loading.
                Log.w("MyProfileScreen", "[LaunchedEffect] Token en ProfileVM es nulo y estado no es Error/Loading. Llamando clearMyProfileState.")
                profileViewModel.clearMyProfileState()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onSignOutClick) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar Sesión")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Log.d("MyProfileScreen", "Recomponiendo MyProfileScreen UI. Estado actual: ${myProfileState::class.java.simpleName}")
            when (val state = myProfileState) {
                is MyProfileState.Loading -> {
                    Log.d("MyProfileScreen", "Mostrando UI: Loading")
                    CircularProgressIndicator()
                }
                is MyProfileState.Error -> {
                    Log.d("MyProfileScreen", "Mostrando UI: Error - ${state.message}")
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Error al cargar tu perfil", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(4.dp))
                        Text(state.message, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = {
                            Log.d("MyProfileScreen", "Botón Reintentar (Error) pulsado.")
                            profileViewModel.loadMyProfile()
                        }) { Text("Reintentar") }
                    }
                }
                is MyProfileState.Success -> {
                    Log.d("MyProfileScreen", "Mostrando UI: Success - Perfil: ${state.profile.email}")
                    MyProfileContent(
                        profile = state.profile,
                        recipeViewModel = recipeViewModel,
                        onNavigateToRecipeDetail = onNavigateToRecipeDetail,
                        onNavigateToProfile = onNavigateToProfile
                    )
                }
            }
        }
    }
}

@Composable
fun MyProfileContent(profile: PerfilPrivadoDto, recipeViewModel: RecipeViewModel, onNavigateToRecipeDetail: (Long) -> Unit, onNavigateToProfile: ((String) -> Unit)?) {
    Column(modifier = Modifier.fillMaxSize()) {
        MyProfileHeader(profile = profile)
        Spacer(modifier = Modifier.height(16.dp))
        MyProfileTabs(recipeViewModel = recipeViewModel, profileData = profile, onNavigateToRecipeDetail = onNavigateToRecipeDetail, onNavigateToProfile = onNavigateToProfile)
    }
}

@Composable
fun MyProfileHeader(profile: PerfilPrivadoDto) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(profile.fotoUrl).crossfade(true).placeholder(R.drawable.ic_launcher_background).error(R.drawable.ic_broken_image_background).fallback(R.drawable.ic_launcher_background).build(), contentDescription = "Mi foto de perfil", modifier = Modifier.size(100.dp).clip(CircleShape), contentScale = ContentScale.Crop)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = profile.nombreMostrado ?: "Nombre no disponible", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(text = profile.email ?: "Email no disponible", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "Miembro desde: ${profile.fechaRegistro ?: "-"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) // Asume que fechaRegistro es String formateado
        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("${profile.numeroRecetas ?: 0} Recetas")
            Text("${profile.recetasLikeadas?.size ?: 0} Me gusta")
            Text("${profile.recetasGuardadas?.size ?: 0} Guardados")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MyProfileTabs(recipeViewModel: RecipeViewModel, profileData: PerfilPrivadoDto, onNavigateToRecipeDetail: (Long) -> Unit, onNavigateToProfile: ((String) -> Unit)?) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Mis Recetas", "Me Gusta", "Guardados")
    Column(Modifier.fillMaxWidth()) {
        PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title -> Tab(selected = selectedTabIndex == index, onClick = { selectedTabIndex = index }, text = { Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis) }) }
        }
        key(selectedTabIndex) {
            Box(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                when (selectedTabIndex) {
                    0 -> TabContentMyRecipes(recipes = profileData.recetasPublicadas ?: emptyList(), recipeViewModel = recipeViewModel, onRecipeClick = onNavigateToRecipeDetail, onNavigateToProfile = onNavigateToProfile)
                    1 -> TabContentLikedRecipes(recipes = profileData.recetasLikeadas ?: emptyList(), recipeViewModel = recipeViewModel, onRecipeClick = onNavigateToRecipeDetail, onNavigateToProfile = onNavigateToProfile)
                    2 -> TabContentSavedRecipes(recipes = profileData.recetasGuardadas ?: emptyList(), recipeViewModel = recipeViewModel, onRecipeClick = onNavigateToRecipeDetail, onNavigateToProfile = onNavigateToProfile)
                }
            }
        }
    }
}

@Composable
fun TabContentMyRecipes(recipes: List<RecetaDto>, recipeViewModel: RecipeViewModel, onRecipeClick: (Long) -> Unit, onNavigateToProfile: ((String) -> Unit)?) {
    val likedIds by recipeViewModel.likedRecipeIds.collectAsStateWithLifecycle()
    val savedIds by recipeViewModel.savedRecipeIds.collectAsStateWithLifecycle()
    if (recipes.isEmpty()) { EmptyStateMessage("Aún no has publicado recetas.") }
    else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
            items(recipes, key = { it.id ?: it.hashCode() }) { recipe ->
                if (recipe.id != null) { RecipeListItem(recipe = recipe, isLiked = likedIds.contains(recipe.id), isSaved = savedIds.contains(recipe.id), onClick = { onRecipeClick(recipe.id) }, onLikeClick = { recipeViewModel.toggleLike(recipe.id) }, onSaveClick = { recipeViewModel.toggleSave(recipe.id) }, onNavigateToProfile = { recipe.usuario?.firebaseUid?.let { uid -> onNavigateToProfile?.invoke(uid) } } )}
            }
        }
    }
}
@Composable
fun TabContentLikedRecipes(recipes: List<RecetaDto>, recipeViewModel: RecipeViewModel, onRecipeClick: (Long) -> Unit, onNavigateToProfile: ((String) -> Unit)?) {
    val savedIds by recipeViewModel.savedRecipeIds.collectAsStateWithLifecycle() // Solo necesitamos savedIds
    if (recipes.isEmpty()) { EmptyStateMessage("Aún no te ha gustado ninguna receta.") }
    else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
            items(recipes, key = { it.id ?: it.hashCode() }) { recipe ->
                if (recipe.id != null) { RecipeListItem(recipe = recipe, isLiked = true, isSaved = savedIds.contains(recipe.id), onClick = { onRecipeClick(recipe.id) }, onLikeClick = { recipeViewModel.toggleLike(recipe.id)}, onSaveClick = { recipeViewModel.toggleSave(recipe.id)}, onNavigateToProfile = { recipe.usuario?.firebaseUid?.let { uid -> onNavigateToProfile?.invoke(uid) } } )}
            }
        }
    }
}
@Composable
fun TabContentSavedRecipes(recipes: List<RecetaDto>, recipeViewModel: RecipeViewModel, onRecipeClick: (Long) -> Unit, onNavigateToProfile: ((String) -> Unit)?) {
    val likedIds by recipeViewModel.likedRecipeIds.collectAsStateWithLifecycle() // Solo necesitamos likedIds
    if (recipes.isEmpty()) { EmptyStateMessage("No has guardado ninguna receta todavía.") }
    else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
            items(recipes, key = { it.id ?: it.hashCode() }) { recipe ->
                if (recipe.id != null) { RecipeListItem(recipe = recipe, isLiked = likedIds.contains(recipe.id), isSaved = true, onClick = { onRecipeClick(recipe.id) }, onLikeClick = { recipeViewModel.toggleLike(recipe.id)}, onSaveClick = { recipeViewModel.toggleSave(recipe.id)}, onNavigateToProfile = { recipe.usuario?.firebaseUid?.let { uid -> onNavigateToProfile?.invoke(uid) } } )}
            }
        }
    }
}

// EmptyStateMessage se asume importado de RecipeListScreen.kt o un archivo común.
// RecipeListItem se asume importado de RecipeListScreen.kt o un archivo común.

@RequiresApi(Build.VERSION_CODES.O) // Para LocalDateTime.now() en Preview
@Preview(showBackground = true, name="MyProfileScreen Preview")
@Composable
fun MyProfileScreenPreview() {
    val sampleUser = UsuarioDto("uid1", "preview@user.com", "Usuario Preview", null)
    val now = LocalDateTime.now()
    val samplePrivateProfile = PerfilPrivadoDto(firebaseUid = "test_uid_private", nombreMostrado = "Asier Logueado", fotoUrl = null, fechaRegistro = now.minusDays(10).format(DateTimeFormatter.ISO_DATE), numeroRecetas = 1, recetasPublicadas = listOf(RecetaDto(101L, "Mi Tortilla", "Jugosa y deliciosa", "Huevos\nPatatas", "1...", 30, 2, null, null, now.minusDays(1).toString(), now.toString(), 5, 2)), email = "asier@foodieclub.com", recetasLikeadas = listOf(RecetaDto(201L, "Tarta de Queso de Otro", "Increíblemente cremosa!", "Queso\nNata", "1...", 20, 8, sampleUser, "https://via.placeholder.com/600x400.png?text=Tarta+Queso", now.minusDays(2).toString(), now.minusHours(5).toString(), 132, 45)), recetasGuardadas = listOf(RecetaDto(201L, "Tarta de Queso de Otro", "Increíblemente cremosa!", "Queso\nNata", "1...", 20, 8, sampleUser, "https://via.placeholder.com/600x400.png?text=Tarta+Queso", now.minusDays(2).toString(), now.minusHours(5).toString(), 132, 45)))
    val dummyRecipeViewModel = RecipeViewModel() // Instancia dummy para la preview
    // Para simular un ProfileViewModel en la preview, necesitarías una instancia dummy
    // o una forma de pasar el estado directamente a MyProfileContent.
    // Por simplicidad, la preview de MyProfileScreen completa es más compleja.
    // La preview de MyProfileContent es más manejable como la tenías.

    FoodieClubTheme {
        // Simular el estado Success para la preview de MyProfileContent
        MyProfileContent(
            profile = samplePrivateProfile,
            recipeViewModel = dummyRecipeViewModel,
            onNavigateToRecipeDetail = {},
            onNavigateToProfile = {}
        )
    }
}