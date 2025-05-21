// En data/preferences/RecipeHistoryManager.kt
package com.example.foodieclub.data.preferences // O tu paquete

// import androidx.datastore.preferences.preferencesDataStore // <-- YA NO NECESITAS ESTE IMPORT AQUÍ
// IMPORTA LA INSTANCIA CENTRALIZADA
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// YA NO DEFINIMOS dataStore AQUÍ

class RecipeHistoryManager(private val context: Context) {

    private object PreferencesKeys {
        val RECIPE_HISTORY_IDS = stringPreferencesKey("recipe_history_ids")
    }

    private val mutex = Mutex()
    private val maxHistorySize = 20

    // USA la instancia importada: context.settingsDataStore
    val recipeHistoryIdsFlow: Flow<List<Long>> = context.settingsDataStore.data
        .map { preferences ->
            val idsString = preferences[PreferencesKeys.RECIPE_HISTORY_IDS] ?: ""
            if (idsString.isBlank()) {
                emptyList()
            } else {
                try {
                    idsString.split(',').mapNotNull { it.toLongOrNull() }
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }

    suspend fun addRecipeToHistory(recipeId: Long) {
        mutex.withLock {
            // USA la instancia importada: context.settingsDataStore
            context.settingsDataStore.edit { preferences ->
                val currentIdsString = preferences[PreferencesKeys.RECIPE_HISTORY_IDS] ?: ""
                val currentIdsList = if (currentIdsString.isBlank()) {
                    mutableListOf()
                } else {
                    try {
                        currentIdsString.split(',').mapNotNull { it.toLongOrNull() }.toMutableList()
                    } catch (e: Exception) {
                        mutableListOf()
                    }
                }
                currentIdsList.remove(recipeId)
                currentIdsList.add(0, recipeId)
                val updatedList = if (currentIdsList.size > maxHistorySize) {
                    currentIdsList.take(maxHistorySize)
                } else {
                    currentIdsList
                }
                preferences[PreferencesKeys.RECIPE_HISTORY_IDS] = updatedList.joinToString(",")
            }
        }
    }

    suspend fun clearHistory() {
        mutex.withLock {
            // USA la instancia importada: context.settingsDataStore
            context.settingsDataStore.edit { preferences ->
                preferences.remove(PreferencesKeys.RECIPE_HISTORY_IDS)
            }
        }
    }
}