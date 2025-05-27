package com.example.foodieclub.ui.screen

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodieclub.data.model.ShoppingListItem
import com.example.foodieclub.ui.viewmodel.ShoppingListUIState
import com.example.foodieclub.ui.viewmodel.ShoppingListViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ShoppingListScreen(
    shoppingListViewModel: ShoppingListViewModel = viewModel()
) {
    val uiState by shoppingListViewModel.uiState.collectAsState()
    var newItemName by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current // Para ocultar teclado

    // Función para añadir ítem, reutilizable
    val addItemAction = {
        if (newItemName.isNotBlank()) {
            Log.d("ShoppingListScreen", "Acción añadir: Ítem '${newItemName}'")
            shoppingListViewModel.addItem(newItemName)
            newItemName = "" // Limpiar campo
            keyboardController?.hide() // Ocultar teclado
        } else {
            Log.d("ShoppingListScreen", "Acción añadir: Nombre de ítem vacío.")
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Ingresa un nombre para el ítem.",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is ShoppingListUIState.Error) {
            val errorMessage = (uiState as ShoppingListUIState.Error).message
            Log.d("ShoppingListScreen", "Mostrando Snackbar con error: $errorMessage")
            scope.launch {
                snackbarHostState.showSnackbar(message = errorMessage, duration = SnackbarDuration.Short)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Lista de la compra") },
                windowInsets = WindowInsets(top = 2.dp),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = addItemAction as () -> Unit) { // Usar la acción definida
                Icon(Icons.Filled.AddCircleOutline, "Añadir ítem")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp) // Ajuste de padding
        ) {
            // Sección para añadir nuevo ítem
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newItemName,
                    onValueChange = { newItemName = it },
                    label = { Text("Nuevo ítem") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done // Cambia el botón de Enter a "Hecho" o similar
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { addItemAction() } // Llama a la acción al presionar "Done"
                    )
                )
                // El FAB ya cumple la función de botón de añadir, así que no necesitamos otro aquí.
            }
            Spacer(modifier = Modifier.height(16.dp)) // Aumentado el espacio

            val currentItems = (uiState as? ShoppingListUIState.Success)?.items ?: emptyList()

            // Botón para limpiar completados (si hay ítems)
            if (currentItems.isNotEmpty()) { // Solo mostrar si hay ítems
                Button(
                    onClick = {
                        Log.d("ShoppingListScreen", "Botón 'Limpiar Completados' clickeado.")
                        shoppingListViewModel.clearCompletedItems()
                    },
                    modifier = Modifier.align(Alignment.End),
                    enabled = currentItems.any { it.isPurchased }
                ) {
                    Icon(Icons.Outlined.ClearAll, contentDescription = "Limpiar", modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Limpiar Completados")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }


            Log.d("ShoppingListScreen", "Evaluando uiState: $uiState")
            Box(modifier = Modifier.weight(1f)) { // Usar Box con weight para que LazyColumn o el mensaje de vacío ocupen el espacio restante
                when (val state = uiState) {
                    is ShoppingListUIState.Loading -> {
                        Log.d("ShoppingListScreen", "Mostrando CircularProgressIndicator (Loading)")
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is ShoppingListUIState.Success -> {
                        Log.d("ShoppingListScreen", "Estado Success. Número de ítems: ${state.items.size}")
                        if (state.items.isEmpty()) {
                            Log.d("ShoppingListScreen", "Mostrando mensaje: Lista vacía.")
                            Text(
                                "Tu lista de la compra está vacía.\n¡Añade algunos ítems usando el campo de arriba o el botón '+'!",
                                modifier = Modifier.align(Alignment.Center),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Log.d("ShoppingListScreen", "Mostrando LazyColumn con ítems.")
                            LazyColumn { // No necesita weight aquí si el Box padre ya lo tiene
                                items(state.items, key = { item -> item.id }) { item ->
                                    Log.d("ShoppingListScreen", "Renderizando ítem en LazyColumn: ${item.name}, comprado: ${item.isPurchased}")
                                    ShoppingListItemRow(
                                        item = item,
                                        onCheckedChanged = { isChecked ->
                                            Log.d("ShoppingListScreen", "Checkbox cambiado para ítem ${item.id} a $isChecked")
                                            shoppingListViewModel.toggleItemPurchased(item.id, isChecked)
                                        },
                                        onDeleteClicked = {
                                            Log.d("ShoppingListScreen", "Botón eliminar clickeado para ítem ${item.id}")
                                            shoppingListViewModel.removeItem(item.id)
                                        }
                                    )
                                    Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                    is ShoppingListUIState.Error -> {
                        Log.d("ShoppingListScreen", "Estado Error (mensaje ya mostrado en Snackbar): ${state.message}")
                        Text(
                            "Ocurrió un error al cargar la lista.\nPor favor, verifica tu conexión e intenta de nuevo.",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    is ShoppingListUIState.Idle -> {
                        Log.d("ShoppingListScreen", "Estado Idle. Esperando carga o acción.")
                        Text(
                            "Cargando tu lista...",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShoppingListItemRow(
    item: ShoppingListItem,
    onCheckedChanged: (Boolean) -> Unit,
    onDeleteClicked: () -> Unit,
    modifier: Modifier = Modifier // Añadir modifier
) {
    Row(
        modifier = modifier // Usar el modifier pasado
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 8.dp), // Aumentado padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.isPurchased,
            onCheckedChange = onCheckedChanged,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Text(
            text = item.name,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp), // Aumentado padding start
            style = if (item.isPurchased) MaterialTheme.typography.bodyLarge.copy(
                textDecoration = TextDecoration.LineThrough,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            ) else MaterialTheme.typography.bodyLarge,
            maxLines = 2, // Permitir hasta dos líneas si el nombre es largo
            overflow = TextOverflow.Ellipsis
        )
        IconButton(onClick = onDeleteClicked) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Eliminar ítem",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}