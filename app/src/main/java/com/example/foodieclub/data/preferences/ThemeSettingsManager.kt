package com.example.foodieclub.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.foodieclub.ui.common.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ThemeSettingsManager(private val context: Context) {

    private object PreferencesKeys {
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
    }

    // USA la instancia importada: context.settingsDataStore
    val themePreferenceFlow: Flow<ThemePreference> = context.settingsDataStore.data
        .map { preferences ->
            val themeName = preferences[PreferencesKeys.THEME_PREFERENCE] ?: ThemePreference.SYSTEM.name
            try {
                ThemePreference.valueOf(themeName)
            } catch (e: IllegalArgumentException) {
                ThemePreference.SYSTEM
            }
        }

    suspend fun setThemePreference(themePreference: ThemePreference) {
        // USA la instancia importada: context.settingsDataStore
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_PREFERENCE] = themePreference.name
        }
    }
}