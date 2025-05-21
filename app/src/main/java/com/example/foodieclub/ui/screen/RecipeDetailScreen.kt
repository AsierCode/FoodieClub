package com.example.foodieclub.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.foodieclub.R
import com.example.foodieclub.data.model.ComentarioDto
import com.example.foodieclub.ui.viewmodel.NutritionAnalysisState
import com.example.foodieclub.ui.viewmodel.RecipeDetailViewModel
import com.example.foodieclub.ui.viewmodel.RecipeViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeDetailViewModel: RecipeDetailViewModel,
    mainRecipeViewModel: RecipeViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (userId: String) -> Unit
) {
    val recipeState by recipeDetailViewModel.detailState.collectAsStateWithLifecycle()
    val nutritionState by recipeDetailViewModel.nutritionAnalysisState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val likedRecipeIds by mainRecipeViewModel.likedRecipeIds.collectAsStateWithLifecycle()
    val savedRecipeIds by mainRecipeViewModel.savedRecipeIds.collectAsStateWithLifecycle()

    val currentLoggedInUserUid: String? = remember { mainRecipeViewModel.getCurrentUserUid() }

    var newCommentText by rememberSaveable { mutableStateOf("") }
    var showDeleteCommentDialog by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = recipeState.recipe?.titulo ?: "Detalle de Receta",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    recipeState.recipe?.id?.let { currentRecipeId ->
                        val isLiked = likedRecipeIds.contains(currentRecipeId)
                        val isSaved = savedRecipeIds.contains(currentRecipeId)

                        IconButton(onClick = { recipeDetailViewModel.toggleSaveOnDetail() }) { // Usa el método del ViewModel
                            Icon(
                                imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (isSaved) "Quitar Guardado" else "Guardar"
                            )
                        }
                        IconButton(onClick = { recipeDetailViewModel.toggleLikeOnDetail() }) { // Usa el método del ViewModel
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isLiked) "Quitar Like" else "Dar Like",
                                tint = if (isLiked) Color.Red else LocalContentColor.current
                            )
                        }
                    }
                },
                windowInsets = WindowInsets(top = 0.dp)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.TopStart
        ) {
            if (recipeState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (recipeState.errorMessage != null && recipeState.recipe == null) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Error: ${recipeState.errorMessage}",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (recipeState.recipe != null) {
                val recipe = recipeState.recipe!!
                // val dateFormatter ya no es necesario aquí si se usa en CommentItem

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // ... (Imagen, Título, Autor, Contadores, Info Adicional - como antes) ...
                    if (!recipe.imagenUrl.isNullOrBlank()) {
                        AsyncImage(model = recipe.imagenUrl, contentDescription = recipe.titulo, modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop, placeholder = painterResource(id = R.drawable.ic_launcher_background), error = painterResource(id = R.drawable.ic_broken_image_background) )
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.RestaurantMenu, contentDescription = "Sin imagen", modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(recipe.titulo ?: "Receta sin título", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        recipe.usuario?.let { autor ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).clickable(onClick = { autor.firebaseUid?.let { onNavigateToProfile(it) } })) {
                                Text("Por: ${autor.nombreMostrado ?: autor.email}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Icon(Icons.Filled.Favorite, contentDescription = "Likes", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${recipe.likesCount ?: 0}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (recipe.tiempoPreparacion != null || recipe.numRaciones != null) {
                        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceAround) {
                                recipe.tiempoPreparacion?.let { InfoChip(icon = Icons.Filled.Timer, text = "$it min") }
                                recipe.numRaciones?.let { InfoChip(icon = Icons.Filled.People, text = "$it raciones") }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    RecipeSection(title = "Descripción", content = recipe.descripcion ?: "No hay descripción disponible.")
                    RecipeSection(title = "Ingredientes", content = recipe.ingredientes ?: "No se especificaron ingredientes.")
                    RecipeSection(title = "Preparación", content = recipe.pasos ?: "No se especificaron los pasos.")
                    Spacer(modifier = Modifier.height(24.dp))

                    // Estimación Nutricional
                    Button(
                        onClick = {
                            if (!recipe.ingredientes.isNullOrBlank()) {
                                recipeDetailViewModel.fetchNutritionInfoUsingAI()
                            } else {
                            }
                        },
                        enabled = nutritionState !is NutritionAnalysisState.LoadingParser && nutritionState !is NutritionAnalysisState.LoadingNutritionApi,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (nutritionState is NutritionAnalysisState.LoadingParser || nutritionState is NutritionAnalysisState.LoadingNutritionApi) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                                Spacer(Modifier.width(8.dp))
                                Text("Analizando...")
                            } else {
                                Icon(Icons.Filled.Restaurant, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                Text("Estimar Nutrición (IA)")
                            }
                        }
                    }

                    when (val nutrState = nutritionState) {
                        is NutritionAnalysisState.Idle -> { /* No mostrar nada */ }
                        is NutritionAnalysisState.LoadingParser, is NutritionAnalysisState.LoadingNutritionApi -> { /* Cubierto */ }
                        is NutritionAnalysisState.Success -> {
                            // CORREGIDO: Acceder a nutrState.uiNutritionInfo (que es UINutritionInfo)
                            val uiInfo = nutrState.uiNutritionInfo
                            Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("Estimación Nutricional (IA + Spoonacular):", style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Calorías: ${uiInfo.calories}", style = MaterialTheme.typography.bodyLarge)
                                    Text("Proteínas: ${uiInfo.protein}", style = MaterialTheme.typography.bodyLarge)
                                    Text("Grasas: ${uiInfo.fat}", style = MaterialTheme.typography.bodyLarge)
                                    Text("Carbohidratos: ${uiInfo.carbs}", style = MaterialTheme.typography.bodyLarge)

                                    if (uiInfo.parsedIngredientsBySpoonacular.isNotEmpty()) {
                                        Spacer(Modifier.height(12.dp))
                                        Text("Ingredientes (según Spoonacular):", style = MaterialTheme.typography.titleSmall)
                                        uiInfo.parsedIngredientsBySpoonacular.forEach {
                                            Text("• $it", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    if (uiInfo.notes.isNotEmpty()) {
                                        Spacer(Modifier.height(8.dp))
                                        uiInfo.notes.forEach { note ->
                                            Text(note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                    Text(
                                        "Nota: Estimación generada. Puede no ser 100% precisa.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                        is NutritionAnalysisState.ParserError -> {
                            Text("Error al procesar ingredientes: ${nutrState.message}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
                        }
                        is NutritionAnalysisState.NutritionApiError -> {
                            Text("Error al obtener datos de nutrición: ${nutrState.message}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    // Comentarios
                    Text("Comentarios", style = MaterialTheme.typography.titleLarge)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    val comments = recipeState.comments
                    if (comments.isEmpty()) {
                        Text("Aún no hay comentarios. ¡Sé el primero!", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp, top = 4.dp))
                    } else {
                        comments.forEach { comment ->
                            CommentItem(
                                comment = comment,
                                isOwnComment = comment.usuario?.firebaseUid == currentLoggedInUserUid && currentLoggedInUserUid != null,
                                onDeleteClick = {
                                    comment.id?.let { commentIdToDelete ->
                                        showDeleteCommentDialog = commentIdToDelete
                                    }
                                }
                            )
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        label = { Text("Escribe un comentario...") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (newCommentText.isNotBlank()) {
                                        recipeDetailViewModel.postCommentOnDetail(newCommentText)
                                        newCommentText = ""
                                    }
                                },
                                enabled = newCommentText.isNotBlank()
                            ) {
                                Icon(Icons.Filled.Send, contentDescription = "Enviar comentario")
                            }
                        },
                        isError = recipeState.postCommentError
                    )
                    if (recipeState.postCommentError && !recipeState.errorMessage.isNullOrBlank()) {
                        Text(
                            recipeState.errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 0.dp, bottom = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("No se pudo cargar la información de la receta.")
                }
            }
        }
    }

    showDeleteCommentDialog?.let { commentIdToDelete ->
        AlertDialog(
            onDismissRequest = { showDeleteCommentDialog = null },
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Estás seguro de que quieres eliminar este comentario?") },
            confirmButton = {
                Button(
                    onClick = {
                        recipeDetailViewModel.deleteCommentOnDetail(commentIdToDelete)
                        showDeleteCommentDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                Button(onClick = { showDeleteCommentDialog = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun RecipeSection(title: String, content: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
    HorizontalDivider(modifier = Modifier.padding(bottom = 6.dp))
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = buildAnnotatedString {
            content.lines().forEachIndexed { index, line ->
                if (line.isNotBlank()) {
                    // No es necesario SpanStyle(fontSize=...) si el MaterialTheme.typography.bodyLarge ya es adecuado.
                    // Si quieres un lineHeight específico aquí, aplícalo al Text directamente o usa ParagraphStyle en buildAnnotatedString
                    withStyle(style = SpanStyle(fontSize = 16.sp)) { // Mantenido tu fontSize
                        append(line.trim())
                    }
                }
                if (index < content.lines().size - 1 && line.isNotBlank()) {
                    if (content.lines().getOrNull(index+1)?.isNotBlank() == true) {
                        append("\n\n")
                    } else {
                        append("\n")
                    }
                } else if (line.isNotBlank()) {
                    append("\n")
                }
            }
        },
        style = MaterialTheme.typography.bodyLarge,
        lineHeight = 22.sp // Aplicar lineHeight aquí si es deseado para todo el Text
    )
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
fun InfoChip(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun CommentItem(
    comment: ComentarioDto,
    isOwnComment: Boolean,
    onDeleteClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.forLanguageTag("es-ES")) }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.usuario?.nombreMostrado ?: "Anónimo",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(8.dp))
                val dateText = comment.fechaCreacion?.let { timestamp ->
                    (timestamp as? Timestamp)?.toDate()?.let { dateFormatter.format(it) }
                } ?: ""
                if (dateText.isNotBlank()) {
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            if (isOwnComment) {
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar comentario", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(comment.texto ?: "", style = MaterialTheme.typography.bodyMedium)
    }
}