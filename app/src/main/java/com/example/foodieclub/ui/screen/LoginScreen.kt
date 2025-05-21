package com.example.foodieclub.ui.screen

import android.app.Activity.RESULT_OK
import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource // Para logo de Google si lo usas
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodieclub.R // Para logo Google si es drawable
import com.example.foodieclub.ui.theme.FoodieClubTheme
import com.example.foodieclub.ui.viewmodel.AuthViewModel
import com.example.foodieclub.ui.viewmodel.AuthState
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch // Para lanzar coroutine desde launcher

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit, // Callback para navegar a la app principal
    onNavigateToRegister: () -> Unit // Callback para navegar a la pantalla de registro
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    // --- Launcher para Google One Tap ---
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            try {
                // Pasar el resultado al ViewModel para procesar
                authViewModel.handleGoogleSignInResult(result.data)
            } catch (e: ApiException) {
                authViewModel.resetAuthState() // Volver a Idle o mostrar error
                Toast.makeText(context, "Error con Google Sign-In: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                authViewModel.resetAuthState()
                Toast.makeText(context, "Error inesperado con Google Sign-In.", Toast.LENGTH_LONG).show()
            }
        } else {
            authViewModel.resetAuthState() // Resetear estado si el usuario cancela
            // Podrías mostrar un mensaje específico si el usuario canceló
            // if (result.resultCode == Activity.RESULT_CANCELED) { ... }
        }
    }
    // ---------------------------------

    // --- Efecto para lanzar Google One Tap ---
    LaunchedEffect(authState) {
        if (authState is AuthState.OneTapSignInAvailable) {
            try {
                val intentSenderRequest = IntentSenderRequest.Builder((authState as AuthState.OneTapSignInAvailable).intentSender).build()
                googleSignInLauncher.launch(intentSenderRequest)
                authViewModel.resetAuthState() // Resetear estado después de lanzar para no relanzar
            } catch (e: Exception) {
                authViewModel.resetAuthState()
                Toast.makeText(context, "No se pudo iniciar sesión con Google.", Toast.LENGTH_SHORT).show()
            }
        } else if (authState is AuthState.Authenticated) {
            onLoginSuccess() // Navegar a la app principal
        }
    }
    // --------------------------------------

    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()), // Permitir scroll si el teclado ocupa espacio
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp) // Espacio entre elementos
        ) {
            // --- LOGO DE LA APP ---
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground), // <-- REEMPLAZA 'foodieclub_logo' CON EL NOMBRE DE TU ARCHIVO
                contentDescription = "Logo de FoodieClub",
                modifier = Modifier
                    .height(220.dp) // Ajusta el tamaño como necesites
                    .padding(bottom = 8.dp) // Espacio después del logo
                    .width(220.dp)// o .size(120.dp) si es cuadrado
                // contentScale = ContentScale.Fit // O .Inside, .Crop según tu logo
            )

            // --- Campo Email ---
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo Electrónico") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next // Ir al siguiente campo (contraseña)
                ),
                singleLine = true
            )

            // --- Campo Contraseña ---
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done // Acción "Hecho" o "Ir" en teclado
                ),
                keyboardActions = KeyboardActions(onDone = { // Al pulsar "Hecho" en teclado
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    authViewModel.signInWithEmailPassword(email.trim(), password) // Intentar login
                }),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    val description = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = description)
                    }
                },
                singleLine = true
            )

            // --- Mensaje de Error ---
            AnimatedVisibility(visible = authState is AuthState.Error) {
                val errorMsg = (authState as? AuthState.Error)?.message ?: "Error desconocido"
                Text(
                    text = errorMsg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                    textAlign = TextAlign.Center // Centrar texto de error
                )
                // Asegúrate de que no haya ningún TODO() aquí
            } // Fin AnimatedVisibility

            // --- Botón Iniciar Sesión (Email/Pass) ---
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    authViewModel.signInWithEmailPassword(email.trim(), password)
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                enabled = authState !is AuthState.Loading // Deshabilitar mientras carga
            ) {
                if (authState == AuthState.Loading) { // Mostrar spinner si está cargando específicamente este método
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Iniciar Sesión")
                }
            }

            // --- Separador y Botón Google ---
            Text("o", modifier = Modifier.padding(vertical = 8.dp))

            Button(
                onClick = {
                    authViewModel.beginGoogleSignIn()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = authState !is AuthState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                // Puedes añadir el logo de Google aquí si tienes el drawable
                // Icon(painter = painterResource(id = R.drawable.ic_google_logo), contentDescription = null, modifier = Modifier.size(18.dp))
                // Spacer(modifier = Modifier.width(8.dp))
                Text("Iniciar Sesión con Google")
            }

            // --- Enlace a Registro ---
            TextButton(
                onClick = onNavigateToRegister,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("¿No tienes cuenta? Regístrate")
            }
        }

        // Mostrar un overlay de carga general si el estado es Loading (para Google Sign-In, etc.)
        // Esto podría cubrir toda la pantalla si se desea.
        if (authState is AuthState.Loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) // Centrado en el Box padre
        }
    }
}


// --- Preview ---
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    FoodieClubTheme {
        // Para la preview, podemos usar un ViewModel dummy o simplemente
        // simular el estado Idle. El viewModel real se inyectará/obtendrá en el NavHost.
        val dummyViewModel = AuthViewModel(Application()) // Necesita Application, pasar una dummy o usar Hilt preview

        LoginScreen(
            authViewModel = dummyViewModel,
            onLoginSuccess = { Log.d("Preview", "Login Exitoso") },
            onNavigateToRegister = { Log.d("Preview", "Navegar a Registro") }
        )
    }
}