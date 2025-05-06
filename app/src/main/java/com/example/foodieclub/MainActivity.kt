package com.example.foodieclub // Revisa tu paquete

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
// --- Imports de Screens ---
import com.example.foodieclub.ui.screen.AuthNeededScreen
import com.example.foodieclub.ui.screen.CreateRecipeScreen
import com.example.foodieclub.ui.screen.RecipeDetailScreen
import com.example.foodieclub.ui.screen.RecipeListScreen
import com.example.foodieclub.ui.screen.ProfileScreen
import com.example.foodieclub.ui.screen.MyProfileScreen // <-- Import añadido
// --------------------------
import com.example.foodieclub.ui.theme.FoodieClubTheme
// --- Imports de ViewModels y Factory ---
import com.example.foodieclub.ui.viewmodel.RecipeViewModel
import com.example.foodieclub.ui.viewmodel.ProfileViewModel
import com.example.foodieclub.ui.viewmodel.provideProfileViewModelFactory
// --------------------------------------
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import androidx.lifecycle.viewmodel.compose.viewModel

// --- ENUM ACTUALIZADO ---
enum class CurrentScreen { LOADING, AUTH_NEEDED, AUTH_FLOW, LIST, CREATE, DETAIL, PROFILE, MY_PROFILE }
// -----------------------

class MainActivity : ComponentActivity() {

    private val recipeViewModel: RecipeViewModel by viewModels()

    private val _userState = mutableStateOf<FirebaseUser?>(null)
    private val userState: State<FirebaseUser?> = _userState
    private val _currentScreenState = mutableStateOf(CurrentScreen.LOADING)
    private val currentScreenState: State<CurrentScreen> = _currentScreenState
    private val _selectedRecipeId = mutableStateOf<Long?>(null)
    private val _selectedProfileUid = mutableStateOf<String?>(null)

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        val previousUserState = _userState.value
        _userState.value = user
        Log.d("AuthState", "Listener: Usuario: ${user?.uid}")

        if (user != null) {
            if ((previousUserState == null && user != null) ||
                _currentScreenState.value == CurrentScreen.LOADING ||
                _currentScreenState.value == CurrentScreen.AUTH_NEEDED ||
                _currentScreenState.value == CurrentScreen.AUTH_FLOW) {
                Log.d("Navigation", "Listener: Usuario detectado/cambiado, cambiando a pantalla LIST")
                _currentScreenState.value = CurrentScreen.LIST
                _selectedRecipeId.value = null
                _selectedProfileUid.value = null
            }
        } else {
            if (previousUserState != null) {
                Log.d("Navigation", "Listener: Usuario nulo (deslogueo), cambiando a pantalla AUTH_NEEDED")
                _currentScreenState.value = CurrentScreen.AUTH_NEEDED
                _selectedRecipeId.value = null
                _selectedProfileUid.value = null
                try {
                    recipeViewModel.clearUserSpecificData()
                } catch (e: Exception) {
                    Log.e("ViewModelCall", "Error llamando a clearUserSpecificData", e)
                }
            } else if (_currentScreenState.value == CurrentScreen.LOADING) {
                Log.d("Navigation", "Listener: Usuario nulo durante carga inicial, cambiando a AUTH_NEEDED")
                _currentScreenState.value = CurrentScreen.AUTH_NEEDED
            }
        }
    }

    private lateinit var signInLauncher: ActivityResultLauncher<android.content.Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Lifecycle", "onCreate")
        setContent {
            signInLauncher = rememberLauncherForActivityResult(
                contract = FirebaseAuthUIActivityResultContract()
            ) { result -> this.onSignInResult(result) }

            val profileViewModelFactory = provideProfileViewModelFactory()

            LaunchedEffect(userState.value) {
                val currentUser = userState.value
                Log.d("TokenUpdate", "LaunchedEffect: User state changed -> ${currentUser?.uid}")
                if (currentUser != null) {
                    currentUser.getIdToken(true).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val newToken = task.result?.token
                            if (newToken != null) {
                                Log.i("TokenUpdate", "Token obtenido, actualizando ViewModel.")
                                recipeViewModel.idToken = newToken
                            } else { Log.e("TokenUpdate", "Token recibido es nulo."); recipeViewModel.idToken = null }
                        } else { Log.e("TokenUpdate", "Error obteniendo token", task.exception); recipeViewModel.idToken = null }
                    }
                } else { recipeViewModel.idToken = null; Log.d("TokenUpdate", "Token limpiado.") }
            }

            val currentScreen by currentScreenState
            val selectedRecipeId by _selectedRecipeId
            val selectedProfileUid by _selectedProfileUid

            LaunchedEffect(currentScreen) {
                if (currentScreen == CurrentScreen.AUTH_NEEDED) {
                    _currentScreenState.value = CurrentScreen.AUTH_FLOW
                    Log.d("Navigation", "Estado AUTH_NEEDED, lanzando FirebaseUI...")
                    launchSignInFlow(signInLauncher)
                }
            }

            FoodieClubTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Log.d("UI", "Recomponiendo para estado: $currentScreen")

                    when (currentScreen) {
                        CurrentScreen.LOADING, CurrentScreen.AUTH_FLOW -> {
                            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                        }
                        CurrentScreen.AUTH_NEEDED -> {
                            AuthNeededScreen {
                                _currentScreenState.value = CurrentScreen.AUTH_FLOW
                                launchSignInFlow(signInLauncher)
                            }
                        }
                        CurrentScreen.LIST -> {
                            RecipeListScreen(
                                recipeViewModel = recipeViewModel,
                                onSignOutClick = { signOut() },
                                onRecipeClick = { id -> navigateToDetail(id) },
                                onAddRecipeClick = { navigateToCreate() },
                                onNavigateToProfile = { userId -> navigateToProfile(userId) }
                                // Aquí podrías añadir un botón/acción para ir a Mi Perfil
                                // Por ejemplo, en la TopAppBar de RecipeListScreen o una BottomBar
                            )
                        }
                        CurrentScreen.CREATE -> {
                            CreateRecipeScreen(
                                recipeViewModel = recipeViewModel,
                                onNavigateBack = { navigateBackToList() }
                            )
                        }
                        CurrentScreen.DETAIL -> {
                            if (selectedRecipeId != null) {
                                RecipeDetailScreen(
                                    recipeId = selectedRecipeId!!,
                                    recipeViewModel = recipeViewModel,
                                    onNavigateBack = { navigateBackToList() },
                                    onNavigateToProfile = { userId -> navigateToProfile(userId) }
                                )
                            } else {
                                LaunchedEffect(Unit) { navigateBackToList() }
                                Box(Modifier.fillMaxSize(), Alignment.Center){ CircularProgressIndicator() }
                            }
                        }
                        CurrentScreen.PROFILE -> {
                            if (selectedProfileUid != null) {
                                val profileViewModel: ProfileViewModel = viewModel(
                                    key = "profile_$selectedProfileUid",
                                    factory = profileViewModelFactory
                                )
                                ProfileScreen(
                                    firebaseUid = selectedProfileUid!!,
                                    viewModel = profileViewModel,
                                    onNavigateToRecipeDetail = { recipeId -> navigateToDetail(recipeId) },
                                    onNavigateBack = { navigateBackFromProfile() }
                                )
                            } else {
                                Log.e("Navigation", "Se intentó ir a PROFILE sin UID.")
                                LaunchedEffect(Unit) { navigateBackToList() }
                                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                            }
                        }
                        // --- NUEVO CASO PARA MY_PROFILE ---
                        CurrentScreen.MY_PROFILE -> {
                            MyProfileScreen(
                                viewModel = recipeViewModel, // Usa el mismo ViewModel
                                onNavigateToRecipeDetail = { recipeId -> navigateToDetail(recipeId) },
                                onSignOutClick = { signOut() }
                                // Podrías necesitar un onNavigateBack si la TopAppBar no lo maneja
                                // onNavigateBack = { navigateBackToList() }
                            )
                        }
                        // ---------------------------------
                    }
                }
            }
        }
    } // Fin onCreate

    override fun onStart() {
        super.onStart()
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
        Log.d("Lifecycle", "Auth listener registrado")
    }

    override fun onStop() {
        super.onStop()
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
        Log.d("Lifecycle", "Auth listener quitado")
    }

    // --- Funciones de Autenticación ---
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
            if (_currentScreenState.value == CurrentScreen.AUTH_FLOW) {
                _currentScreenState.value = CurrentScreen.AUTH_NEEDED
            }
        }
    }

    private fun launchSignInFlow(launcher: ActivityResultLauncher<android.content.Intent>) {
        val providers = listOf(AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build())
        val signInIntent = AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(providers).setIsSmartLockEnabled(false).build()
        try { launcher.launch(signInIntent); Log.d("AuthAction", "Intent FirebaseUI lanzado.") }
        catch (e: Exception) {
            Log.e("AuthAction", "Error lanzando FirebaseUI", e)
            Toast.makeText(this, "Error iniciando autenticación.", Toast.LENGTH_SHORT).show()
            _currentScreenState.value = CurrentScreen.AUTH_NEEDED
        }
    }

    private fun signOut() {
        Log.d("AuthAction", "Iniciando cierre de sesión...")
        AuthUI.getInstance().signOut(this).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("AuthSignOut", "SignOut OK (Listener cambiará a AUTH_NEEDED).")
            } else {
                Log.e("AuthSignOut", "Error en signOut", task.exception)
                Toast.makeText(this, "Error al cerrar sesión.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- Funciones de Navegación ---
    private fun navigateBackToList() {
        Log.d("Navigation", "Navegando de vuelta a LIST")
        _currentScreenState.value = CurrentScreen.LIST
        _selectedRecipeId.value = null
        _selectedProfileUid.value = null
    }

    private fun navigateToCreate() {
        Log.d("Navigation", "Navegando a CREATE")
        _currentScreenState.value = CurrentScreen.CREATE
    }

    private fun navigateToDetail(recipeId: Long) {
        Log.d("Navigation", "Navegando a DETAIL para receta ID: $recipeId")
        _selectedRecipeId.value = recipeId
        _currentScreenState.value = CurrentScreen.DETAIL
    }

    private fun navigateToProfile(userId: String) {
        Log.d("Navigation", "Navegando a PROFILE para usuario UID: $userId")
        _selectedProfileUid.value = userId
        _currentScreenState.value = CurrentScreen.PROFILE
    }

    // --- FUNCIÓN PARA IR A MI PERFIL ---
    private fun navigateToMyProfile() {
        Log.d("Navigation", "Navegando a MY_PROFILE")
        _currentScreenState.value = CurrentScreen.MY_PROFILE
        _selectedRecipeId.value = null
        _selectedProfileUid.value = null
    }
    // ----------------------------------

    private fun navigateBackFromProfile() {
        // Decide a dónde volver, LIST es lo más simple por ahora
        Log.d("Navigation", "Navegando atrás desde PROFILE")
        _currentScreenState.value = CurrentScreen.LIST
        _selectedProfileUid.value = null
    }

} // Fin MainActivity