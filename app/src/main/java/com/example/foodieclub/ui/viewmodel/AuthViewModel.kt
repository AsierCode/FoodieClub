package com.example.foodieclub.ui.viewmodel

import android.app.Application // <-- IMPORTAR Application
import android.content.Intent // Para handleGoogleSignInResult
import android.util.Log
import androidx.lifecycle.AndroidViewModel // <-- CAMBIAR a AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieclub.R // Para obtener Web Client ID
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient // <-- IMPORTAR SignInClient
import com.google.android.gms.common.api.ApiException // Para errores de Google
import com.google.android.gms.common.api.CommonStatusCodes // Para códigos de error comunes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException // Para error específico
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest // Importar userProfileChangeRequest
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CancellationException // <-- IMPORTAR CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


// -----------------------------------------------------------------------------------------


class AuthViewModel(application: Application) : AndroidViewModel(application) { // <-- Heredar y pasar Application

    private val auth: FirebaseAuth = Firebase.auth
    // --- DECLARAR oneTapClient AQUÍ ---
    private val oneTapClient: SignInClient = Identity.getSignInClient(application.applicationContext)
    // ---------------------------------

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Ya no necesitamos _oneTapSignInIntentSender, usaremos el estado AuthState.OneTapSignInAvailable

    fun signInWithEmailPassword(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email y contraseña requeridos.")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                _authState.value = AuthState.Authenticated
            } catch (e: Exception) {
                _authState.value = AuthState.Error(mapAuthException(e))
            }
        }
    }

    fun signUpWithEmailPassword(displayName: String, email: String, password: String) {
        // --- CORRECCIÓN: Usar .isBlank() ---
        if (displayName.isBlank() || email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Nombre, email y contraseña requeridos.")
            return
        }
        if (password.length < 6) {
            _authState.value = AuthState.Error("La contraseña debe tener al menos 6 caracteres.")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()

                val profileUpdates = userProfileChangeRequest { // Usar builder de KTX
                    this.displayName = displayName // Asignar nombre
                }
                authResult.user?.updateProfile(profileUpdates)?.await()

                _authState.value = AuthState.Authenticated
            } catch (e: Exception) {
                _authState.value = AuthState.Error(mapAuthException(e))
            }
        }
    }

    fun beginGoogleSignIn() {
        _authState.value = AuthState.Loading

        // --- CORRECCIÓN: Obtener Application Context ---
        val appContext = getApplication<Application>().applicationContext
        val webClientId = appContext.getString(R.string.default_web_client_id)
        // ----------------------------------------------

        val signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(webClientId)
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            ).build()

        viewModelScope.launch {
            try {
                val result = oneTapClient.beginSignIn(signInRequest).await()
                // --- CORRECCIÓN: Acceder a intentSender y emitir estado ---
                _authState.value = AuthState.OneTapSignInAvailable(result.pendingIntent.intentSender)
                // ---------------------------------------------------------
            } catch (e: Exception) {
                if (e is CancellationException) throw e // No mostrar cancelación al usuario
                // --- CORRECCIÓN: Mejor manejo de errores de API de Google ---
                val apiException = e as? ApiException
                if (apiException?.statusCode == CommonStatusCodes.NETWORK_ERROR) {
                    _authState.value = AuthState.Error("Error de red al intentar iniciar sesión con Google.")
                } else {
                    _authState.value = AuthState.Error("Error con Google: ${e.localizedMessage ?: "Desconocido"}")
                }
            }
        }
    }

    fun handleGoogleSignInResult(data: Intent?) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val credential = oneTapClient.getSignInCredentialFromIntent(data)
                val googleIdToken = credential.googleIdToken
                if (googleIdToken != null) {
                    val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    auth.signInWithCredential(firebaseCredential).await()
                    _authState.value = AuthState.Authenticated
                } else {
                    _authState.value = AuthState.Error("No se pudo obtener la credencial de Google.")
                }
            } catch (e: ApiException) { // Capturar específicamente ApiException
                if (e.statusCode == CommonStatusCodes.CANCELED) { // Comprobar si fue cancelación
                    _authState.value = AuthState.Idle // Volver a Idle si cancela
                } else {
                    _authState.value = AuthState.Error("Error al iniciar sesión con Google: ${CommonStatusCodes.getStatusCodeString(e.statusCode)}")
                }
            } catch (e: Exception) { // Capturar otras excepciones (ej. Firebase)
                if (e is CancellationException) throw e
                // --- CORRECCIÓN: Usar localizedMessage ---
                _authState.value = AuthState.Error("Error al iniciar sesión con Google: ${e.localizedMessage ?: "Error desconocido"}")
            }
            // No necesitamos finally para limpiar _oneTapSignInIntentSender porque ya no existe
        }
    }

    // Ya no necesitamos resetOneTapSender, la UI reaccionará al cambio de AuthState

    fun resetAuthState() {
        // Solo resetear si no estamos ya en Idle o Authenticated
        if (_authState.value !is AuthState.Idle && _authState.value !is AuthState.Authenticated) {
            _authState.value = AuthState.Idle
        }
    }

    private fun mapAuthException(e: Exception): String {
        return when (e) {
            is FirebaseAuthUserCollisionException -> "El correo electrónico ya está en uso."
            is FirebaseAuthWeakPasswordException -> "La contraseña debe tener al menos 6 caracteres."
            is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos."
            // Considerar otros FirebaseAuthException aquí
            else -> e.localizedMessage ?: "Ocurrió un error inesperado."
        }
    }
}