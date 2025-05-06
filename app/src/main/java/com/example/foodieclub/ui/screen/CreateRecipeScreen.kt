package com.example.foodieclub.ui.screen // Revisa tu paquete

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
// import androidx.compose.material.icons.filled.BrokenImage // No se usa explícitamente aquí
import androidx.compose.material3.*
import androidx.compose.runtime.* // Necesario para remember, mutableStateOf, getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
// --- Imports para estado y ViewModel ---
import androidx.lifecycle.compose.collectAsStateWithLifecycle // <-- IMPORT NECESARIO
import androidx.lifecycle.viewmodel.compose.viewModel
// --------------------------------------
import coil.compose.AsyncImage
import com.example.foodieclub.R // Para drawable de error
import com.example.foodieclub.ui.theme.FoodieClubTheme
import com.example.foodieclub.ui.viewmodel.CreateRecipeState
import com.example.foodieclub.ui.viewmodel.RecipeViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRecipeScreen(
    recipeViewModel: RecipeViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    // Estados para los campos del formulario
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var ingredients by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf("") }
    var prepTime by remember { mutableStateOf("") }
    var portions by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // --- CORRECCIÓN: Usar collectAsStateWithLifecycle ---
    val createRecipeState by recipeViewModel.createRecipeState.collectAsStateWithLifecycle()
    // ---------------------------------------------------
    val context = LocalContext.current

    // Efecto para mostrar Toasts y navegar atrás
    LaunchedEffect(createRecipeState) {
        when (val state = createRecipeState) { // Acceder directamente gracias a 'by'
            is CreateRecipeState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                recipeViewModel.resetCreateState() // Resetear estado en ViewModel
                onNavigateBack() // Llamar al callback para navegar
            }
            is CreateRecipeState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                recipeViewModel.resetCreateState() // Resetear también en error
            }
            // No hacer nada para Idle o Loading en este effect
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
                    IconButton(onClick = onNavigateBack) { // Usa el callback recibido
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // --- Campos de Texto ---
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título*") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                // Mostrar error si está vacío DESPUÉS de un intento de envío fallido
                isError = title.isBlank() && createRecipeState is CreateRecipeState.Error
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción*") },
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 100.dp),
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
                    label = { Text("Tiempo (min)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = portions,
                    onValueChange = { portions = it.filter { char -> char.isDigit() } },
                    label = { Text("Raciones") },
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
                }) { Text("Seleccionar Imagen*") }

                // Mostrar la imagen seleccionada o un placeholder
                AsyncImage(
                    model = selectedImageUri ?: R.drawable.ic_launcher_background, // Placeholder si no hay URI
                    contentDescription = "Imagen seleccionada",
                    modifier = Modifier.size(100.dp).clip(MaterialTheme.shapes.small), // Clip con bordes suaves
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = R.drawable.ic_broken_image_background) // Imagen si falla la carga del URI
                )
            }
            // Mostrar error si no se seleccionó imagen y hubo intento fallido
            // Comprobamos si el mensaje de error contiene la palabra "imagen" (simple, puede mejorarse)
            if (selectedImageUri == null && createRecipeState is CreateRecipeState.Error /*&& (createRecipeState as CreateRecipeState.Error).message.contains("imagen", ignoreCase = true)*/) {
                // Comentar la condición del mensaje por ahora si no es necesaria
                Text("Debes seleccionar una imagen*", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }


            Spacer(modifier = Modifier.height(24.dp))

            // --- Botón Guardar Receta ---
            Button(
                onClick = {
                    val imageUriToUpload = selectedImageUri
                    val tituloVal = title.trim()
                    val descVal = description.trim()
                    val ingrVal = ingredients.trim()
                    val pasosVal = steps.trim()

                    // Validar campos obligatorios
                    if (tituloVal.isBlank() || descVal.isBlank() || ingrVal.isBlank() || pasosVal.isBlank()) {
                        Toast.makeText(context, "Completa los campos obligatorios (*)", Toast.LENGTH_LONG).show()
                    } else if (imageUriToUpload == null) {
                        Toast.makeText(context, "Por favor, selecciona una imagen", Toast.LENGTH_LONG).show()
                    } else {
                        // Llamar al ViewModel
                        recipeViewModel.uploadImageAndCreateRecipe(
                            imageUri = imageUriToUpload,
                            title = tituloVal,
                            description = descVal,
                            ingredients = ingrVal,
                            steps = pasosVal,
                            prepTime = prepTime.toIntOrNull(),
                            portions = portions.toIntOrNull()
                        )
                    }
                },
                // Deshabilitar botón mientras carga
                enabled = createRecipeState != CreateRecipeState.Loading,
                modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxWidth().height(48.dp)
            ) {
                // Mostrar indicador de carga o texto
                if (createRecipeState == CreateRecipeState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Guardar Receta")
                }
            }
            Spacer(modifier = Modifier.height(16.dp)) // Espacio al final para scroll
        }
    }
}

// --- Preview ---
@Preview(showBackground = true)
@Composable
fun CreateRecipeScreenPreview() {
    FoodieClubTheme {
        // En la preview, el ViewModel será uno nuevo y el estado inicial será Idle
        CreateRecipeScreen(onNavigateBack = {})
    }
}