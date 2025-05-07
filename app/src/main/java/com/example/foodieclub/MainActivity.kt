package com.example.foodieclub // Revisa tu paquete

import android.app.Activity
import android.content.Intent
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
import androidx.compose.ui.unit.dp
// --- Imports de Navegación ---
import com.example.foodieclub.ui.navigation.MainAppScaffold
// ---------------------------
import com.example.foodieclub.ui.theme.FoodieClubTheme
// --- Imports de ViewModels ---
import com.example.foodieclub.ui.viewmodel.RecipeViewModel
import com.example.foodieclub.ui.viewmodel.ProfileViewModel
import com.example.foodieclub.ui.viewmodel.provideProfileViewModelFactory
// ---------------------------
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import androidx.lifecycle.viewmodel.compose.viewModel // Para obtener ProfileViewModel con factory
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {

    // RecipeViewModel obtenido por defecto usando el delegado by viewModels()
    private val recipeViewModel: RecipeViewModel by viewModels()
    // ProfileViewModel se obtendrá en setContent usando su factory

    // Estado para el usuario de Firebase y el estado de carga inicial
    private var firebaseUser by mutableStateOf<FirebaseUser?>(null)
    private var isLoadingAuthState by mutableStateOf(true)

    // Listener de Autenticación de Firebase
    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val newUser = firebaseAuth.currentUser
        val oldUserUid = firebaseUser?.uid
        Log.d("AuthState", "[Listener] Estado de Auth cambió. Nuevo UID: ${newUser?.uid}, Anterior UID: $oldUserUid")

        if (oldUserUid != newUser?.uid) { // Solo actualizar si el UID realmente cambió
            firebaseUser = newUser
            Log.d("AuthState", "[Listener] firebaseUser (propiedad de Activity) ACTUALIZADO a UID: ${firebaseUser?.uid}")
        }

        if (isLoadingAuthState) { // Solo cambiar si estaba en true
            isLoadingAuthState = false
            Log.d("AuthState", "[Listener] isLoadingAuthState puesto a false.")
        }
    }

    // Launcher para FirebaseUI Auth
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>

    // --- Función para lanzar el flujo de inicio de sesión ---
    private fun launchSignInFlow() {
        if (!::signInLauncher.isInitialized) {
            Log.e("AuthAction", "signInLauncher no inicializado! No se puede lanzar FirebaseUI.")
            Toast.makeText(this, "Error al iniciar sesión (launcher).", Toast.LENGTH_SHORT).show()
            isLoadingAuthState = false // Permitir que la UI muestre algo si esto falla
            return
        }
        Log.d("AuthAction", "Lanzando FirebaseUI desde MainActivity...")
        val providers = listOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setIsSmartLockEnabled(false)
            .build()
        try {
            signInLauncher.launch(signInIntent)
        } catch (e: Exception) {
            Log.e("AuthAction", "Error lanzando FirebaseUI", e)
            Toast.makeText(this, "Error iniciando autenticación.", Toast.LENGTH_SHORT).show()
            isLoadingAuthState = false // Permitir que la UI reaccione si falla el lanzamiento
        }
    }

    // --- Función para cerrar sesión ---
    private fun signOut() {
        Log.d("AuthAction", "Cerrando sesión desde MainActivity...")
        recipeViewModel.clearUserSpecificData()
        // El ProfileViewModel limpiará su estado cuando su idToken se establezca a null
        AuthUI.getInstance().signOut(this).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("AuthSignOut", "SignOut de FirebaseUI OK.")
            } else {
                Log.e("AuthSignOut", "Error en signOut de FirebaseUI", task.exception)
                Toast.makeText(this, "Error al cerrar sesión.", Toast.LENGTH_SHORT).show()
            }
            // El AuthStateListener se encargará de actualizar firebaseUser a null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Lifecycle", "MainActivity onCreate")

        // Intentar obtener el usuario actual al inicio. El listener lo confirmará/actualizará.
        firebaseUser = FirebaseAuth.getInstance().currentUser
        Log.d("Lifecycle", "MainActivity onCreate - firebaseUser inicial: ${firebaseUser?.uid}")

        setContent {
            signInLauncher = rememberLauncherForActivityResult(
                contract = FirebaseAuthUIActivityResultContract()
            ) { result -> this.onSignInResult(result) }

            // Obtener ProfileViewModel usando la factory.
            // Esta instancia será la que se use para "Mi Perfil".
            val profileViewModelFactory = provideProfileViewModelFactory()
            val myProfileViewModel: ProfileViewModel = viewModel( // Renombrar para claridad
                key = "main_activity_my_profile_vm", // Key opcional para esta instancia específica
                factory = profileViewModelFactory
            )
            Log.d("ViewModelInstance", "[MainActivity] ProfileViewModel (para Mi Perfil) CREADO/OBTENIDO: ${myProfileViewModel.hashCode()}")


            // Efecto para actualizar tokens en ambos ViewModels cuando firebaseUser cambie
            LaunchedEffect(firebaseUser) {
                val currentUser = firebaseUser
                Log.d("SUPER_DEBUG_TOKEN", "[MainActivity LE] TRIGGERED. firebaseUser (capturado): ${currentUser?.uid}")

                if (currentUser != null) {
                    Log.d("SUPER_DEBUG_TOKEN", "[MainActivity LE] currentUser NO es null (${currentUser.uid}), intentando obtener token...")
                    try {
                        val tokenResult = currentUser.getIdToken(true).await() // Forzar refresh
                        val token = tokenResult.token
                        if (token != null) {
                            Log.i("SUPER_DEBUG_TOKEN", "[MainActivity LE] Token OBTENIDO con ÉXITO para ${currentUser.uid}. Token (primeros 20): ${token.take(20)}...")
                            Log.d("SUPER_DEBUG_TOKEN", "[MainActivity LE] Asignando token a recipeViewModel.idToken...")
                            recipeViewModel.idToken = token
                            Log.d("SUPER_DEBUG_TOKEN", "[MainActivity LE] recipeViewModel.idToken ASIGNADO.")
                            Log.d("SUPER_DEBUG_TOKEN", "[MainActivity LE] Asignando token a myProfileViewModel (${myProfileViewModel.hashCode()}).idToken...")
                            myProfileViewModel.idToken = token // Usar la instancia myProfileViewModel
                            Log.d("SUPER_DEBUG_TOKEN", "[MainActivity LE] myProfileViewModel.idToken ASIGNADO.")
                        } else {
                            Log.w("SUPER_DEBUG_TOKEN", "[MainActivity LE] getIdToken(true) devolvió un token NULO para ${currentUser.uid}.")
                            recipeViewModel.idToken = null
                            myProfileViewModel.idToken = null
                        }
                    } catch (e: Exception) {
                        Log.e("SUPER_DEBUG_TOKEN", "[MainActivity LE] EXCEPCIÓN obteniendo token para ${currentUser.uid}", e)
                        recipeViewModel.idToken = null
                        myProfileViewModel.idToken = null
                    }
                } else {
                    Log.d("SUPER_DEBUG_TOKEN", "[MainActivity LE] currentUser ES NULL. Limpiando tokens en VMs.")
                    recipeViewModel.idToken = null
                    myProfileViewModel.idToken = null
                }
            }

            FoodieClubTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val currentFirebaseUser = firebaseUser
                    val currentIsLoadingAuthState = isLoadingAuthState
                    Log.d("UIState", "[MainActivity Recompose] isLoading: $currentIsLoadingAuthState, user: ${currentFirebaseUser?.uid}")

                    when {
                        currentIsLoadingAuthState -> {
                            Log.d("UIState", "Mostrando: Carga Inicial Auth (Spinner)")
                            Box(Modifier.fillMaxSize(), Alignment.Center) {
                                CircularProgressIndicator()
                                Text("Verificando sesión...", modifier = Modifier.padding(top = 60.dp))
                            }
                        }
                        currentFirebaseUser != null -> {
                            Log.d("UIState", "Mostrando: MainAppScaffold (Usuario: ${currentFirebaseUser.uid})")
                            // --- PASAR LAS INSTANCIAS CORRECTAS A MainAppScaffold ---
                            MainAppScaffold(
                                recipeViewModel = recipeViewModel,         // Instancia de RecipeVM de la Activity
                                profileViewModel = myProfileViewModel,    // Instancia de ProfileVM (para Mi Perfil) de la Activity
                                onSignOut = ::signOut
                            )
                            // ---------------------------------------------------------
                        }
                        else -> { // firebaseUser es null y isLoadingAuthState es false
                            Log.d("UIState", "Mostrando: Flujo de Login (Usuario No Logueado)")
                            var hasAttemptedSignIn by remember { mutableStateOf(false) }
                            if (!hasAttemptedSignIn) {
                                LaunchedEffect(Unit) {
                                    Log.d("UIState", "firebaseUser null & !isLoadingAuthState, llamando a launchSignInFlow() (intento único).")
                                    launchSignInFlow()
                                    hasAttemptedSignIn = true
                                }
                            }
                            Box(Modifier.fillMaxSize(), Alignment.Center) {
                                CircularProgressIndicator()
                                Text("Iniciando sesión...", modifier = Modifier.padding(top = 60.dp))
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
        Log.d("Lifecycle", "MainActivity onStart: Auth listener registrado.")
    }
    override fun onStop() {
        super.onStop()
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
        Log.d("Lifecycle", "MainActivity onStop: Auth listener quitado")
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        isLoadingAuthState = false
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("AuthResult", "Login/Registro OK. AuthStateListener actualizará firebaseUser.")
        } else {
            Log.w("AuthResult", "FirebaseUI falló o cancelado. Código: ${result.resultCode}")
            if (response == null) { Toast.makeText(this, "Inicio de sesión cancelado.", Toast.LENGTH_SHORT).show() }
            else if (response.error != null) { Log.e("AuthResult", "Error FirebaseUI: ${response.error?.errorCode} - ${response.error?.localizedMessage}", response.error); Toast.makeText(this, "Error de autenticación: ${response.error?.localizedMessage}", Toast.LENGTH_LONG).show() }
        }
    }
}