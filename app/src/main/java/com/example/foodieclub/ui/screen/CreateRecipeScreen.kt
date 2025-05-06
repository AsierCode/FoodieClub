package com.example.foodieclub.ui.screen // Revisa tu paquete

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState // Para scroll vertical
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll // Para scroll vertical
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage // O usa un drawable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.foodieclub.R // Para drawable
import com.example.foodieclub.ui.theme.FoodieClubTheme
import com.example.foodieclub.ui.viewmodel.CreateRecipeState
import com.example.foodieclub.ui.viewmodel.RecipeViewModel
//import androidx.compose.ui.text.input.KeyboardOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRecipeScreen(
    recipeViewModel: RecipeViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    // Estados para los campos del formulario
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") } // <-- Estado para descripción
    var ingredients by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf("") }
    var prepTime by remember { mutableStateOf("") }
    var portions by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val createRecipeState by recipeViewModel.createRecipeState
    val context = LocalContext.current

    // Efecto para mostrar Toasts y navegar atrás
    LaunchedEffect(createRecipeState) {
        when (val state = createRecipeState) {
            is CreateRecipeState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                recipeViewModel.resetCreateState()
                onNavigateBack() // Volver a la lista
            }
            is CreateRecipeState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                recipeViewModel.resetCreateState()
            }
            else -> {}
        }
    }

    // Lanzador para el selector de imágenes
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear Nueva Receta") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        // Columna principal con scroll vertical
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp) // Padding ajustado
                .fillMaxSize()
                .verticalScroll(rememberScrollState()) // Habilitar scroll
        ) {
            // --- Campos de Texto ---
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título*") }, // Marcar como obligatorio
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = title.isBlank() && createRecipeState is CreateRecipeState.Error // Mostrar error si está vacío y hubo intento fallido
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = description, // <-- Usar estado de descripción
                onValueChange = { description = it },
                label = { Text("Descripción*") }, // Marcar como obligatorio
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 100.dp), // Altura mínima
                isError = description.isBlank() && createRecipeState is CreateRecipeState.Error
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = ingredients,
                onValueChange = { ingredients = it },
                label = { Text("Ingredientes* (uno por línea)") },
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 120.dp),
                isError = ingredients.isBlank() && createRecipeState is CreateRecipeState.Error
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = steps,
                onValueChange = { steps = it },
                label = { Text("Pasos* (uno por línea)") },
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 150.dp),
                isError = steps.isBlank() && createRecipeState is CreateRecipeState.Error
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = prepTime,
                    onValueChange = { prepTime = it.filter { char -> char.isDigit() } },
                    label = { Text("Tiempo (min)") }, // Opcional
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number) // Usar alias o import directo
                )
                OutlinedTextField(
                    value = portions,
                    onValueChange = { portions = it.filter { char -> char.isDigit() } },
                    label = { Text("Raciones") }, // Opcional
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // --- Sección de Imagen ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }) { Text("Seleccionar Imagen*") } // Marcar como obligatorio

                AsyncImage(
                    model = selectedImageUri ?: android.R.drawable.ic_menu_gallery,
                    contentDescription = "Imagen seleccionada",
                    modifier = Modifier.size(100.dp),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = R.drawable.ic_launcher_foreground) // Asegúrate que existe
                )
            }
            // Mostrar error si no se seleccionó imagen y hubo intento fallido
            if (selectedImageUri == null && createRecipeState is CreateRecipeState.Error && (createRecipeState as CreateRecipeState.Error).message.contains("imagen")) {
                Text("Debes seleccionar una imagen", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }


            Spacer(modifier = Modifier.height(24.dp)) // Más espacio antes del botón final

            // --- Botón Guardar Receta ---
            Button(
                onClick = {
                    val imageUriToUpload = selectedImageUri
                    val tituloVal = title.trim()
                    val descVal = description.trim() // <-- Obtener valor de descripción
                    val ingrVal = ingredients.trim()
                    val pasosVal = steps.trim()

                    // Validar campos obligatorios antes de llamar al ViewModel
                    if (tituloVal.isBlank() || descVal.isBlank() || ingrVal.isBlank() || pasosVal.isBlank()) {
                        Toast.makeText(context, "Completa los campos obligatorios (*)", Toast.LENGTH_LONG).show()
                    } else if (imageUriToUpload == null) {
                        Toast.makeText(context, "Por favor, selecciona una imagen", Toast.LENGTH_LONG).show()
                    } else {
                        // Llamar al ViewModel SOLO si todo es válido
                        recipeViewModel.uploadImageAndCreateRecipe(
                            imageUri = imageUriToUpload,
                            title = tituloVal,
                            description = descVal, // <-- Pasar descripción
                            ingredients = ingrVal,
                            steps = pasosVal,
                            prepTime = prepTime.toIntOrNull(),
                            portions = portions.toIntOrNull()
                        )
                    }
                },
                enabled = createRecipeState != CreateRecipeState.Loading,
                modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxWidth().height(48.dp) // Altura estándar botón
            ) {
                if (createRecipeState == CreateRecipeState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Guardar Receta")
                }
            }
            Spacer(modifier = Modifier.height(16.dp)) // Espacio al final
        }
    }
}

// --- Preview ---
@Preview(showBackground = true)
@Composable
fun CreateRecipeScreenPreview() {
    FoodieClubTheme {
        CreateRecipeScreen(onNavigateBack = {})
    }
}