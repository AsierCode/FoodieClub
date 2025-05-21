package com.example.foodieclub.ui.viewmodel

// import com.example.foodieclub.data.network.RetrofitClient // No es necesario aquí si ApiService se inyecta
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieclub.data.model.PerfilPrivadoDto
import com.example.foodieclub.data.model.PerfilPublicoDto
import com.example.foodieclub.data.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class ProfileViewModel(
    private val apiService: ApiService // Asumimos inyección o pasada por factory
) : ViewModel() {

    private val _publicProfileState = MutableStateFlow<PublicProfileState>(PublicProfileState.Loading)
    val publicProfileState: StateFlow<PublicProfileState> = _publicProfileState.asStateFlow()

    private val _myProfileState = MutableStateFlow<MyProfileState>(MyProfileState.Loading)
    val myProfileState: StateFlow<MyProfileState> = _myProfileState.asStateFlow()

    var idToken: String? = null
        set(value) {
            val oldValue = field
            field = value // Establecer el nuevo valor siempre
            if (oldValue != value) { // Solo actuar si el valor del token realmente cambió
                if (value != null) { // Se asignó un NUEVO token (no nulo)
                    // Llamar a loadMyProfile SIEMPRE que se establezca un token válido.
                    // loadMyProfile se encargará de no hacer nada si ya está cargando o ya tiene datos (con su guarda interna).
                    loadMyProfile()
                } else { // El token se ha limpiado (value es null)
                    // Poner en un estado de error claro que indique que no hay sesión.
                    _myProfileState.value = MyProfileState.Error("Sesión cerrada o token inválido.")
                }
            }
            // No es necesario un 'else if' aquí para el caso de reasignación del mismo token si
            // la lógica de loadMyProfile ya previene cargas duplicadas si el estado es Success.
        }

    init {
        // Si por alguna razón el token ya está aquí y estamos en Loading (ej. recreación de VM), intentar cargar.
        if (idToken != null && _myProfileState.value is MyProfileState.Loading) {
            loadMyProfile()
        }
    }

    fun loadPublicProfile(firebaseUid: String) {


        _publicProfileState.value = PublicProfileState.Loading
        viewModelScope.launch {
            try {
                val profile: PerfilPublicoDto = apiService.getPublicProfile(firebaseUid)
                _publicProfileState.value = PublicProfileState.Success(profile)
            } catch (e: Exception) {
                handleApiError("loadPublicProfile (UID: $firebaseUid)", e) { msg ->
                    _publicProfileState.value = PublicProfileState.Error(msg)
                }
            }
        }
    }

// En ProfileViewModel.kt

// ... (declaraciones de StateFlows y setter de idToken como antes) ...

    fun loadMyProfile() {
        val currentToken = idToken
        if (currentToken.isNullOrEmpty()) {
            if (!(_myProfileState.value is MyProfileState.Error &&
                        ((_myProfileState.value as MyProfileState.Error).message.contains("autenticado", ignoreCase = true) ||
                                (_myProfileState.value as MyProfileState.Error).message.contains("inválido", ignoreCase = true) ||
                                (_myProfileState.value as MyProfileState.Error).message.contains("Sesión cerrada", ignoreCase = true)
                                ))) {
                _myProfileState.value = MyProfileState.Error("No autenticado para cargar perfil.")
            }
            return
        }
        val bearerToken = "Bearer $currentToken"


        _myProfileState.value = MyProfileState.Loading // Establecer Loading ANTES de la coroutine
        // -------------------------------

        viewModelScope.launch {
            try {
                val profile: PerfilPrivadoDto = apiService.getMyProfile(bearerToken)
                _myProfileState.value = MyProfileState.Success(profile)
            } catch (e: Exception) {
                handleApiError("loadMyProfile", e) { msg ->
                    _myProfileState.value = MyProfileState.Error(msg)
                }
            }
        }
    }

// ... (resto de ProfileViewModel.kt ) ...

    private fun handleApiError(functionName: String, e: Exception, onErrorStateUpdate: (String) -> Unit) {
        val errorMsg = when(e) {
            is IOException -> "Error de Red. Revisa tu conexión."
            is HttpException -> "Error del Servidor (${e.code()}). Inténtalo más tarde."
            else -> "Error inesperado (${e::class.java.simpleName})."
        }
        val finalMessage = errorMsg
        onErrorStateUpdate(finalMessage)
    }

    fun clearMyProfileState() {
        // Al limpiar, volvemos a Loading. Si no hay token, loadMyProfile pondrá Error.
        // Si hay token, loadMyProfile intentará cargar.
        _myProfileState.value = MyProfileState.Loading
    }
}