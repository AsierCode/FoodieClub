package com.example.foodieclub.ui.viewmodel // O tu paquete correcto para ViewModels/Factories

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.foodieclub.data.network.ApiService
import com.example.foodieclub.data.network.RetrofitClient // <-- ¡¡IMPORTANTE!! Asegúrate que este import sea correcto y que RetrofitClient provea la instancia

/**
 * Factory para crear instancias de ProfileViewModel.
 * Necesita la instancia de ApiService para pasarla al ViewModel.
 */
class ProfileViewModelFactory(private val apiService: ApiService) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(apiService) as T // Crea ProfileViewModel con ApiService
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

/**
 * Composable auxiliar para obtener fácilmente la ProfileViewModelFactory.
 * Asume que tienes una forma de acceder a tu instancia singleton de ApiService.
 */
@Composable
fun provideProfileViewModelFactory(): ProfileViewModelFactory {
    // ---> ¡¡AJUSTA ESTO!! Cambia RetrofitClient.instance por tu forma real de obtener ApiService <---
    val apiService = remember { RetrofitClient.instance }
    // Usamos remember con apiService como key para recrear la factory si la instancia de apiService cambiara (poco probable si es singleton)
    return remember(apiService) { ProfileViewModelFactory(apiService) }
}