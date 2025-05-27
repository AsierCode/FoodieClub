package com.example.foodieclub.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieclub.data.model.ShoppingListItem // Asegúrate que la ruta sea correcta
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration // Para manejar el listener
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Estado para la UI
sealed interface ShoppingListUIState {
    object Loading : ShoppingListUIState
    data class Success(val items: List<ShoppingListItem>) : ShoppingListUIState
    data class Error(val message: String) : ShoppingListUIState
    object Idle : ShoppingListUIState
}

class ShoppingListViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private var listenerRegistration: ListenerRegistration? = null

    private val _uiState = MutableStateFlow<ShoppingListUIState>(ShoppingListUIState.Idle)
    val uiState: StateFlow<ShoppingListUIState> = _uiState.asStateFlow()

    private var currentUserId: String? = null

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                if (currentUserId != user.uid) {
                    currentUserId = user.uid
                    Log.d("ShoppingListVM", "Auth Listener: Usuario autenticado: $currentUserId. Adjuntando listener.")
                    attachShoppingListListener()
                } else {
                    Log.d("ShoppingListVM", "Auth Listener: Mismo usuario ya procesado: $currentUserId.")
                }
            } else {
                Log.d("ShoppingListVM", "Auth Listener: Usuario desautenticado. Limpiando lista y listener.")
                currentUserId = null
                listenerRegistration?.remove()
                listenerRegistration = null
                _uiState.value = ShoppingListUIState.Success(emptyList())
            }
        }
        // Carga inicial si el usuario ya está logueado al crear el ViewModel
        auth.currentUser?.uid?.let { userId ->
            if (currentUserId == null) { // Solo si no se ha establecido por el listener aún
                currentUserId = userId
                Log.d("ShoppingListVM", "Init Block: Usuario ya autenticado: $currentUserId. Adjuntando listener.")
                attachShoppingListListener()
            }
        } ?: run {
            if (currentUserId == null) { // Si no hay usuario y no se estableció por el listener
                Log.d("ShoppingListVM", "Init Block: Usuario no autenticado.")
                _uiState.value = ShoppingListUIState.Error("Usuario no autenticado.")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
        Log.d("ShoppingListVM", "ViewModel cleared, listener removido.")
    }

    private fun attachShoppingListListener() {
        val userId = currentUserId ?: run {
            Log.e("ShoppingListVM", "attachShoppingListListener: currentUserId es nulo.")
            _uiState.value = ShoppingListUIState.Error("No se pudo cargar la lista: Usuario no identificado.")
            return
        }
        _uiState.value = ShoppingListUIState.Loading
        Log.d("ShoppingListVM", "Adjuntando SnapshotListener para usuario: $userId")

        listenerRegistration?.remove() // Quitar listener anterior

        val userListDocumentRef = db.collection("shopping_lists").document(userId)

        listenerRegistration = userListDocumentRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("ShoppingListVM", "SnapshotListener Error para usuario $userId:", error)
                _uiState.value = ShoppingListUIState.Error("Error al cargar la lista: ${error.localizedMessage}")
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                Log.d("ShoppingListVM", "Snapshot recibido para $userId. Documento existe. Procesando ítems...")
                val itemsMapList = snapshot.get("items") as? List<Map<String, Any>> ?: emptyList()
                Log.d("ShoppingListVM", "itemsMapList de Firestore: $itemsMapList")

                val shoppingListItems = itemsMapList.mapNotNull { itemMap ->
                    val id = itemMap["id"] as? String
                    val name = itemMap["name"] as? String
                    val isPurchased = itemMap["isPurchased"] as? Boolean
                    val addedAtFirebase = itemMap["addedAt"] // Mantener como Any para inspección del tipo

                    Log.d("ShoppingListVM", "Parseando itemMap: id=$id, name=$name, isPurchased=$isPurchased, addedAtType=${addedAtFirebase?.javaClass?.simpleName}, addedAtValue=$addedAtFirebase")

                    if (id != null && name != null && isPurchased != null && addedAtFirebase is Timestamp) {
                        ShoppingListItem(id, name, isPurchased, addedAtFirebase)
                    } else {
                        Log.e("ShoppingListVM", "Fallo el casteo o algún campo es nulo/tipo incorrecto para itemMap: $itemMap. id: $id, name: $name, isPurchased: $isPurchased, addedAtFirebase: $addedAtFirebase (tipo: ${addedAtFirebase?.javaClass?.simpleName})")
                        null
                    }
                }.sortedBy { it.addedAt } // O la lógica de ordenación que prefieras
                _uiState.value = ShoppingListUIState.Success(shoppingListItems)
                Log.d("ShoppingListVM", "UI State actualizado a Success con ${shoppingListItems.size} ítems para $userId.")
            } else {
                Log.d("ShoppingListVM", "Snapshot para $userId es nulo o no existe el documento. Mostrando lista vacía.")
                _uiState.value = ShoppingListUIState.Success(emptyList())
            }
        }
    }

    fun addItem(itemName: String) {
        val userId = currentUserId ?: run {
            _uiState.value = ShoppingListUIState.Error("No se pudo añadir el ítem: Usuario no autenticado.")
            Log.e("ShoppingListVM", "addItem: currentUserId es nulo.")
            return
        }
        if (itemName.isBlank()) {
            _uiState.value = ShoppingListUIState.Error("El nombre del ítem no puede estar vacío.")
            return
        }
        Log.d("ShoppingListVM", "Intentando añadir ítem: '$itemName' para usuario: $userId")

        val newItem = ShoppingListItem(name = itemName.trim())
        val newItemMap = mapOf(
            "id" to newItem.id,
            "name" to newItem.name,
            "isPurchased" to newItem.isPurchased,
            "addedAt" to newItem.addedAt
        )

        viewModelScope.launch {
            try {
                val docRef = db.collection("shopping_lists").document(userId)
                docRef.set(mapOf("userId" to userId), SetOptions.merge()).await()
                docRef.update("items", FieldValue.arrayUnion(newItemMap)).await()
                Log.d("ShoppingListVM", "Ítem '$itemName' añadido a Firestore para $userId.")
                // El listener actualiza la UI. Podemos cambiar a Idle para limpiar mensajes de error/loaders.
                if (_uiState.value is ShoppingListUIState.Error) { // Solo si había un error previo
                    _uiState.value = ShoppingListUIState.Idle
                }
            } catch (e: Exception) {
                Log.e("ShoppingListVM", "Error al añadir ítem '$itemName' para $userId.", e)
                _uiState.value = ShoppingListUIState.Error("Error al añadir ítem: ${e.localizedMessage}")
            }
        }
    }

    fun toggleItemPurchased(itemId: String, newPurchasedState: Boolean) {
        val userId = currentUserId ?: run {
            Log.e("ShoppingListVM", "toggleItemPurchased: currentUserId es nulo.")
            return
        }
        Log.d("ShoppingListVM", "Intentando actualizar ítem: $itemId a isPurchased=$newPurchasedState para usuario: $userId")
        viewModelScope.launch {
            // _uiState.value = ShoppingListUIState.Loading // Opcional
            try {
                val docRef = db.collection("shopping_lists").document(userId)
                val snapshot = docRef.get().await()
                if (snapshot.exists()) {
                    val itemsMapList = snapshot.get("items") as? List<Map<String, Any>> ?: emptyList() // Usar List en lugar de MutableList para el casteo inicial
                    val updatedItems = itemsMapList.map { itemMap ->
                        if ((itemMap["id"] as? String) == itemId) {
                            itemMap.toMutableMap().apply { this["isPurchased"] = newPurchasedState }
                        } else {
                            itemMap
                        }
                    }
                    docRef.update("items", updatedItems).await()
                    Log.d("ShoppingListVM", "Ítem $itemId actualizado en Firestore a isPurchased=$newPurchasedState para $userId.")
                } else {
                    Log.w("ShoppingListVM", "toggleItemPurchased: Documento no existe para $userId al intentar actualizar $itemId")
                }
                // if (_uiState.value is ShoppingListUIState.Error) _uiState.value = ShoppingListUIState.Idle // Opcional
            } catch (e: Exception) {
                Log.e("ShoppingListVM", "Error al actualizar ítem $itemId para $userId.", e)
                _uiState.value = ShoppingListUIState.Error("Error al actualizar ítem: ${e.localizedMessage}")
            }
        }
    }

    fun removeItem(itemId: String) {
        val userId = currentUserId ?: run {
            Log.e("ShoppingListVM", "removeItem: currentUserId es nulo.")
            return
        }
        Log.d("ShoppingListVM", "Intentando eliminar ítem: $itemId para usuario: $userId")
        viewModelScope.launch {
            // _uiState.value = ShoppingListUIState.Loading // Opcional
            try {
                val docRef = db.collection("shopping_lists").document(userId)
                val snapshot = docRef.get().await()
                if (snapshot.exists()) {
                    val itemsMapList = snapshot.get("items") as? List<Map<String, Any>> ?: emptyList()
                    val itemToRemoveMap = itemsMapList.find { (it["id"] as? String) == itemId }

                    if (itemToRemoveMap != null) {
                        docRef.update("items", FieldValue.arrayRemove(itemToRemoveMap)).await()
                        Log.d("ShoppingListVM", "Ítem $itemId eliminado de Firestore para $userId.")
                    } else {
                        Log.w("ShoppingListVM", "removeItem: Ítem $itemId no encontrado en la lista para $userId.")
                    }
                } else {
                    Log.w("ShoppingListVM", "removeItem: Documento no existe para $userId al intentar eliminar $itemId")
                }
                // if (_uiState.value is ShoppingListUIState.Error) _uiState.value = ShoppingListUIState.Idle // Opcional
            } catch (e: Exception) {
                Log.e("ShoppingListVM", "Error al eliminar ítem $itemId para $userId.", e)
                _uiState.value = ShoppingListUIState.Error("Error al eliminar ítem: ${e.localizedMessage}")
            }
        }
    }

    fun clearCompletedItems() {
        val userId = currentUserId ?: run {
            Log.e("ShoppingListVM", "clearCompletedItems: currentUserId es nulo.")
            return
        }
        Log.d("ShoppingListVM", "Intentando limpiar ítems completados para usuario: $userId")
        viewModelScope.launch {
            // _uiState.value = ShoppingListUIState.Loading // Opcional
            try {
                val docRef = db.collection("shopping_lists").document(userId)
                val snapshot = docRef.get().await()
                if (snapshot.exists()) {
                    val itemsMapList = snapshot.get("items") as? List<Map<String, Any>> ?: emptyList()
                    val itemsToKeep = itemsMapList.filterNot { (it["isPurchased"] as? Boolean) == true }
                    docRef.update("items", itemsToKeep).await()
                    Log.d("ShoppingListVM", "Ítems completados eliminados de Firestore para $userId.")
                } else {
                    Log.w("ShoppingListVM", "clearCompletedItems: Documento no existe para $userId.")
                }
                // if (_uiState.value is ShoppingListUIState.Error) _uiState.value = ShoppingListUIState.Idle // Opcional
            } catch (e: Exception) {
                Log.e("ShoppingListVM", "Error al limpiar ítems completados para $userId.", e)
                _uiState.value = ShoppingListUIState.Error("Error al limpiar ítems: ${e.localizedMessage}")
            }
        }
    }
}