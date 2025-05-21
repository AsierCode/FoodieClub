// En ui/viewmodel/SettingsViewModel.kt
package com.example.foodieclub.ui.viewmodel

import android.app.Application // Necesario para el contexto
import android.util.Log
import androidx.lifecycle.AndroidViewModel // Usamos AndroidViewModel para obtener el contexto de la aplicación
import androidx.lifecycle.viewModelScope
import com.example.foodieclub.data.preferences.ThemeSettingsManager
import com.example.foodieclub.ui.common.ThemePreference
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val themeSettingsManager = ThemeSettingsManager(application) // Pasamos el contexto de la app

    val currentThemePreference: StateFlow<ThemePreference> =
        themeSettingsManager.themePreferenceFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Comparte mientras haya subscriptores
            initialValue = ThemePreference.SYSTEM // Valor inicial mientras carga el real
        )

    fun updateThemePreference(newPreference: ThemePreference) {
        viewModelScope.launch {
            themeSettingsManager.setThemePreference(newPreference)
        }
    }
}