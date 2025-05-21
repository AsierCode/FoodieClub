package com.example.foodieclub

// import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult // Comentado si onSignInResult no se usa activamente
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.foodieclub.ui.navigation.MainAppScaffold
import com.example.foodieclub.ui.navigation.Screen
import com.example.foodieclub.ui.navigation.authGraph
import com.example.foodieclub.ui.theme.FoodieClubTheme
import com.example.foodieclub.ui.viewmodel.AuthState
import com.example.foodieclub.ui.viewmodel.AuthViewModel
import com.example.foodieclub.ui.viewmodel.ProfileViewModel
import com.example.foodieclub.ui.viewmodel.RecipeViewModel
import com.example.foodieclub.ui.viewmodel.SettingsViewModel
import com.example.foodieclub.ui.viewmodel.provideProfileViewModelFactory
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {

    private val recipeViewModel: RecipeViewModel by viewModels()
    private var firebaseUser by mutableStateOf<FirebaseUser?>(null)
    private var isLoadingAuthState by mutableStateOf(true)

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val newUser = firebaseAuth.currentUser
        val oldUserUid = firebaseUser?.uid
        if (oldUserUid != newUser?.uid) { firebaseUser = newUser }
        if (isLoadingAuthState) { isLoadingAuthState = false; Log.d("AuthState", "[Listener] isLoadingAuthState -> false") }
    }

    private lateinit var oneTapLauncher: ActivityResultLauncher<IntentSenderRequest>

    private fun signOut(authViewModel: AuthViewModel?) {
        recipeViewModel.clearUserSpecificData()
        // profileViewModel.clearProfileData() // Si tienes una función para limpiar datos del perfil, llámala aquí
        authViewModel?.resetAuthState()
        FirebaseAuth.getInstance().signOut()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Mantenlo si usas status bar transparente

        firebaseUser = FirebaseAuth.getInstance().currentUser
        initializeRemoteConfig()

        setContent {
            // --- Inicializar ViewModels ---
            val authViewModel: AuthViewModel = viewModel()
            val profileViewModelFactory = provideProfileViewModelFactory()
            val profileViewModel: ProfileViewModel = viewModel(factory = profileViewModelFactory)
            val settingsViewModel: SettingsViewModel = viewModel() // Obtener SettingsViewModel


            // --- Observar la preferencia del tema ---
            val currentThemePreference by settingsViewModel.currentThemePreference.collectAsStateWithLifecycle()

            // Log para depurar el cambio de currentThemePreference en MainActivity
            LaunchedEffect(currentThemePreference) {
            }
            // -------------------------------------

            // --- Inicializar Launcher para Google One Tap ---
            oneTapLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartIntentSenderForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) { // Usar Activity.RESULT_OK
                    authViewModel.handleGoogleSignInResult(result.data)
                } else {
                    authViewModel.resetAuthState()
                }
            }
            // -------------------------------------------

            // --- Efecto para manejar el token de Firebase ---
            LaunchedEffect(firebaseUser) {
                val currentUser = firebaseUser
                if (currentUser != null) {
                    try {
                        val tokenResult = currentUser.getIdToken(true).await()
                        val token = tokenResult.token
                        if (token != null) {
                            recipeViewModel.idToken = token
                            profileViewModel.idToken = token
                        } else {
                            recipeViewModel.idToken = null; profileViewModel.idToken = null
                        }
                    } catch (e: Exception) {
                        recipeViewModel.idToken = null; profileViewModel.idToken = null
                    }
                } else {
                    recipeViewModel.idToken = null; profileViewModel.idToken = null
                }
            }
            // -----------------------------------------

            // --- Observar el estado de AuthViewModel para lanzar Google One Tap ---
            val authState by authViewModel.authState.collectAsStateWithLifecycle()
            LaunchedEffect(authState) {
                if (authState is AuthState.OneTapSignInAvailable) {
                    try {
                        val intentSenderRequest = IntentSenderRequest.Builder((authState as AuthState.OneTapSignInAvailable).intentSender).build()
                        oneTapLauncher.launch(intentSenderRequest)
                        authViewModel.resetAuthState() // Resetear después de lanzar
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Error con Google: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        authViewModel.resetAuthState()
                    }
                }
            }
            // -------------------------------------------------------------------

            // --- Aplicar el tema y construir la UI ---

            FoodieClubTheme(
                userThemePreference = currentThemePreference,
                // dynamicColor = true, // O tu valor por defecto
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val currentFirebaseUser = firebaseUser // Capturar para la lógica de UI
                    val currentIsLoadingAuthState = isLoadingAuthState // Capturar para la lógica de UI

                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = remember(currentFirebaseUser, currentIsLoadingAuthState) { // Clave para estabilidad
                            if (!currentIsLoadingAuthState) {
                                if (currentFirebaseUser == null) Screen.AuthRoot.route else Screen.MainRoot.route
                            } else {
                                Screen.AuthRoot.route // Podría ser una pantalla Splash dedicada si prefieres
                            }
                        }
                    ) {
                        authGraph(
                            navController = navController,
                            onLoginSuccess = {
                                navController.navigate(Screen.MainRoot.route) {
                                    popUpTo(Screen.AuthRoot.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                        composable(Screen.MainRoot.route) {
                            if (currentFirebaseUser != null && !currentIsLoadingAuthState) {
                                MainAppScaffold(
                                    recipeViewModel = recipeViewModel,
                                    profileViewModel = profileViewModel,
                                    settingsViewModel = settingsViewModel, // Pasar la instancia correcta
                                    onSignOut = { signOut(authViewModel) }
                                )
                            } else if (!currentIsLoadingAuthState) { // Solo redirigir si no está cargando
                                LaunchedEffect(Unit) { // LaunchedEffect para navegación segura
                                    navController.navigate(Screen.AuthRoot.route) {
                                        popUpTo(Screen.MainRoot.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            }
                            // El overlay de carga se maneja abajo
                        }
                    } // Fin NavHost

                    // --- SPINNER DE CARGA INICIAL (Overlay) ---
                    if (currentIsLoadingAuthState) {
                        Box(
                            Modifier.fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)), // Más opacidad para que se note
                            Alignment.Center
                        ) {
                            CircularProgressIndicator()
                            Text(
                                "Verificando sesión...",
                                modifier = Modifier.padding(top = 80.dp),
                                color = MaterialTheme.colorScheme.onSurface // Para que el texto sea visible
                            )
                        }
                    }
                    // -----------------------------------------
                }
            }
        }
    } // Fin onCreate

    private fun initializeRemoteConfig() {
        val remoteConfig = Firebase.remoteConfig // Obtener instancia

        // 1. Configurar ajustes (intervalo mínimo de fetch)
        // Para desarrollo, un intervalo bajo (ej. 0 o 60 segundos) es útil.
        // Para producción, usa un valor más alto (ej. 3600 segundos = 1 hora o más).
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0
        }
        remoteConfig.setConfigSettingsAsync(configSettings)

        // 2. Establecer valores predeterminados desde un archivo XML
        // Esto asegura que la app tenga valores si no puede conectarse o es la primera vez.
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults) // <-- NECESITARÁS CREAR ESTE ARCHIVO XML
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 3. Obtener y activar los últimos valores
                    fetchAndActivateRemoteConfigValues()
                } else {
                }
            }
    }

    // --- NUEVA FUNCIÓN PARA OBTENER Y ACTIVAR VALORES ---
    private fun fetchAndActivateRemoteConfigValues() {
        val remoteConfig = Firebase.remoteConfig
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val updated = task.result
                    recipeViewModel.setRemoteConfigInstance(remoteConfig)
                    // ----------------------------------------
                } else {
                    recipeViewModel.setRemoteConfigInstance(remoteConfig)
                }
            }
    }

    override fun onStart() {
        super.onStart()
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
    }
}