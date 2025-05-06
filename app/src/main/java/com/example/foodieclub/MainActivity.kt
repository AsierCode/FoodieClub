package com.example.foodieclub // Revisa tu paquete

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
// import androidx.compose.ui.tooling.preview.Preview // No preview para MainActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodieclub.ui.screen.AuthNeededScreen
import com.example.foodieclub.ui.screen.CreateRecipeScreen
import com.example.foodieclub.ui.screen.RecipeDetailScreen
import com.example.foodieclub.ui.screen.RecipeListScreen
import com.example.foodieclub.ui.theme.FoodieClubTheme
import com.example.foodieclub.ui.viewmodel.RecipeViewModel
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

enum class CurrentScreen { LOADING, AUTH_NEEDED, AUTH_FLOW, LIST, CREATE, DETAIL }

class MainActivity : ComponentActivity() {

    private val _userState = mutableStateOf<FirebaseUser?>(null)
    private val userState: State<FirebaseUser?> = _userState
    private val _currentScreenState = mutableStateOf(CurrentScreen.LOADING)
    private val currentScreenState: State<CurrentScreen> = _currentScreenState
    private val _selectedRecipeId = mutableStateOf<Long?>(null)

    // Listener para cambios de autenticación
    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        val previousUser = _userState.value // Necesario para lógica de transición
        _userState.value = user // Actualizar el estado del usuario
        Log.d("AuthState", "Listener: Usuario: ${user?.uid}")

        // Cambiar pantalla según el estado de autenticación
        if (user != null) { // Usuario logueado
            if (_currentScreenState.value == CurrentScreen.LOADING || _currentScreenState.value == CurrentScreen.AUTH_NEEDED || _currentScreenState.value == CurrentScreen.AUTH_FLOW) {
                Log.d("Navigation", "Listener: Usuario detectado, cambiando a pantalla LIST")
                _currentScreenState.value = CurrentScreen.LIST
                _selectedRecipeId.value = null
            }
        } else { // Usuario no logueado
            // Solo cambiar a AUTH_NEEDED si no estábamos ya en un estado de no-logueado o carga inicial
            if (_currentScreenState.value != CurrentScreen.AUTH_NEEDED && _currentScreenState.value != CurrentScreen.LOADING) {
                Log.d("Navigation", "Listener: Usuario nulo, cambiando a pantalla AUTH_NEEDED")
                _currentScreenState.value = CurrentScreen.AUTH_NEEDED
                _selectedRecipeId.value = null
            } else if (_currentScreenState.value == CurrentScreen.LOADING) {
                _currentScreenState.value = CurrentScreen.AUTH_NEEDED // Salir de carga si no hay usuario
            }
        }
    }

    // Launcher para el flujo de FirebaseUI Auth (inicializado en setContent)
    private lateinit var signInLauncher: ActivityResultLauncher<android.content.Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Lifecycle", "onCreate")
        setContent {
            signInLauncher = rememberLauncherForActivityResult(
                contract = FirebaseAuthUIActivityResultContract()
            ) { result -> this.onSignInResult(result) }

            val recipeViewModel: RecipeViewModel = viewModel()

            // Efecto para actualizar token y cargar interacciones al cambiar el usuario
            LaunchedEffect(userState.value) {
                val currentUser = userState.value
                Log.d("TokenUpdate", "LaunchedEffect: User state changed -> ${currentUser?.uid}")
                if (currentUser != null) {
                    currentUser.getIdToken(true).addOnCompleteListener { task -> // Forzar refresh con true
                        if (task.isSuccessful) {
                            val newToken = task.result?.token
                            if (newToken != null) {
                                Log.i("TokenUpdate", "Token obtenido, actualizando ViewModel y cargando interacciones.")
                                recipeViewModel.idToken = newToken // Asignar token al ViewModel
                                recipeViewModel.loadUserInteractions() // <-- LLAMAR DESPUÉS DE ASIGNAR TOKEN
                            } else { Log.e("TokenUpdate", "Token recibido es nulo."); recipeViewModel.idToken = null }
                        } else { Log.e("TokenUpdate", "Error obteniendo token", task.exception); recipeViewModel.idToken = null }
                    }
                } else { recipeViewModel.idToken = null; Log.d("TokenUpdate", "Token limpiado.") }
            }

            val currentScreen = currentScreenState.value
            val selectedId = _selectedRecipeId.value

            // Efecto para lanzar login si es necesario
            LaunchedEffect(currentScreen) {
                if (currentScreen == CurrentScreen.AUTH_NEEDED) {
                    _currentScreenState.value = CurrentScreen.AUTH_FLOW // Cambiar estado ANTES de lanzar
                    Log.d("Navigation", "Estado AUTH_NEEDED, lanzando FirebaseUI...")
                    launchSignInFlow(signInLauncher)
                }
            }

            // Composición de la UI principal
            FoodieClubTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Log.d("UI", "Recomponiendo para estado: $currentScreen")
                    when (currentScreen) {
                        CurrentScreen.LOADING, CurrentScreen.AUTH_FLOW -> {
                            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                        }
                        CurrentScreen.AUTH_NEEDED -> {
                            AuthNeededScreen {
                                // Al pulsar reintentar, volvemos a disparar el flujo
                                _currentScreenState.value = CurrentScreen.AUTH_FLOW
                                launchSignInFlow(signInLauncher)
                            }
                        }
                        CurrentScreen.LIST -> {
                            RecipeListScreen(
                                recipeViewModel = recipeViewModel,
                                onSignOutClick = { signOut() },
                                onRecipeClick = { id -> _selectedRecipeId.value = id; _currentScreenState.value = CurrentScreen.DETAIL },
                                onAddRecipeClick = { _currentScreenState.value = CurrentScreen.CREATE }
                            )
                        }
                        CurrentScreen.CREATE -> {
                            CreateRecipeScreen(recipeViewModel) { _currentScreenState.value = CurrentScreen.LIST }
                        }
                        CurrentScreen.DETAIL -> {
                            if (selectedId != null) {
                                RecipeDetailScreen(selectedId, recipeViewModel) { _currentScreenState.value = CurrentScreen.LIST; _selectedRecipeId.value = null }
                            } else { // Estado inválido
                                LaunchedEffect(Unit) { _currentScreenState.value = CurrentScreen.LIST }
                                Box(Modifier.fillMaxSize(), Alignment.Center){ CircularProgressIndicator() }
                            }
                        }
                    }
                }
            }
        }
    } // Fin onCreate

    override fun onStart() {
        super.onStart()
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
        Log.d("Lifecycle", "Auth listener registrado")
        // La lógica inicial se maneja ahora principalmente por el listener
    }

    override fun onStop() {
        super.onStop()
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
        Log.d("Lifecycle", "Auth listener quitado")
    }

    // --- Funciones de Ayuda ---
    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("AuthSuccess", "Login/Registro OK (Listener cambiará a LIST).")
            FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnCompleteListener { task ->
                if(task.isSuccessful) Log.i("AuthSuccess", "Token inmediato OK")
                else Log.e("AuthSuccess", "Error token inmediato", task.exception)
            }
        } else {
            Log.w("AuthResult", "FirebaseUI falló o cancelado. Código: ${result.resultCode}")
            if (response?.error != null) Log.e("AuthError", "Error FirebaseUI: ${response.error?.errorCode}", response.error)
            Toast.makeText(this, "Autenticación fallida o cancelada.", Toast.LENGTH_SHORT).show()
            _currentScreenState.value = CurrentScreen.AUTH_NEEDED // Mostrar pantalla de reintento
        }
    }

    private fun launchSignInFlow(launcher: ActivityResultLauncher<android.content.Intent>) {
        val providers = listOf(AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build())
        val signInIntent = AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(providers).setIsSmartLockEnabled(false).build()
        try { launcher.launch(signInIntent); Log.d("AuthAction", "Intent FirebaseUI lanzado.") }
        catch (e: Exception) { Log.e("AuthAction", "Error lanzando FirebaseUI", e); _currentScreenState.value = CurrentScreen.AUTH_NEEDED }
    }

    private fun signOut() {
        Log.d("AuthAction", "Iniciando cierre de sesión...")
        AuthUI.getInstance().signOut(this).addOnCompleteListener { task ->
            if (task.isSuccessful) Log.d("AuthSignOut", "SignOut OK (Listener cambiará a AUTH_NEEDED).")
            else Log.e("AuthSignOut", "Error en signOut", task.exception)
            // El listener se encarga de cambiar el estado de la pantalla
        }
    }
} // Fin MainActivity