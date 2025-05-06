package com.example.foodieclub.ui.viewmodel // O tu paquete correcto

import com.example.foodieclub.data.model.PerfilPrivadoDto

/** Estados posibles para la pantalla del perfil privado del usuario actual */
sealed class MyProfileState {
    object Loading : MyProfileState() // Estado mientras se cargan los datos del perfil
    data class Success(val profile: PerfilPrivadoDto) : MyProfileState() // Estado de éxito con los datos privados
    data class Error(val message: String) : MyProfileState() // Estado de error con mensaje
}