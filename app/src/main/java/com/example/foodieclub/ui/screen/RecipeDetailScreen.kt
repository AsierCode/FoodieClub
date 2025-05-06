@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.foodieclub.ui.screen // Revisa tu paquete

import android.util.Log
import android.widget.Toast // Para mensajes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.* // Importar todos los filled para simplicidad
import androidx.compose.material.icons.outlined.BookmarkBorder // Importar Outlined
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.foodieclub.R
import com.example.foodieclub.data.model.ComentarioDto
import com.example.foodieclub.data.model.RecetaDto
import com.example.foodieclub.data.model.UsuarioDto
import com.example.foodieclub.ui.theme.FoodieClubTheme
import com.example.foodieclub.ui.viewmodel.RecipeDetailState
import com.example.foodieclub.ui.viewmodel.RecipeViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: Long,
    recipeViewModel: RecipeViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    // Observar estados relevantes
    val detailState by recipeViewModel.recipeDetailState.collectAsState()
    val likedIds by recipeViewModel.likedRecipeIds.collectAsState()
    val savedIds by recipeViewModel.savedRecipeIds.collectAsState()

    // Cargar datos al componer o si cambia el ID/Token
    LaunchedEffect(recipeId, recipeViewModel.idToken) {
        recipeViewModel.loadRecipeDetail(recipeId)
        if (recipeViewModel.idToken != null) {
            recipeViewModel.loadUserInteractions()
        }
    }

    // Calcular estado de like/save para la receta actual
    val isLiked = likedIds.contains(recipeId)
    val isSaved = savedIds.contains(recipeId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detailState.recipe?.titulo ?: "Cargando...", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
                actions = {
                    // Mostrar acciones solo si la receta está cargada y no hay error
                    if (detailState.recipe != null && detailState.errorMessage == null) {
                        IconButton(onClick = { recipeViewModel.toggleSave(recipeId) }) {
                            Icon( if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, if (isSaved) "Quitar Guardado" else "Guardar",
                                tint = if (isSaved) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                        }
                        IconButton(onClick = { recipeViewModel.toggleLike(recipeId) }) {
                            Icon( if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, if (isLiked) "Quitar Like" else "Dar Like",
                                tint = if (isLiked) Color.Red else LocalContentColor.current)
                        }
                    }
                }
            )
        }
    ) { paddingValues -> // Contenido principal

        // Decidir qué mostrar basado en el estado de carga/error/éxito
        when {
            detailState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(paddingValues), Alignment.Center) { CircularProgressIndicator() }
            }
            detailState.errorMessage != null -> {
                Column(Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), Arrangement.Center, Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.ErrorOutline, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Text("Error al cargar:", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    Text(detailState.errorMessage!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { recipeViewModel.loadRecipeDetail(recipeId) }) { Text("Reintentar") }
                }
            }
            detailState.recipe != null -> {
                // Mostrar contenido principal si hay receta
                RecipeDetailContent(
                    recipe = detailState.recipe!!,
                    comments = detailState.comments,
                    onPostComment = { commentText -> recipeViewModel.postComment(recipeId, commentText) },
                    onDeleteComment = { commentId -> recipeViewModel.deleteComment(recipeId, commentId) },
                    modifier = Modifier.padding(paddingValues) // Pasar padding
                )
            }
            else -> { // Estado inicial o inesperado
                Box(Modifier.fillMaxSize().padding(paddingValues), Alignment.Center) { Text("No se pudo cargar la receta.") }
            }
        }
    }
}

// --- Composable para el Contenido del Detalle ---
@Composable
fun RecipeDetailContent(
    recipe: RecetaDto,
    comments: List<ComentarioDto>,
    onPostComment: (String) -> Unit,
    onDeleteComment: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var newCommentText by remember { mutableStateOf("") }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp) // Espacio entre secciones principales
    ) {
        // --- Sección Detalles Receta ---
        item {
            Column {
                // Imagen
                if (!recipe.imagenUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = recipe.imagenUrl,
                        contentDescription = "Imagen de la receta",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.ic_launcher_foreground)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                // Título (Opcional aquí, ya está en TopAppBar)
                // Text(recipe.titulo ?: "Receta", style = MaterialTheme.typography.headlineMedium)
                // Autor
                Text("Por: ${recipe.usuario?.nombreMostrado ?: recipe.usuario?.email ?: "Anónimo"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                // Descripción
                Text("Descripción", style = MaterialTheme.typography.titleMedium)
                Text(recipe.descripcion ?: "N/A", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                // Tiempo y Raciones
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (recipe.tiempoPreparacion != null) Text("🕒 ${recipe.tiempoPreparacion} min", style = MaterialTheme.typography.bodyMedium)
                    if (recipe.numRaciones != null) Text("👤 ${recipe.numRaciones} raciones", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Ingredientes
                Text("Ingredientes", style = MaterialTheme.typography.titleMedium)
                recipe.ingredientes?.lines()?.filter { it.isNotBlank() }?.forEach {
                    Text("• ${it.trim()}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Pasos
                Text("Pasos", style = MaterialTheme.typography.titleMedium)
                recipe.pasos?.lines()?.filter { it.isNotBlank() }?.forEachIndexed { index, paso ->
                    Text("${index + 1}. ${paso.trim()}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 4.dp))
                }
            }
        }

        // --- Sección Comentarios ---
        item {
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            Text("Comentarios (${comments.size})", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (comments.isEmpty()) {
            item { Text("Sé el primero en comentar.", style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(comments, key = { comment -> comment.id ?: comment.hashCode() }) { comentario ->
                ComentarioItem(
                    comentario = comentario,
                    currentUserId = currentUserId,
                    onDeleteClick = { comentario.id?.let { id -> onDeleteComment(id) } }
                )
                Divider(modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp)) // Separador más sutil
            }
        }

        // --- Campo Añadir Comentario ---
        item {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = newCommentText,
                onValueChange = { newCommentText = it },
                label = { Text("Escribe un comentario...") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(
                        onClick = { if (newCommentText.isNotBlank()) { onPostComment(newCommentText); newCommentText = "" } },
                        enabled = newCommentText.isNotBlank()
                    ) { Icon(Icons.Filled.Send, "Enviar") }
                }
            )
            Spacer(modifier = Modifier.height(16.dp)) // Espacio al final
        }
    } // Fin LazyColumn
}

// --- ComentarioItem ---
@Composable
fun ComentarioItem(comentario: ComentarioDto, currentUserId: String?, onDeleteClick: () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) { // Permitir que el nombre/fecha ocupe espacio
                // TODO: Añadir Foto de perfil (comentario.usuario?.fotoUrl) con Coil AsyncImage(modifier = Modifier.size(24.dp).clip(CircleShape))
                Text(
                    comentario.usuario?.nombreMostrado ?: comentario.usuario?.email ?: "Usuario",
                    style = MaterialTheme.typography.titleSmall, // Un poco más grande
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    comentario.fechaCreacion?.take(10) ?: "-",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Botón borrar (solo si es el autor)
            if (currentUserId != null && comentario.usuario?.firebaseUid == currentUserId) {
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) { // Ajustar tamaño
                    Icon( Icons.Filled.Delete, "Borrar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp)) // Espacio antes del texto
        Text(comentario.texto ?: "", style = MaterialTheme.typography.bodyMedium)
    }
}

// --- Preview ---
@Preview(showBackground = true)
@Composable
fun RecipeDetailScreenPreview() {
    val sampleUser = UsuarioDto("uid1", "preview@user.com", "Usuario Preview", null)
    val sampleRecipe = RecetaDto(1L,"Receta Detalle Preview", "Desc...", "Ingr1\nIngr2", "Paso1\nPaso2", 40, 3, sampleUser, null, "2024-01-01T10:00:00", "2024-01-01T11:00:00", 15, 8)
    val sampleComments = listOf(
        ComentarioDto(10L, "¡Comentario 1!", "2024-01-02", UsuarioDto("uid2", "otro@user.com", "Otro Usuario Largo Nombre", null)),
        ComentarioDto(11L, "Mi propio comentario para ver el botón de borrar.", "2024-01-03", sampleUser) // Comentario del usuario preview
    )
    // Simular estado para la preview
    val previewDetailState = remember { mutableStateOf(RecipeDetailState(isLoading=false, recipe = sampleRecipe, comments = sampleComments))}
    val previewLikedIds = remember { mutableStateOf(setOf(1L)) } // Simular like en esta receta
    val previewSavedIds = remember { mutableStateOf(emptySet<Long>()) }

    FoodieClubTheme {
        // Usamos el estado simulado para la preview
        val detailStateValue = previewDetailState.value
        val isLiked = previewLikedIds.value.contains(detailStateValue.recipe?.id)
        val isSaved = previewSavedIds.value.contains(detailStateValue.recipe?.id)

        Scaffold(
            topBar = { TopAppBar(title = { Text(detailStateValue.recipe?.titulo ?: "Preview")},
                navigationIcon = { IconButton({}){Icon(Icons.AutoMirrored.Filled.ArrowBack,"")} },
                actions = {
                    IconButton({}){ Icon(if(isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,"") }
                    IconButton({}){ Icon(if(isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,"", tint = if(isLiked) Color.Red else LocalContentColor.current) }
                }
            )}
        ) { paddingValues ->
            if(detailStateValue.recipe != null) {
                RecipeDetailContent(
                    recipe = detailStateValue.recipe!!,
                    comments = detailStateValue.comments,
                    onPostComment = { Log.d("Preview", "Post: $it") },
                    onDeleteComment = { Log.d("Preview", "Delete: $it") },
                    modifier = Modifier.padding(paddingValues)
                )
            } else {
                Box(Modifier.fillMaxSize().padding(paddingValues), Alignment.Center){Text("Error en Preview")}
            }
        }
    }
}