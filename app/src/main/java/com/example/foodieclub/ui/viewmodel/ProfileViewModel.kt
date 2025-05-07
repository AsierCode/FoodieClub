package com.example.foodieclub.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieclub.data.model.PerfilPrivadoDto
import com.example.foodieclub.data.model.PerfilPublicoDto
import com.example.foodieclub.data.network.ApiService
// import com.example.foodieclub.data.network.RetrofitClient // No es necesario aquí si ApiService se inyecta
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
            Log.d("ProfileViewModel", "[TokenSetter] idToken en ProfileVM. Nuevo: ${value != null} (longitud: ${value?.length}), Anterior: ${oldValue != null}. ¿Cambió?: ${oldValue != value}")

            if (oldValue != value) { // Solo actuar si el valor del token realmente cambió
                if (value != null) { // Se asignó un NUEVO token (no nulo)
                    Log.i("ProfileViewModel", "[TokenSetter] Token ASIGNADO/ACTUALIZADO. Estado MyProfile actual: ${_myProfileState.value::class.java.simpleName}. Llamando a loadMyProfile().")
                    // Llamar a loadMyProfile SIEMPRE que se establezca un token válido.
                    // loadMyProfile se encargará de no hacer nada si ya está cargando o ya tiene datos (con su guarda interna).
                    loadMyProfile()
                } else { // El token se ha limpiado (value es null)
                    Log.w("ProfileViewModel", "[TokenSetter] Token LIMPIADO (era no-null, ahora es null). Reseteando MyProfileState a Error.")
                    // Poner en un estado de error claro que indique que no hay sesión.
                    _myProfileState.value = MyProfileState.Error("Sesión cerrada o token inválido.")
                }
            }
            // No es necesario un 'else if' aquí para el caso de reasignación del mismo token si
            // la lógica de loadMyProfile ya previene cargas duplicadas si el estado es Success.
        }

    init {
        Log.d("ProfileViewModelInit", "ProfileViewModel inicializado. idToken actual (al init): ${idToken != null}")
        // Si por alguna razón el token ya está aquí y estamos en Loading (ej. recreación de VM), intentar cargar.
        if (idToken != null && _myProfileState.value is MyProfileState.Loading) {
            Log.d("ProfileViewModelInit", "Token presente en init y estado es Loading. Llamando a loadMyProfile().")
            loadMyProfile()
        }
    }

    fun loadPublicProfile(firebaseUid: String) {
        // Evitar recarga si ya estamos cargando el mismo perfil o si ya lo tenemos (opcional)
        // val currentSuccess = _publicProfileState.value as? PublicProfileState.Success
        // if (_publicProfileState.value is PublicProfileState.Loading || currentSuccess?.profile?.firebaseUid == firebaseUid) {
        //     Log.d("ProfileViewModelAPI", "loadPublicProfile: Ya cargando o perfil ya cargado para $firebaseUid. Omitiendo.")
        //     return
        // }

        _publicProfileState.value = PublicProfileState.Loading
        Log.d("ProfileViewModelAPI", "loadPublicProfile: ESTADO -> Loading. Iniciando para UID $firebaseUid")
        viewModelScope.launch {
            Log.d("ProfileViewModelAPI", "loadPublicProfile: Coroutine lanzada para UID $firebaseUid")
            try {
                Log.d("ProfileViewModelAPI", "loadPublicProfile: Llamando apiService.getPublicProfile para UID $firebaseUid...")
                val profile: PerfilPublicoDto = apiService.getPublicProfile(firebaseUid)
                Log.i("ProfileViewModelAPI", "loadPublicProfile: API OK. Perfil UID $firebaseUid recibido: ${profile.nombreMostrado}")
                _publicProfileState.value = PublicProfileState.Success(profile)
                Log.d("ProfileViewModelAPI", "loadPublicProfile: Estado actualizado a Success para UID $firebaseUid.")
            } catch (e: Exception) {
                Log.e("ProfileViewModelAPI", "loadPublicProfile: EXCEPCIÓN para UID $firebaseUid", e)
                handleApiError("loadPublicProfile (UID: $firebaseUid)", e) { msg ->
                    _publicProfileState.value = PublicProfileState.Error(msg)
                    Log.d("ProfileViewModelAPI", "loadPublicProfile: Estado actualizado a Error para UID $firebaseUid: $msg")
                }
            }
        }
    }

// En ProfileViewModel.kt

// ... (declaraciones de StateFlows y setter de idToken como antes) ...

    fun loadMyProfile() {
        val currentToken = idToken
        if (currentToken.isNullOrEmpty()) {
            Log.w("ProfileViewModelAPI", "loadMyProfile: Intento de carga pero idToken es NULO o VACÍO. Estado -> Error.")
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

        // --- CORRECCIÓN DE LA GUARDA ---
        // Siempre establecer a Loading al iniciar una nueva carga,
        // a menos que ya esté en Success y no queramos recargar.
        // Si ya estaba en Error, poner a Loading para indicar reintento.
        if (_myProfileState.value is MyProfileState.Success) {
            Log.d("ProfileViewModelAPI", "loadMyProfile: Ya en estado Success. Si se requiere recarga, usar un método refresh explícito.")
            // Considera si una llamada a loadMyProfile siempre debe recargar.
            // Por ahora, si ya es Success, no hacemos nada para evitar recargas innecesarias
            // a menos que sea una acción explícita del usuario (que podría llamar a clearMyProfileState primero).
            // return // DESCOMENTAR ESTO SI NO QUIERES QUE loadMyProfile RECARGUE SI YA ESTÁ EN SUCCESS
        }

        _myProfileState.value = MyProfileState.Loading // Establecer Loading ANTES de la coroutine
        Log.d("ProfileViewModelAPI", "loadMyProfile: ESTADO -> Loading. Iniciando con token.")
        // -------------------------------

        viewModelScope.launch {
            Log.d("ProfileViewModelAPI", "loadMyProfile: Coroutine lanzada.")
            try {
                Log.d("ProfileViewModelAPI", "loadMyProfile: Llamando apiService.getMyProfile...")
                val profile: PerfilPrivadoDto = apiService.getMyProfile(bearerToken)
                Log.i("ProfileViewModelAPI", "loadMyProfile: API OK. Perfil recibido para email: ${profile.email}")
                _myProfileState.value = MyProfileState.Success(profile)
                Log.d("ProfileViewModelAPI", "loadMyProfile: Estado actualizado a Success.")
            } catch (e: Exception) {
                Log.e("ProfileViewModelAPI", "loadMyProfile: EXCEPCIÓN", e)
                handleApiError("loadMyProfile", e) { msg ->
                    _myProfileState.value = MyProfileState.Error(msg)
                    Log.d("ProfileViewModelAPI", "loadMyProfile: Estado actualizado a Error: $msg")
                }
            }
        }
    }

// ... (resto de ProfileViewModel.kt sin cambios) ...

    private fun handleApiError(functionName: String, e: Exception, onErrorStateUpdate: (String) -> Unit) {
        val errorMsg = when(e) {
            is IOException -> "Error de Red. Revisa tu conexión."
            is HttpException -> "Error del Servidor (${e.code()}). Inténtalo más tarde."
            else -> "Error inesperado (${e::class.java.simpleName})."
        }
        val finalMessage = errorMsg
        Log.e("ViewModelAPIError", "Error en '$functionName' (ProfileVM): ${e.message}", e) // Loguear mensaje original de la excepción
        onErrorStateUpdate(finalMessage)
    }

    fun clearMyProfileState() {
        // Al limpiar, volvemos a Loading. Si no hay token, loadMyProfile pondrá Error.
        // Si hay token, loadMyProfile intentará cargar.
        _myProfileState.value = MyProfileState.Loading
        Log.d("ProfileViewModel", "clearMyProfileState: Estado Mi Perfil reseteado a Loading.")
    }
}