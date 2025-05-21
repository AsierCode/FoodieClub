package com.example.foodieclub.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodieclub.ui.theme.FoodieClubTheme
import com.example.foodieclub.ui.viewmodel.AuthState
import com.example.foodieclub.ui.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit, // Callback para navegar a la app principal
    onNavigateToLogin: () -> Unit  // Callback para volver a la pantalla de login
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var displayName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    // --- Efecto para Navegar en Éxito ---
    // Similar a LoginScreen, pero podría redirigir a Login para verificar email si implementaras eso
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            // Aquí podrías mostrar un Toast de "Registro exitoso" antes de navegar
            Toast.makeText(context, "¡Registro exitoso!", Toast.LENGTH_SHORT).show()
            onRegisterSuccess()
        }
    }
    // ----------------------------------

    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Crear Cuenta", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(24.dp))

            // --- Campo Nombre Mostrado ---
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Nombre a mostrar*") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                singleLine = true,
                isError = displayName.isBlank() && authState is AuthState.Error // Mostrar error si está vacío y hubo intento
            )

            // --- Campo Email ---
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo Electrónico*") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                singleLine = true,
                isError = email.isBlank() && authState is AuthState.Error
            )

            // --- Campo Contraseña ---
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña* (mín. 6 caracteres)") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(imageVector = image, "") }
                },
                singleLine = true,
                isError = password.length < 6 && authState is AuthState.Error // Error si es corta y hubo intento
            )

            // --- Campo Confirmar Contraseña ---
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirmar Contraseña*") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide(); focusManager.clearFocus()
                    if (password == confirmPassword && password.length >= 6) {
                        authViewModel.signUpWithEmailPassword(displayName.trim(), email.trim(), password)
                    } else {
                        // Mostrar error localmente o dejar que el ViewModel lo maneje si se intenta enviar
                    }
                }),
                trailingIcon = {
                    val image = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) { Icon(imageVector = image, "") }
                },
                singleLine = true,
                // Error si no coincide Y hubo un intento de registro fallido
                isError = (password != confirmPassword || confirmPassword.isBlank()) && authState is AuthState.Error
            )

            // Mostrar error si las contraseñas no coinciden (localmente)
            if (password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword) {
                Text(
                    text = "Las contraseñas no coinciden",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }


            // --- Botón Registrarse ---
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    if (password != confirmPassword) {
                        Toast.makeText(context, "Las contraseñas no coinciden.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (password.length < 6) {
                        Toast.makeText(context, "La contraseña debe tener al menos 6 caracteres.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    // Llamar al ViewModel solo si las validaciones básicas pasan
                    authViewModel.signUpWithEmailPassword(displayName.trim(), email.trim(), password)
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                enabled = authState !is AuthState.Loading
            ) {
                if (authState == AuthState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Registrarse")
                }
            }

            // --- Enlace a Login ---
            TextButton(
                onClick = onNavigateToLogin,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("¿Ya tienes cuenta? Inicia Sesión")
            }
        }

        // Overlay de carga general (opcional si ya lo tienes en el botón)
        if (authState is AuthState.Loading) {
            // Podrías oscurecer el fondo aquí o simplemente mostrar el indicador
            // CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

// --- Preview ---
@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    // Necesitas un Application Context para el AndroidViewModel en Preview.
    // Una forma es crear una Application dummy o usar librerías de preview avanzadas.
    // La forma más simple es comentar el contenido que depende del ViewModel en la preview.
    FoodieClubTheme {
        // Para simplificar la preview, mostramos la estructura sin interacción con ViewModel real
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Crear Cuenta", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(value = "Asier", onValueChange = {}, label = { Text("Nombre a mostrar*") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = "test@test.com", onValueChange = {}, label = { Text("Correo Electrónico*") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = "123456", onValueChange = {}, label = { Text("Contraseña*") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
            OutlinedTextField(value = "123456", onValueChange = {}, label = { Text("Confirmar Contraseña*") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
            Button(onClick = {}, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("Registrarse") }
            TextButton(onClick = {}, modifier = Modifier.padding(top = 16.dp)) { Text("¿Ya tienes cuenta? Inicia Sesión") }
        }
    }
}