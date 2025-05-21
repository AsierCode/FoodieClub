package com.example.foodieclub.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieclub.data.model.Article
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


// Estado para la pantalla de detalle del artículo
sealed interface ArticleDetailState {
    object Loading : ArticleDetailState
    data class Success(val article: Article) : ArticleDetailState
    data class Error(val message: String) : ArticleDetailState
}

class ArticleDetailViewModel : ViewModel() {

    private val db: FirebaseFirestore = Firebase.firestore

    private val _articleDetailState = MutableStateFlow<ArticleDetailState>(ArticleDetailState.Loading)
    val articleDetailState: StateFlow<ArticleDetailState> = _articleDetailState.asStateFlow()

    fun loadArticle(articleId: String) {
        if (articleId.isBlank()) {
            _articleDetailState.value = ArticleDetailState.Error("ID de artículo no válido.")
            return
        }

        // Evitar recargar si ya está cargado y es el mismo artículo (opcional)
        val currentState = _articleDetailState.value
        if (currentState is ArticleDetailState.Success && currentState.article.id == articleId) {
            return
        }

        _articleDetailState.value = ArticleDetailState.Loading
        viewModelScope.launch {
            try {
                val documentSnapshot = db.collection("articles")
                    .document(articleId)
                    .get()
                    .await()

                val article = documentSnapshot.toObject<Article>() // Convierte a objeto Article (incluye ID)

                if (article != null) {
                    _articleDetailState.value = ArticleDetailState.Success(article)
                } else {
                    _articleDetailState.value = ArticleDetailState.Error("Artículo no encontrado.")
                }

            } catch (e: Exception) {
                _articleDetailState.value = ArticleDetailState.Error("No se pudo cargar el artículo: ${e.localizedMessage}")
            }
        }
    }
}