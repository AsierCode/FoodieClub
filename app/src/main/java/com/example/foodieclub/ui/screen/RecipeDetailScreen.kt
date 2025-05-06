@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.foodieclub.ui.screen // Revisa tu paquete

import android.util.Log
// import android.widget.Toast // No se usa actualmente
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
// import androidx.compose.ui.text.font.FontWeight
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
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val detailState by recipeViewModel.recipeDetailState.collectAsState()
    val likedIds by recipeViewModel.likedRecipeIds.collectAsState()
    val savedIds by recipeViewModel.savedRecipeIds.collectAsState()

    LaunchedEffect(recipeId, recipeViewModel.idToken) {
        recipeViewModel.loadRecipeDetail(recipeId)
        if (recipeViewModel.idToken != null) {
            recipeViewModel.loadUserInteractions()
        }
    }

    val isLiked = likedIds.contains(recipeId)
    val isSaved = savedIds.contains(recipeId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detailState.recipe?.titulo ?: "Cargando...", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
                actions = {
                    // Solo mostrar acciones si la receta está cargada (no nula) y no hay error
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
    ) { paddingValues ->
        when {
            detailState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(paddingValues), Alignment.Center) { CircularProgressIndicator() }
            }
            detailState.errorMessage != null -> {
                Column(Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), Arrangement.Center, Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.ErrorOutline, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Text("Error al cargar:", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    Text(detailState.errorMessage ?: "Error desconocido", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) // Usamos elvis operator aquí por si acaso
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { recipeViewModel.loadRecipeDetail(recipeId) }) { Text("Reintentar") }
                }
            }
            // --- INICIO CORRECCIÓN TYPE MISMATCH (USANDO let) ---
            detailState.recipe != null -> {
                // Usamos 'let' para obtener una referencia no nula segura (nonNullRecipe)
                detailState.recipe?.let { nonNullRecipe ->
                    RecipeDetailContent(
                        recipe = nonNullRecipe, // <-- Usar la variable del 'let' (tipo RecetaDto)
                        comments = detailState.comments,
                        onPostComment = { commentText -> recipeViewModel.postComment(recipeId, commentText) },
                        onDeleteComment = { commentId -> recipeViewModel.deleteComment(recipeId, commentId) },
                        onNavigateToProfile = onNavigateToProfile,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
            // --- FIN CORRECCIÓN TYPE MISMATCH ---
            else -> {
                // Este caso podría darse si no está cargando, no hay error, pero recipe es null (poco probable si la lógica es correcta)
                Box(Modifier.fillMaxSize().padding(paddingValues), Alignment.Center) { Text("No se pudo cargar la receta.") }
            }
        }
    }
}

// RecipeDetailContent, ComentarioItem y Preview no necesitan cambios respecto a la versión anterior
// (ya asumen que 'recipe' en RecipeDetailContent no es nulo)

@Composable
fun RecipeDetailContent(
    recipe: RecetaDto, // Ya no es nullable aquí
    comments: List<ComentarioDto>,
    onPostComment: (String) -> Unit,
    onDeleteComment: (Long) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var newCommentText by remember { mutableStateOf("") }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Detalles Receta ---
        item {
            Column {
                // Imagen
                if (!recipe.imagenUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = recipe.imagenUrl,
                        contentDescription = "Imagen de la receta",
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.ic_launcher_background),
                        error = painterResource(R.drawable.ic_broken_image_background)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                // Autor (Clicable)
                val authorName = recipe.usuario?.nombreMostrado ?: recipe.usuario?.email ?: "Anónimo"
                val authorUid = recipe.usuario?.firebaseUid
                Text(
                    text = "Por: $authorName",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (authorUid != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clickable(enabled = authorUid != null) {
                            authorUid?.let { uid ->
                                Log.d("Navigation", "Click en autor (detalle): $uid")
                                onNavigateToProfile(uid)
                            }
                        }
                        .padding(bottom = 16.dp)
                )

                // Descripción, Tiempo, Raciones, Ingredientes, Pasos...
                Text("Descripción", style = MaterialTheme.typography.titleMedium)
                Text(recipe.descripcion ?: "N/A", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (recipe.tiempoPreparacion != null) Text("🕒 ${recipe.tiempoPreparacion} min", style = MaterialTheme.typography.bodyMedium)
                    if (recipe.numRaciones != null) Text("👤 ${recipe.numRaciones} raciones", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text("Ingredientes", style = MaterialTheme.typography.titleMedium)
                recipe.ingredientes?.lines()?.filter { it.isNotBlank() }?.forEach {
                    Text("• ${it.trim()}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text("Pasos", style = MaterialTheme.typography.titleMedium)
                recipe.pasos?.lines()?.filter { it.isNotBlank() }?.forEachIndexed { index, paso ->
                    Text("${index + 1}. ${paso.trim()}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 4.dp))
                }
            }
        }

        // --- Comentarios ---
        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
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
                    onDeleteClick = { comentario.id?.let { id -> onDeleteComment(id) } },
                    onNavigateToProfile = onNavigateToProfile
                )
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp), thickness = 0.5.dp)
            }
        }

        // --- Añadir Comentario ---
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
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Enviar")
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ComentarioItem(
    comentario: ComentarioDto,
    currentUserId: String?,
    onDeleteClick: () -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .clickable(enabled = comentario.usuario?.firebaseUid != null) {
                        comentario.usuario?.firebaseUid?.let { uid ->
                            Log.d("Navigation", "Click en autor (comentario): $uid")
                            onNavigateToProfile(uid)
                        }
                    }
            ) {
                // TODO: Foto perfil con AsyncImage(comentario.usuario?.fotoUrl)
                Column {
                    Text(
                        comentario.usuario?.nombreMostrado ?: comentario.usuario?.email ?: "Usuario",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if(comentario.usuario?.firebaseUid != null) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                    Text(
                        comentario.fechaCreacion?.take(10) ?: "-",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (currentUserId != null && comentario.usuario?.firebaseUid == currentUserId) {
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                    Icon( Icons.Filled.Delete, "Borrar", tint = MaterialTheme.colorScheme.error)
                }
            } else {
                Spacer(modifier = Modifier.size(32.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(comentario.texto ?: "", style = MaterialTheme.typography.bodyMedium)
    }
}


// --- Preview (Sin cambios) ---
@Preview(showBackground = true)
@Composable
fun RecipeDetailScreenPreview() {
    val sampleUser = UsuarioDto(firebaseUid = "test_firebase_uid_1", email = "preview@user.com", nombreMostrado = "Usuario Preview", fotoUrl = null)
    val sampleOtherUser = UsuarioDto(firebaseUid = "test_firebase_uid_2", email = "otro@user.com", nombreMostrado = "Otro Usuario Largo Nombre", fotoUrl = null)
    val sampleRecipe = RecetaDto(id = 1L, titulo = "Receta Detalle Preview", descripcion = "Desc...", ingredientes = "Ingr1\nIngr2", pasos = "Paso1\nPaso2", tiempoPreparacion = 40, numRaciones = 3, usuario = sampleUser, imagenUrl = "https://via.placeholder.com/1600x900.png?text=Receta+Preview", fechaCreacion = "2024-01-01T10:00:00", fechaActualizacion = "2024-01-01T11:00:00", likesCount = 15, guardadosCount = 8)
    val sampleComments = listOf(ComentarioDto(id = 10L, texto = "¡Comentario 1!", fechaCreacion = "2024-01-02", usuario = sampleOtherUser), ComentarioDto(id = 11L, texto = "Mi propio comentario para ver el botón de borrar.", fechaCreacion = "2024-01-03", usuario = sampleUser))
    val previewDetailState = remember { mutableStateOf(RecipeDetailState(isLoading=false, recipe = sampleRecipe, comments = sampleComments))}
    val previewLikedIds = remember { mutableStateOf(setOf(1L)) }
    val previewSavedIds = remember { mutableStateOf(emptySet<Long>()) }

    FoodieClubTheme {
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
            // Usamos 'let' aquí también para la preview, por consistencia
            detailStateValue.recipe?.let { nonNullRecipe ->
                RecipeDetailContent(
                    recipe = nonNullRecipe,
                    comments = detailStateValue.comments,
                    onPostComment = { Log.d("Preview", "Post: $it") },
                    onDeleteComment = { Log.d("Preview", "Delete: $it") },
                    onNavigateToProfile = { userId -> Log.d("Preview", "Navigate Profile: $userId") },
                    modifier = Modifier.padding(paddingValues)
                )
            } ?: Box(Modifier.fillMaxSize().padding(paddingValues), Alignment.Center){Text("Error en Preview: Receta nula")} // Manejo si recipe fuera null en preview
        }
    }
}