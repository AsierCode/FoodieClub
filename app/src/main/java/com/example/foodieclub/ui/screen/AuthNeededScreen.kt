package com.example.foodieclub.ui.screen // Asegúrate que el paquete es correcto

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.foodieclub.ui.theme.FoodieClubTheme // Importa tu tema

/**
 * Pantalla que se muestra cuando el usuario necesita iniciar sesión
 * (ej. después de cancelar el flujo de FirebaseUI o al abrir la app sin sesión).
 * Ofrece un botón para volver a intentar el inicio de sesión.
 *
 * @param onLoginClick Lambda que se ejecuta cuando el usuario pulsa el botón
 *                     para iniciar el flujo de autenticación de nuevo.
 */
@Composable
fun AuthNeededScreen(onLoginClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize() // Ocupa toda la pantalla
            .padding(16.dp), // Añade padding
        horizontalAlignment = Alignment.CenterHorizontally, // Centra horizontalmente
        verticalArrangement = Arrangement.Center // Centra verticalmente
    ) {
        // Mensaje indicando la necesidad de login
        Text(
            "Debes iniciar sesión para continuar",
            style = MaterialTheme.typography.headlineSmall, // Estilo de título
            textAlign = androidx.compose.ui.text.style.TextAlign.Center // Centrar texto
        )

        Spacer(modifier = Modifier.height(24.dp)) // Espacio vertical

        // Botón para reintentar el login/registro
        Button(onClick = onLoginClick) {
            Text("Iniciar Sesión / Registrarse")
        }

        // Opcional: Podrías añadir un botón para salir de la aplicación aquí
        // Spacer(modifier = Modifier.height(16.dp))
        // Button(onClick = { /* Lógica para cerrar la app */ }) {
        //     Text("Salir")
        // }
    }
}

// Preview para ver cómo se ve esta pantalla en el editor
@Preview(showBackground = true, name = "AuthNeededScreen Preview")
@Composable
fun AuthNeededScreenPreview() {
    FoodieClubTheme { // Aplica el tema para la preview
        AuthNeededScreen(onLoginClick = {}) // Pasamos una lambda vacía para la preview
    }
}