// En data/preferences/AppDataStore.kt
package com.example.foodieclub.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

// Define el DataStore como una extensión de Context a nivel superior AQUÍ UNA SOLA VEZ
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_preferences")