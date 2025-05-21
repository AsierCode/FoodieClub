package com.example.foodieclub.ui.screen // O tu paquete correcto

// --- IMPORT CORRECTO ---
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.foodieclub.R
import com.example.foodieclub.data.model.PerfilPublicoDto
import com.example.foodieclub.data.model.RecetaDto
import com.example.foodieclub.ui.common.UserProfileHeader
import com.example.foodieclub.ui.viewmodel.ProfileViewModel
import com.example.foodieclub.ui.viewmodel.PublicProfileState
import com.example.foodieclub.ui.viewmodel.RecipeViewModel

// -----------------------


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    firebaseUid: String,
    viewModel: ProfileViewModel, // <-- RECIBE ProfileViewModel
    recipeViewModel: RecipeViewModel, // El RecipeViewModel principal
    onNavigateToRecipeDetail: (Long) -> Unit,
    onNavigateBack: () -> Unit
) {
    // Observar estado PÚBLICO desde ProfileViewModel
    val uiState by viewModel.publicProfileState.collectAsStateWithLifecycle() // <-- Observa publicProfileState

    LaunchedEffect(firebaseUid) {
        viewModel.loadPublicProfile(firebaseUid) // Llama a la función en ProfileViewModel
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        // Acceder a los datos dentro del estado Success
                        text = when (val state = uiState) {
                            is PublicProfileState.Loading -> "Cargando Perfil..."
                            is PublicProfileState.Success -> state.profile.nombreMostrado ?: "Perfil de Usuario"
                            is PublicProfileState.Error -> "Error"
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors( // Colores opcionales
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                windowInsets = WindowInsets(top = 2.dp) // <-- CAMBIO APLICADO AQUÍ
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            // Usar el estado directamente (gracias a 'by')
            when (val currentState = uiState) {
                is PublicProfileState.Loading -> { CircularProgressIndicator() }
                is PublicProfileState.Error -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Error al cargar el perfil", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(4.dp))
                        Text(currentState.message, textAlign = TextAlign.Center) // Usar mensaje del estado
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadPublicProfile(firebaseUid) }) { Text("Reintentar") }
                    }
                }
                is PublicProfileState.Success -> {
                    // Pasar el perfil (que ya sabemos que no es null) a ProfileContent
                    ProfileContent(
                        profile = currentState.profile,
                        onRecipeClick = onNavigateToRecipeDetail
                    )
                }
            }
        }
    }
}

//// --- ProfileContent ---
//@Composable
//fun ProfileContent(
//    profile: PerfilPublicoDto, // Recibe el DTO directamente
//    onRecipeClick: (Long) -> Unit
//) {
//    LazyColumn(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(horizontal = 16.dp),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        contentPadding = PaddingValues(bottom = 16.dp)
//    ) {
//        item {
//            Spacer(modifier = Modifier.height(16.dp))
//            //ProfileHeader(profile) // Pasa el DTO
//            Spacer(modifier = Modifier.height(16.dp))
//            HorizontalDivider()
//            Spacer(modifier = Modifier.height(16.dp))
//            Text(
//                text = "Recetas publicadas",
//                style = MaterialTheme.typography.titleLarge,
//                modifier = Modifier.fillMaxWidth(),
//                textAlign = TextAlign.Start
//            )
//            Spacer(modifier = Modifier.height(16.dp))
//        }
//
//        if (profile.recetasPublicadas.isNullOrEmpty()) {
//            item {
//                Text(
//                    text = "Este usuario todavía no ha publicado ninguna receta.",
//                    modifier = Modifier.padding(vertical = 32.dp).fillMaxWidth(),
//                    textAlign = TextAlign.Center,
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//            }
//        } else {
//            items(profile.recetasPublicadas, key = { it.id ?: it.hashCode() }) { receta ->
//                if (receta.id != null) {
//                    // Usar SimpleRecipeListItem o tu RecipeListItem/Card común
//                    SimpleRecipeListItem(
//                        recipe = receta,
//                        onClick = { onRecipeClick(receta.id) }
//                    )
//                    Spacer(modifier = Modifier.height(12.dp))
//                }
//            }
//        }
//    }
//}

// --- ProfileHeader ---
// --- CONTENIDO DEL PERFIL PÚBLICO (CUANDO SE CARGA CON ÉXITO) ---
@Composable
fun ProfileContent(
    profile: PerfilPublicoDto, // Recibe PerfilPublicoDto
    onRecipeClick: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp), // Padding horizontal para todo el contenido
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(bottom = 16.dp) // Padding abajo para que el FAB no tape lo último
    ) {
        // --- Cabecera del Perfil (Usa el Composable común) ---
        item {
            Spacer(modifier = Modifier.height(16.dp)) // Espacio desde TopAppBar
            UserProfileHeader(
                photoUrl = profile.fotoUrl,
                displayName = profile.nombreMostrado,
                memberSince = profile.fechaRegistro, // Asume String formateado en DTO Android
                recipeCount = profile.numeroRecetas
                // No se pasan likeCount ni savedCount para el perfil público
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider() // Separador visual
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Recetas publicadas",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(), // Ocupa ancho
                textAlign = TextAlign.Start // Alinea texto al inicio
            )
            Spacer(modifier = Modifier.height(16.dp)) // Espacio antes de la lista o mensaje
        }

        // --- Lista de Recetas ---
        if (profile.recetasPublicadas.isNullOrEmpty()) {
            item {
                Text(
                    text = "Este usuario todavía no ha publicado ninguna receta.",
                    modifier = Modifier.padding(vertical = 32.dp).fillMaxWidth(), // Más espacio si está vacío y centrado
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant // Color suave
                )
            }
        } else {
            items(profile.recetasPublicadas, key = { it.id ?: it.hashCode() }) { receta ->
                // Asegurarse de que la receta tenga ID antes de hacerla clicable
                if (receta.id != null) {
                    // Usar SimpleRecipeListItem o tu RecipeListItem común
                    SimpleRecipeListItem(
                        recipe = receta,
                        onClick = { onRecipeClick(receta.id) } // Llama al callback con el ID
                    )
                    Spacer(modifier = Modifier.height(12.dp)) // Espacio entre tarjetas
                }
            }
        }
    } // Fin LazyColumn
}

// --- SimpleRecipeListItem (Puede estar aquí o en un archivo común) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleRecipeListItem(
    recipe: RecetaDto,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(recipe.imagenUrl)
                    .crossfade(true)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_broken_image_background)
                    .fallback(R.drawable.ic_launcher_background)
                    .build(),
                contentDescription = "Imagen de ${recipe.titulo}",
                modifier = Modifier.size(64.dp).clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recipe.titulo ?: "Receta sin título",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}