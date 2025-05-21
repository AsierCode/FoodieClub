package com.example.foodieclub.ui.viewmodel // O tu paquete de estados

// Estados de la UI para el flujo de Autenticación
sealed class AuthState {
    object Idle : AuthState()        // Estado inicial o después de una operación completada/reset
    object Loading : AuthState()     // Mostrando progreso (ej. llamando a Firebase)
    object Authenticated : AuthState() // El usuario se ha autenticado con éxito
    data class Error(val message: String) : AuthState() // Ocurrió un error
    data class OneTapSignInAvailable(val intentSender: android.content.IntentSender) : AuthState() {
        annotation class IntentSender
    }
}