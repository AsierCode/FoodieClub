// En ui/screen/ArticleDetailScreen.kt
package com.example.foodieclub.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.foodieclub.ui.viewmodel.ArticleDetailState
import com.example.foodieclub.ui.viewmodel.ArticleDetailViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    articleId: String, // Recibe el ID como String
    articleDetailViewModel: ArticleDetailViewModel = viewModel(key = "article_detail_$articleId"), // Key para instancia única por ID
    onNavigateBack: () -> Unit
) {
    // Cargar el artículo cuando la pantalla se lanza o el ID cambia
    LaunchedEffect(articleId) {
        articleDetailViewModel.loadArticle(articleId)
    }

    val state by articleDetailViewModel.articleDetailState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState() // Para hacer scroll del contenido

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Mostrar título solo cuando se cargue
                    val titleText = (state as? ArticleDetailState.Success)?.article?.title ?: ""
                    Text(text = titleText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },windowInsets = WindowInsets(top = 2.dp) // <-- CAMBIO APLICADO AQUÍ

            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (val currentState = state) {
                is ArticleDetailState.Loading -> {
                    CircularProgressIndicator()
                }
                is ArticleDetailState.Error -> {
                    Text(
                        text = "Error: ${currentState.message}",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                is ArticleDetailState.Success -> {
                    val article = currentState.article
                    val dateFormatter = remember {
                        SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState) // Hacer la columna scrollable
                            .padding(16.dp)
                    ) {
                        // Título grande
                        Text(
                            text = article.title,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Autor y Fecha
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Por: ${article.author ?: "Anónimo"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = dateFormatter.format(article.getDate()),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        // Imagen (si existe)
                        if (!article.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = article.imageUrl,
                                contentDescription = article.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .padding(bottom = 16.dp),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Contenido del artículo
                        Text(
                            text = article.content,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}