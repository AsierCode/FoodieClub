package com.example.foodieclub.ui.screen // O tu paquete correcto

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Icono para atrás
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Warning
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle // Mejor para coleccionar flows en UI
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.foodieclub.R // Asume R.drawable.ic_launcher_background y ic_broken_image_background
import com.example.foodieclub.data.model.PerfilPublicoDto
import com.example.foodieclub.data.model.RecetaDto
import com.example.foodieclub.ui.viewmodel.ProfileViewModel

// --- ENTRADA PRINCIPAL DEL COMPOSABLE ---
@OptIn(ExperimentalMaterial3Api::class) // Para Scaffold y TopAppBar
@Composable
fun ProfileScreen(
    firebaseUid: String,
    viewModel: ProfileViewModel, // Inyectado o via Factory
    onNavigateToRecipeDetail: (Long) -> Unit, // Navegar al detalle de receta
    onNavigateBack: () -> Unit // <-- NUEVO: Para volver atrás
) {
    // Carga el perfil cuando el Composable entra o el UID cambia
    LaunchedEffect(firebaseUid) {
        viewModel.loadUserProfile(firebaseUid)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Mostrar nombre de usuario en la barra si ya se cargó
                    Text(
                        text = if (uiState.isLoading) "Cargando Perfil..." else uiState.profile?.nombreMostrado ?: "Perfil de Usuario",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { // <-- Botón para volver atrás
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors( // Opcional: Colores personalizados
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues -> // Contenido principal con padding de la TopAppBar

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues), // Aplicar padding de Scaffold
            contentAlignment = Alignment.Center
        ) {
            when {
                // Estado de Carga
                uiState.isLoading -> {
                    CircularProgressIndicator()
                }
                // Estado de Error
                uiState.error != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp) // Padding interno para el error
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Error al cargar el perfil", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                        Text(uiState.error ?: "Error desconocido", textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadUserProfile(firebaseUid) }) {
                            Text("Reintentar")
                        }
                    }
                }
                // Estado de Éxito
                uiState.profile != null -> {
                    ProfileContent(
                        profile = uiState.profile!!, // Sabemos que no es null aquí
                        onRecipeClick = onNavigateToRecipeDetail
                    )
                }
                // Estado inicial (antes de que empiece la carga, opcional)
                // else -> { Text("Iniciando...") }
            }
        }
    } // Fin Scaffold
}

// --- CONTENIDO DEL PERFIL (CUANDO SE CARGA CON ÉXITO) ---
@Composable
fun ProfileContent(
    profile: PerfilPublicoDto,
    onRecipeClick: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp), // Padding horizontal para todo el contenido
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(bottom = 16.dp) // Padding abajo para que el FAB no tape lo último
    ) {
        // --- Cabecera del Perfil ---
        item {
            Spacer(modifier = Modifier.height(16.dp)) // Espacio desde TopAppBar
            ProfileHeader(profile)
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider() // Separador visual (Material 3)
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
                    modifier = Modifier.padding(vertical = 32.dp), // Más espacio si está vacío
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant // Color suave
                )
            }
        } else {
            items(profile.recetasPublicadas, key = { it.id ?: it.hashCode() }) { receta ->
                // Asegurarse de que la receta tenga ID antes de hacerla clicable
                if (receta.id != null) {
                    // *** USA TU RecipeCard AQUÍ SI TIENES UNA ***
                    // SimpleRecipeListItem(receta = receta, onClick = { onRecipeClick(receta.id) })

                    // Usando SimpleRecipeListItem como fallback:
                    SimpleRecipeListItem(
                        receta = receta,
                        onClick = { onRecipeClick(receta.id) } // Llama al callback con el ID
                    )
                    Spacer(modifier = Modifier.height(12.dp)) // Espacio entre tarjetas
                }
            }
        }
    } // Fin LazyColumn
}

// --- CABECERA DEL PERFIL (INFO BÁSICA) ---
@Composable
fun ProfileHeader(profile: PerfilPublicoDto) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(profile.fotoUrl)
                .crossfade(true)
                .placeholder(R.drawable.ic_launcher_background) // Revisa tu placeholder
                .error(R.drawable.ic_broken_image_background) // Revisa tu imagen de error
                .fallback(R.drawable.ic_broken_image_background) // Si data es null
                .build(),
            contentDescription = "Foto de perfil de ${profile.nombreMostrado}",
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape), // Imagen redonda
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = profile.nombreMostrado ?: "Usuario FoodieClub",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Formatear fecha si es necesario (ej. quitar la hora)
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
        // Aquí podrías añadir biografía si la tuvieras en PerfilPublicoDto
        // profile.biografia?.let { bio ->
        //     Spacer(modifier = Modifier.height(8.dp))
        //     Text(bio, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        // }
    }
}

// --- ITEM SIMPLE PARA LA LISTA DE RECETAS EN EL PERFIL ---
// Reemplaza esto con tu RecipeCard si ya tienes uno más completo
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleRecipeListItem(
    receta: RecetaDto,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), // Sombra sutil
        shape = MaterialTheme.shapes.medium // Bordes redondeados
    ) {
        Row(
            modifier = Modifier.padding(12.dp), // Padding interno
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(receta.imagenUrl) // Asume que RecetaDto tiene imagenUrl
                    .crossfade(true)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_broken_image_background)
                    .fallback(R.drawable.ic_launcher_background)
                    .build(),
                contentDescription = "Imagen de ${receta.titulo}",
                modifier = Modifier
                    .size(64.dp) // Un poco más grande para la lista de perfil
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            // Título y quizás descripción corta
            Column(modifier = Modifier.weight(1f)) { // Ocupar espacio restante
                Text(
                    text = receta.titulo ?: "Receta sin título",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Opcional: Mostrar descripción corta si hay espacio
                // Spacer(modifier = Modifier.height(2.dp))
                // Text(
                //     receta.descripcion ?: "",
                //     style = MaterialTheme.typography.bodySmall,
                //     maxLines = 1,
                //     overflow = TextOverflow.Ellipsis,
                //     color = MaterialTheme.colorScheme.onSurfaceVariant
                // )
            }
            // Icono > para indicar que es clicable
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}