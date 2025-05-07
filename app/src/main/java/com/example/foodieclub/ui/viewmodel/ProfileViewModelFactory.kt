package com.example.foodieclub.ui.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.foodieclub.data.network.ApiService
import com.example.foodieclub.data.network.RetrofitClient // Asegúrate que este import sea correcto

/**
 * Factory para crear instancias de ProfileViewModel (que ahora maneja ambos perfiles).
 */
class ProfileViewModelFactory(private val apiService: ApiService) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) { // <- Apunta a ProfileViewModel
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(apiService) as T // <- Crea ProfileViewModel
        }
        // Considerar añadir lógica para crear RecipeViewModel si esta factory fuera genérica
        // if (modelClass.isAssignableFrom(RecipeViewModel::class.java)) { ... }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

/**
 * Composable auxiliar para obtener la ProfileViewModelFactory.
 */
@Composable
fun provideProfileViewModelFactory(): ProfileViewModelFactory {
    // ---> ¡¡AJUSTA ESTO!! Cambia RetrofitClient.instance por tu forma real de obtener ApiService <---
    val apiService = RetrofitClient.instance // O tu forma de obtener ApiService
    return remember { ProfileViewModelFactory(apiService) }
}