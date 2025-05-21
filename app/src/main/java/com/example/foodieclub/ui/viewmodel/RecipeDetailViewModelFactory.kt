package com.example.foodieclub.ui.viewmodel // O el paquete de tu factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.foodieclub.data.preferences.RecipeHistoryManager
import com.example.foodieclub.ui.viewmodel.RecipeDetailViewModel // <-- ¡ASEGÚRATE DE ESTE IMPORT!
import com.example.foodieclub.ui.viewmodel.RecipeViewModel // Para mainRecipeViewModel

class RecipeDetailViewModelFactory(
    private val application: Application,
    private val recipeId: Long,
    private val recipeHistoryManager: RecipeHistoryManager,
    private val mainRecipeViewModel: RecipeViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecipeDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecipeDetailViewModel(application, recipeId, recipeHistoryManager, mainRecipeViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}