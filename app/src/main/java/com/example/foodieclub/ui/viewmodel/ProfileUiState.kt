// En: com.example.foodieclub.ui.viewmodel (o donde tengas tus UiState)
package com.example.foodieclub.ui.viewmodel // O tu paquete correcto

import com.example.foodieclub.data.model.PerfilPublicoDto

data class ProfileUiState(
    val isLoading: Boolean = false,
    val profile: PerfilPublicoDto? = null,
    val error: String? = null
)