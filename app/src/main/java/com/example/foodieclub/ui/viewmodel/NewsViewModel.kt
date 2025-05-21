package com.example.foodieclub.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieclub.data.model.Article // Importa tu modelo Article
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Estado para la pantalla de noticias
sealed interface NewsState {
    object Loading : NewsState
    data class Success(val articles: List<Article>) : NewsState
    data class Error(val message: String) : NewsState
}

class NewsViewModel : ViewModel() {

    private val db: FirebaseFirestore = Firebase.firestore

    private val _newsState = MutableStateFlow<NewsState>(NewsState.Loading)
    val newsState: StateFlow<NewsState> = _newsState.asStateFlow()

    init {
        loadNews()
    }

    fun loadNews() {
        _newsState.value = NewsState.Loading
        viewModelScope.launch {
            try {
                val querySnapshot = db.collection("articles")
                    .orderBy("timestamp", Query.Direction.DESCENDING) // Ordenar por fecha, más nuevos primero
                    // .limit(20) // Opcional: Limitar el número de artículos cargados
                    .get()
                    .await() // Esperar resultado

                // Convertir documentos a objetos Article
                // Nota: Firestore KTX 'toObjects' usa el constructor sin argumentos.
                val articles = querySnapshot.toObjects<Article>()

                _newsState.value = NewsState.Success(articles)

            } catch (e: Exception) {
                _newsState.value = NewsState.Error("No se pudieron cargar las noticias: ${e.localizedMessage}")
            }
        }
    }
}