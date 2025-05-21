// En ui/screen/NewsScreen.kt
package com.example.foodieclub.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu // O un icono más apropiado si es necesario
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodieclub.data.model.Article // Importar modelo Article actualizado con ID
import com.example.foodieclub.ui.screen.ArticleListItem // Importar el item
import com.example.foodieclub.ui.viewmodel.NewsState
import com.example.foodieclub.ui.viewmodel.NewsViewModel
// Importar EmptyStateMessage si está en otro archivo, o definirlo aquí
// import com.example.foodieclub.ui.screen.EmptyStateMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    // Puede recibir el ViewModel o crearlo aquí si está acotado a esta pantalla/NavGraph
    newsViewModel: NewsViewModel = viewModel(),
    onNavigateToArticleDetail: (articleId: String) -> Unit, // <-- Recibe String (ID)
    onNavigateBack: (() -> Unit)? = null // Opcional: para botón atrás si es pantalla secundaria
) {
    val newsState by newsViewModel.newsState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Noticias y Consejos") },
                navigationIcon = {
                    onNavigateBack?.let { // Mostrar solo si se proporciona onNavigateBack
                        IconButton(onClick = it) {
                            // Cambia el icono si es necesario (ej. ArrowBack si viene de otra pantalla)
                            Icon(Icons.Default.Menu, contentDescription = "Volver o Menú")
                        }
                    }
                },
                windowInsets = WindowInsets(top = 2.dp),
                actions = {
                    IconButton(onClick = { newsViewModel.loadNews() }) { // Llamar a loadNews
                        Icon(Icons.Filled.Refresh, contentDescription = "Refrescar Noticias")
                    }
                }
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.Center // Centrar Loading/Error/Empty
        ) {
            when (val state = newsState) {
                is NewsState.Loading -> {
                    CircularProgressIndicator()
                }
                is NewsState.Error -> {
                    Column( // Usar Column para poder añadir un botón de reintentar
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Error: ${state.message}",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { newsViewModel.loadNews() }) {
                            Text("Reintentar")
                        }
                    }
                }
                is NewsState.Success -> {
                    if (state.articles.isEmpty()) {
                        EmptyStateMessage("No hay noticias disponibles en este momento.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(state.articles, key = { it.id.random() }) { article -> // Usar ID como key
                                ArticleListItem(
                                    article = article,
                                    onClick = {
                                        // Asegurarse de que el ID no esté vacío antes de navegar
                                        if (article.id.isNotBlank()) {
                                            onNavigateToArticleDetail(article.id) // <-- Pasar el ID
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}