// En: com.example.foodieclub.ui.viewmodel (o donde tengas tus ViewModels)
package com.example.foodieclub.ui.viewmodel // O tu paquete correcto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieclub.data.model.PerfilPublicoDto
import com.example.foodieclub.data.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException // Importante para errores HTTP
import java.io.IOException // Importante para errores de red

// Asume que tienes una forma de obtener ApiService, por ejemplo, a través de RetrofitClient
// Si usas Hilt/Dagger, inyéctalo (@Inject constructor(private val apiService: ApiService))
class ProfileViewModel(
    private val apiService: ApiService // Inyecta o pasa tu instancia de ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadUserProfile(firebaseUid: String) {
        // Evita recargar si ya está cargando o si ya tiene datos para ese UID (opcional)
        // if (_uiState.value.isLoading || _uiState.value.profile?.firebaseUid == firebaseUid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) } // Empieza la carga
            try {
                val profileData = apiService.getPublicProfile(firebaseUid)
                _uiState.update {
                    it.copy(isLoading = false, profile = profileData)
                }
            } catch (e: HttpException) {
                // Error específico de HTTP (ej: 404 Not Found, 500 Server Error)
                _uiState.update {
                    it.copy(isLoading = false, error = "Error ${e.code()}: ${e.message()}")
                }
                // Podrías querer parsear e.response()?.errorBody() si tu API devuelve JSON en errores
            } catch (e: IOException) {
                // Error de Red (sin conexión, timeout, etc.)
                _uiState.update {
                    it.copy(isLoading = false, error = "Error de red. Verifica tu conexión.")
                }
            } catch (e: Exception) {
                // Otro tipo de error (ej: fallo de parseo JSON)
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Error desconocido")
                }
                e.printStackTrace() // Loguea el error para depuración
            }
        }
    }
}