package com.example.foodieclub.ui.screen // O tu paquete correcto

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Icono atrás
import androidx.compose.material.icons.filled.ChevronRight // Icono para lista
import androidx.compose.material.icons.filled.Warning // Icono error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.foodieclub.R // Para drawables
import com.example.foodieclub.data.model.PerfilPublicoDto
import com.example.foodieclub.data.model.RecetaDto
// --- IMPORT CORRECTO ---
import com.example.foodieclub.ui.viewmodel.ProfileViewModel
import com.example.foodieclub.ui.viewmodel.PublicProfileState // Importar el estado específico
// -----------------------


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    firebaseUid: String,
    viewModel: ProfileViewModel, // <-- RECIBE ProfileViewModel
    onNavigateToRecipeDetail: (Long) -> Unit,
    onNavigateBack: () -> Unit
) {
    // Observar estado PÚBLICO desde ProfileViewModel
    val uiState by viewModel.publicProfileState.collectAsStateWithLifecycle() // <-- Observa publicProfileState

    LaunchedEffect(firebaseUid) {
        Log.d("ProfileScreenCheck", "LaunchedEffect ejecutado para UID: $firebaseUid. Llamando loadPublicProfile...")
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
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
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

// --- ProfileContent ---
@Composable
fun ProfileContent(
    profile: PerfilPublicoDto, // Recibe el DTO directamente
    onRecipeClick: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            ProfileHeader(profile) // Pasa el DTO
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Recetas publicadas",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (profile.recetasPublicadas.isNullOrEmpty()) {
            item {
                Text(
                    text = "Este usuario todavía no ha publicado ninguna receta.",
                    modifier = Modifier.padding(vertical = 32.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(profile.recetasPublicadas, key = { it.id ?: it.hashCode() }) { receta ->
                if (receta.id != null) {
                    // Usar SimpleRecipeListItem o tu RecipeListItem/Card común
                    SimpleRecipeListItem(
                        recipe = receta,
                        onClick = { onRecipeClick(receta.id) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

// --- ProfileHeader ---
@Composable
fun ProfileHeader(profile: PerfilPublicoDto) { // Recibe el DTO
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(profile.fotoUrl)
                .crossfade(true)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_broken_image_background)
                .fallback(R.drawable.ic_launcher_background)
                .build(),
            contentDescription = "Foto de perfil de ${profile.nombreMostrado}",
            modifier = Modifier.size(100.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = profile.nombreMostrado ?: "Usuario FoodieClub",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        val fechaRegistroMostrada = profile.fechaRegistro?.take(10) ?: "Fecha desconocida"
        Text(
            text = "Miembro desde: $fechaRegistroMostrada",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${profile.numeroRecetas ?: 0} Recetas publicadas",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
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