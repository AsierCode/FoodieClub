// En ui/screen/NewScreen.kt
package com.example.foodieclub.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu // O el icono que uses para volver/menú
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodieclub.data.model.Article // Importar modelo
import com.example.foodieclub.ui.viewmodel.NewsState
import com.example.foodieclub.ui.viewmodel.NewsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    // Puede recibir el ViewModel o crearlo aquí si está acotado a esta pantalla
    newsViewModel: NewsViewModel = viewModel(),
    onNavigateToArticleDetail: (Article) -> Unit, // Pasa el objeto Article entero
    onNavigateBack: (() -> Unit)? = null // Opcional: para botón atrás si no es pantalla raíz
) {
    val newsState by newsViewModel.newsState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Noticias y Consejos") },
                navigationIcon = {
                    // Mostrar botón atrás solo si se proporciona la lambda
                    onNavigateBack?.let {
                        IconButton(onClick = it) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Volver o Menú"
                            ) // Cambiar icono si es necesario
                        }
                    }
                }
                // Puedes añadir acciones si es necesario (ej. Refrescar)
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.Center // Centrar Loading/Error
        ) {
            when (val state = newsState) {
                is NewsState.Loading -> {
                    CircularProgressIndicator()
                }

                is NewsState.Error -> {
                    Text(
                        text = "Error: ${state.message}",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
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
                            items(
                                state.articles,
                                key = { it.hashCode() }) { article -> // Usar hashcode como key temporal si no tienes ID estable
                                ArticleListItem(
                                    article = article,
                                    onClick = { onNavigateToArticleDetail(article) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Asume que EmptyStateMessage está disponible/importado