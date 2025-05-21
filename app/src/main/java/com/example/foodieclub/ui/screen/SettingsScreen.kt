// En SettingsScreen.kt
package com.example.foodieclub.ui.screen

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.* // Importar todos los filled
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler // Para abrir URLs
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel // Para preview o si se obtiene aquí
import coil.imageLoader // Para limpiar caché
import com.example.foodieclub.ui.common.ThemePreference
import com.example.foodieclub.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateUp: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val currentThemePreference by settingsViewModel.currentThemePreference.collectAsState()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    // Obtener la versión de la app
    val appVersionName = try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName
    } catch (e: PackageManager.NameNotFoundException) {
        "N/A"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                windowInsets = WindowInsets(top = 2.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()) // Hacer toda la columna scrollable
            // No añadir padding general aquí si SettingItem ya lo maneja o queremos divisores de ancho completo
        ) {
            // --- Sección Tema ---
            SettingsSectionTitle("Tema de la aplicación")
            Column(Modifier.selectableGroup().padding(horizontal = 16.dp)) { // Padding para los radio buttons
                ThemePreferenceOption(ThemePreference.SYSTEM, currentThemePreference, { settingsViewModel.updateThemePreference(ThemePreference.SYSTEM) }, "Seguir configuración del sistema")
                ThemePreferenceOption(ThemePreference.LIGHT, currentThemePreference, { settingsViewModel.updateThemePreference(ThemePreference.LIGHT) }, "Claro")
                ThemePreferenceOption(ThemePreference.DARK, currentThemePreference, { settingsViewModel.updateThemePreference(ThemePreference.DARK) }, "Oscuro")
            }
            // --- Fin Sección Tema ---

            HorizontalDivider(modifier = Modifier.padding(top = 16.dp))

            // --- Sección Datos ---
            SettingsSectionTitle("Datos y Almacenamiento")
            SettingItem(
                icon = Icons.Filled.History,
                title = "Historial de recetas vistas",
                onClick = onNavigateToHistory,
                showChevron = true // Mostrar flecha para indicar navegación
            )
            SettingItem(
                icon = Icons.Filled.DeleteSweep,
                title = "Borrar caché de imágenes",
                subtitle = "Libera espacio eliminando imágenes descargadas temporalmente.",
                onClick = {
                    try {
                        context.imageLoader.diskCache?.clear() // Limpiar caché de disco
                        context.imageLoader.memoryCache?.clear() // Limpiar caché de memoria
                        Toast.makeText(context, "Caché de imágenes borrada", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error al borrar caché: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            )
            // --- Fin Sección Datos ---

            HorizontalDivider()

            // --- NUEVO: Sección Información ---
            SettingsSectionTitle("Información")
            SettingItem(
            icon = Icons.Filled.Info,
                title = "Versión de la aplicación",
                subtitle = appVersionName, // Mostrar la versión obtenida
                onClick = {} // No hacer nada al clickar, solo informativo
            )
            SettingItem(
                icon = Icons.Filled.Description, // O un icono de GitHub/Portfolio
                title = "Acerca del desarrollador",
                subtitle = "Desarrollado por Asier Nuñez", // Pon tu nombre
                onClick = {
                    // Reemplaza con la URL de tu portfolio o GitHub si tienes
                    // uriHandler.openUri("https://github.com/tu_usuario")
                    Toast.makeText(context, "¡Gracias por usar FoodieClub!", Toast.LENGTH_SHORT).show()
                }
            )
            SettingItem(
                icon = Icons.Filled.Policy, // Icono para políticas o atribuciones
                title = "Créditos y Atribuciones",
                subtitle = "APIs: Gemini (Google AI), Spoonacular. Iconos: Material Icons.", // Ejemplo
                onClick = {
                    // Podrías abrir una pantalla de detalle o un diálogo con más info
                    Toast.makeText(context, "Usando Gemini AI y Spoonacular API", Toast.LENGTH_LONG).show()
                }
            )
            // --- Fin Sección Información ---

            HorizontalDivider()

            // --- NUEVO: Sección Contacto ---
            SettingsSectionTitle("Ayuda")
            SettingItem(
                icon = Icons.Filled.Email,
                title = "Contactar / Enviar Feedback",
                subtitle = "Envíanos tus dudas o sugerencias.",
                onClick = {
                    val recipientEmail = "foodieclubcontact@gmail.com" // Reemplaza con tu email real
                    val subject = "Feedback/Soporte - FoodieClub App v$appVersionName"
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:") // Solo apps de email
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
                        putExtra(Intent.EXTRA_SUBJECT, subject)
                        // Podrías añadir putExtra(Intent.EXTRA_TEXT, "Hola, tengo una sugerencia...")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "No se encontró aplicación de email.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
            // --- Fin Sección Contacto ---
        }
    }
}

// Composable para los títulos de sección
@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall, // Un poco más pequeño que titleLarge
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 8.dp) // Ajustar padding
    )
}


// Composable reutilizable para cada fila de ajuste
@Composable
fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    showChevron: Boolean = false // Para indicar si navega a otra pantalla
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp), // Buen padding vertical
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null, // Decorativo
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


// Composable para las opciones de tema (ya lo tenías)
@Composable
private fun ThemePreferenceOption(
    preference: ThemePreference,
    currentSelection: ThemePreference,
    onSelected: () -> Unit,
    label: String
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .selectable(
                selected = (preference == currentSelection),
                onClick = onSelected,
                role = Role.RadioButton
            )
            .padding(vertical = 12.dp), // Un poco más de padding vertical
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = (preference == currentSelection),
            onClick = null
        )
        Spacer(Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}