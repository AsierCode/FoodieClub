package com.example.foodieclub.ui.viewmodel // O tu paquete correcto

import com.example.foodieclub.data.model.PerfilPrivadoDto

/** Estados posibles para la pantalla del perfil privado del usuario actual */
sealed class MyProfileState {
    object Loading : MyProfileState()
    data class Success(val profile: PerfilPrivadoDto) : MyProfileState()
    data class Error(val message: String) : MyProfileState()
}