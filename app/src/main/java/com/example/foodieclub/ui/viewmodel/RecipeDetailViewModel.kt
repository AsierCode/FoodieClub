package com.example.foodieclub.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieclub.BuildConfig
import com.example.foodieclub.data.ai.GeminiIngredientParser
import com.example.foodieclub.data.model.ComentarioDto
import com.example.foodieclub.data.model.ParsedIngredient
import com.example.foodieclub.data.model.SpoonacularAnalyzeRequest
import com.example.foodieclub.data.model.SpoonacularNutritionResponse
import com.example.foodieclub.data.model.UINutritionInfo
import com.example.foodieclub.data.network.ApiService
import com.example.foodieclub.data.network.RetrofitClient
import com.example.foodieclub.data.network.SpoonacularApiClient
import com.example.foodieclub.data.network.SpoonacularApiService
import com.example.foodieclub.data.preferences.RecipeHistoryManager
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.util.Locale
import javax.inject.Inject

// --- ESTADOS ---
// (Asegúrate de que RecipeDetailState, UINutritionInfo y CommentActionResult
// estén definidas correctamente e importadas si están en otro archivo.
// RecipeDetailState DEBE incluir 'postCommentError: Boolean = false')

// data class RecipeDetailState( ... ) // Asumido importado y correcto
// data class UINutritionInfo( ... ) // Asumido importado y correcto (definido en SpoonacularDtos.kt o model)
// sealed interface CommentActionResult { ... } // Asumido importado (definido en RecipeViewModel.kt)

sealed interface NutritionAnalysisState {
    object Idle : NutritionAnalysisState
    object LoadingParser : NutritionAnalysisState
    object LoadingNutritionApi : NutritionAnalysisState
    data class Success(val uiNutritionInfo: UINutritionInfo) : NutritionAnalysisState
    data class ParserError(val message: String) : NutritionAnalysisState
    data class NutritionApiError(val message: String) : NutritionAnalysisState
}
// ---------------

class ApiKeyProvider @Inject constructor() {
    val geminiKey = BuildConfig.GEMINI_API_KEY
    val spoonacularKey = BuildConfig.SPOONACULAR_API_KEY
}


class RecipeDetailViewModel(
    application: Application,
    private val recipeId: Long,
    private val recipeHistoryManager: RecipeHistoryManager,
    private val mainRecipeViewModel: RecipeViewModel
) : AndroidViewModel(application) {

    private val yourApiService: ApiService = RetrofitClient.instance
    private val spoonacularApiService: SpoonacularApiService = SpoonacularApiClient.instance
    private val geminiParser = GeminiIngredientParser()
    private val gson = Gson()

    private val _detailState = MutableStateFlow(RecipeDetailState(isLoading = true))
    val detailState: StateFlow<RecipeDetailState> = _detailState.asStateFlow()

    private val _nutritionAnalysisState = MutableStateFlow<NutritionAnalysisState>(NutritionAnalysisState.Idle)
    val nutritionAnalysisState: StateFlow<NutritionAnalysisState> = _nutritionAnalysisState.asStateFlow()

    init {
        loadRecipeDetailData()
        observeMainViewModelCommentResults()
    }

    private fun loadRecipeDetailData() {
        _detailState.value = RecipeDetailState(isLoading = true, postCommentError = false)
        _nutritionAnalysisState.value = NutritionAnalysisState.Idle
        viewModelScope.launch {
            recipeHistoryManager.addRecipeToHistory(recipeId)
            try {
                val recipeData = yourApiService.getRecetaById(recipeId)
                val commentsData = try { yourApiService.getComments(recipeId) } catch (e: Exception) { emptyList<ComentarioDto>() }
                _detailState.value = RecipeDetailState(recipe = recipeData, comments = commentsData)
            } catch (e: Exception) {
                val errorMsg = when (e) {
                    is IOException -> "Error de Red."
                    is HttpException -> "Error del Servidor (${e.code()})."
                    else -> "Error inesperado al cargar receta."
                }
                _detailState.value = RecipeDetailState(errorMessage = errorMsg)
            }
        }
    }
// ... el resto de tu RecipeDetailViewModel ...

    fun fetchNutritionInfoUsingAI() {
        val currentRecipeData = _detailState.value.recipe
        val recipeIngredientsText = currentRecipeData?.ingredientes

        if (recipeIngredientsText.isNullOrBlank()) {
            _nutritionAnalysisState.value = NutritionAnalysisState.ParserError("No hay ingredientes para analizar.")
            return
        }
        if (BuildConfig.GEMINI_API_KEY.isBlank() || BuildConfig.SPOONACULAR_API_KEY.isBlank()) {
            val missingKey = if (BuildConfig.GEMINI_API_KEY.isBlank()) "Gemini" else "Spoonacular"
            _nutritionAnalysisState.value = NutritionAnalysisState.ParserError("Servicio no disponible (sin API key $missingKey).")
            return
        }

        _nutritionAnalysisState.value = NutritionAnalysisState.LoadingParser
        viewModelScope.launch {
            var jsonToProcess = geminiParser.parseIngredients(recipeIngredientsText, BuildConfig.GEMINI_API_KEY) // Usar un nuevo nombre para la var

            if (jsonToProcess == null) {
                _nutritionAnalysisState.value = NutritionAnalysisState.ParserError("IA no pudo procesar la lista de ingredientes.")
                return@launch
            }

            Log.d("RecipeDetailVM", "JSON original de Gemini: '$jsonToProcess'")

            // --- INICIO: Limpiar el string JSON de Gemini ---
            val jsonPrefixToRemove = "(Ask Gemini)" // O cualquier otro prefijo que Gemini esté añadiendo
            var tempCleanedJson = jsonToProcess // Trabajar con una copia temporal para la limpieza

            // Primero, intentamos quitar el prefijo si está al inicio después de un trim.
            if (tempCleanedJson.trimStart().startsWith(jsonPrefixToRemove)) {
                tempCleanedJson = tempCleanedJson.substringAfter(jsonPrefixToRemove).trimStart()
            }

            // Segundo, una forma más robusta de limpiar si el prefijo no está exactamente al inicio
            // o si hay otros caracteres no JSON antes del array/objeto.
            // Buscamos el primer '[' o '{' que indica el inicio del JSON real.
            val firstArrayBracket = tempCleanedJson.indexOf('[')
            val firstObjectBracket = tempCleanedJson.indexOf('{')

            var startIndex = -1

            if (firstArrayBracket != -1 && firstObjectBracket != -1) {
                startIndex = minOf(firstArrayBracket, firstObjectBracket)
            } else if (firstArrayBracket != -1) {
                startIndex = firstArrayBracket
            } else if (firstObjectBracket != -1) {
                startIndex = firstObjectBracket
            }

            if (startIndex != -1) {
                // Si encontramos un bracket, tomamos el substring desde ahí
                tempCleanedJson = tempCleanedJson.substring(startIndex)

                // Adicionalmente, si el JSON de Gemini a veces incluye texto explicativo *después* del JSON,
                // intentamos encontrar el último ']' o '}'
                val lastArrayBracket = tempCleanedJson.lastIndexOf(']')
                val lastObjectBracket = tempCleanedJson.lastIndexOf('}')
                var endIndex = -1

                if (lastArrayBracket != -1 && lastObjectBracket != -1) {
                    endIndex = maxOf(lastArrayBracket, lastObjectBracket)
                } else if (lastArrayBracket != -1) {
                    endIndex = lastArrayBracket
                } else if (lastObjectBracket != -1) {
                    endIndex = lastObjectBracket
                }

                if (endIndex != -1 && endIndex < tempCleanedJson.length -1) { // Corregido: endIndex < tempCleanedJson.length - 1
                    tempCleanedJson = tempCleanedJson.substring(0, endIndex + 1)
                }

            } else {
                // Si no se encuentra '[' ni '{', el string probablemente no es JSON válido
                Log.e("RecipeDetailVM", "No se encontró inicio de JSON válido ('[' o '{') en la respuesta de Gemini después de limpieza inicial: '$tempCleanedJson'")
                _nutritionAnalysisState.value = NutritionAnalysisState.ParserError("IA no devolvió un formato JSON reconocible.")
                return@launch
            }

            // Sobrescribir la variable original con la versión limpia
            jsonToProcess = tempCleanedJson
            Log.d("RecipeDetailVM", "JSON final a procesar (después de limpieza): '$jsonToProcess'")
            // --- FIN: Limpiar el string JSON de Gemini ---

            try {
                Log.d("RecipeDetailVM", "Intentando deserializar con Gson. JSON: '$jsonToProcess'")
                val ingredientsArray: Array<ParsedIngredient> = gson.fromJson(jsonToProcess, Array<ParsedIngredient>::class.java)
                val structuredIngredients: List<ParsedIngredient> = ingredientsArray.toList()

                if (structuredIngredients.isNotEmpty()) {
                    _nutritionAnalysisState.value = NutritionAnalysisState.LoadingNutritionApi
                    val ingredientsForSpoonacular: List<String> = structuredIngredients.mapNotNull { ing ->
                        ing.name?.let { name ->
                            val quantityStr = ing.quantity?.let { q -> if (q % 1.0 == 0.0) q.toInt().toString() else String.format(Locale.US, "%.1f", q) } ?: ""
                            val unitStr = ing.unit ?: ""
                            "$quantityStr $unitStr $name ${ing.notes ?: ""}".trim().replace("  +", " ")
                        }
                    }.filter { it.isNotBlank() }

                    if (ingredientsForSpoonacular.isEmpty()) {
                        _nutritionAnalysisState.value = NutritionAnalysisState.ParserError("No se pudieron formatear ingredientes válidos para la API de nutrición.")
                        return@launch
                    }

                    try {
                        val spoonacularResponse = spoonacularApiService.analyzeRecipeIngredients(
                            apiKey = BuildConfig.SPOONACULAR_API_KEY,
                            request = SpoonacularAnalyzeRequest(
                                title = currentRecipeData?.titulo ?: "Receta Analizada FoodieClub",
                                ingredients = ingredientsForSpoonacular,
                                servings = currentRecipeData?.numRaciones ?: 1
                            )
                        )

                        if (spoonacularResponse.isSuccessful && spoonacularResponse.body() != null) {
                            val nutritionData = spoonacularResponse.body()!!
                            val uiInfo = mapSpoonacularResponseToUINutritionInfo(nutritionData, ingredientsForSpoonacular.size)
                            _nutritionAnalysisState.value = NutritionAnalysisState.Success(uiInfo)
                        } else {
                            val errorBody = spoonacularResponse.errorBody()?.string() ?: "Cuerpo de error desconocido o respuesta no exitosa sin cuerpo."
                            Log.e("RecipeDetailVM", "Error API Nutrición (${spoonacularResponse.code()}): $errorBody")
                            _nutritionAnalysisState.value = NutritionAnalysisState.NutritionApiError("Error API Nutrición (${spoonacularResponse.code()}): ${errorBody.take(150)}")
                        }
                    } catch (e: Exception) { // Captura excepciones de la llamada a Spoonacular
                        Log.e("RecipeDetailVM", "EXCEPCIÓN llamando a Spoonacular. JSON que se usó: '$jsonToProcess'", e)
                        _nutritionAnalysisState.value = NutritionAnalysisState.NutritionApiError("Fallo al obtener datos de nutrición: ${e.localizedMessage}")
                    }
                } else {
                    _nutritionAnalysisState.value = NutritionAnalysisState.ParserError("IA no extrajo ingredientes válidos del JSON: '$jsonToProcess'")
                }
            } catch (e: JsonSyntaxException) { // Captura excepciones al parsear el JSON de Gemini
                Log.e("RecipeDetailVM", "JsonSyntaxException. JSON que falló: '$jsonToProcess'", e)
                _nutritionAnalysisState.value = NutritionAnalysisState.ParserError("Formato de respuesta de IA no válido: ${e.localizedMessage}")
            } catch (e: Exception) { // Captura otras excepciones (como el error de TypeToken)
                Log.e("RecipeDetailVM", "EXCEPCIÓN GENERAL. JSON que falló: '$jsonToProcess'", e)
                _nutritionAnalysisState.value = NutritionAnalysisState.ParserError("Error procesando respuesta de IA: ${e.message ?: e.javaClass.simpleName} (Ver logs)")
            }
        }
    }

// ... el resto de tu RecipeDetailViewModel y la función mapSpoonacularResponseToUINutritionInfo, etc. ...

// ... resto de tu ViewModel ...

    private fun mapSpoonacularResponseToUINutritionInfo(response: SpoonacularNutritionResponse, originalGeminiIngredientCount: Int): UINutritionInfo {
        // Acceder a nutrients a través del objeto 'nutrition'
        val nutrientsList = response.nutrition?.nutrients // Es nullable

        val findNutrient: (String) -> String = { nutrientName ->
            nutrientsList?.find { it.name?.equals(nutrientName, ignoreCase = true) == true }
                ?.let { nutrient ->
                    val amountStr = nutrient.amount?.let { amount ->
                        if (amount % 1.0 == 0.0) amount.toInt().toString()
                        else String.format(Locale.US, "%.1f", amount)
                    } ?: ""
                    val unitStr = nutrient.unit ?: ""
                    "$amountStr $unitStr".trim()
                }
                ?: "N/A"
        }

        val calories = findNutrient("Calories")
        val protein = findNutrient("Protein")
        val fat = findNutrient("Fat")
        val carbs = findNutrient("Carbohydrates") // O "Carbohydrate, by difference"

        val parsedBySpoonacular = response.parsedIngredients?.mapNotNull { ingredient ->
            val quantity = ingredient.amount?.let { q -> if (q % 1.0 == 0.0) q.toInt().toString() else String.format(Locale.US, "%.1f", q) } ?: ""
            val unit = ingredient.unit ?: ""
            val name = ingredient.nameClean ?: ingredient.name ?: ingredient.originalName ?: ingredient.original
            if (name.isNullOrBlank()) null else "$quantity $unit $name".trim().replace("  +", " ")
        }?.filter { it.isNotBlank() } ?: emptyList()

        val notes = mutableListOf<String>()
        if (parsedBySpoonacular.isNotEmpty() && parsedBySpoonacular.size < originalGeminiIngredientCount && originalGeminiIngredientCount > 0) {
            notes.add("Ingredietes relevantes reconocidos ${parsedBySpoonacular.size} de ~$originalGeminiIngredientCount ingredientes procesados por IA.")
        } else if (parsedBySpoonacular.isNotEmpty()) {
            notes.add("Ingredientes relevantes reconocidos ${parsedBySpoonacular.size} ingrediente(s).")
        }

        response.weightPerServing?.let {
            notes.add("Peso por ración (aprox): ${it.amount} ${it.unit}")
        }
        response.caloricBreakdown?.let {
            notes.add("Desglose calórico: Prot ${String.format(Locale.US,"%.0f",it.percentProtein)}% / Grasa ${String.format(Locale.US,"%.0f",it.percentFat)}% / Carbs ${String.format(Locale.US,"%.0f",it.percentCarbs)}%")
        }

        return UINutritionInfo(
            calories = calories,
            protein = protein,
            fat = fat,
            carbs = carbs,
            parsedIngredientsBySpoonacular = parsedBySpoonacular,
            notes = notes
        )
    }

    // --- Acciones delegadas y observación de comentarios ---
    fun toggleLikeOnDetail() {
        mainRecipeViewModel.toggleLike(recipeId)
        // La UI observará likedRecipeIds del mainRecipeViewModel para el estado del icono.
    }

    fun toggleSaveOnDetail() {
        mainRecipeViewModel.toggleSave(recipeId)
        // La UI observará savedRecipeIds del mainRecipeViewModel.
    }

    fun postCommentOnDetail(commentText: String) {
        mainRecipeViewModel.postComment(recipeId, commentText)
    }

    fun clearPostCommentErrorOnDetail() {
        _detailState.update { it.copy(postCommentError = false, errorMessage = null) }
        mainRecipeViewModel.clearCommentActionResult()
    }

    fun deleteCommentOnDetail(commentId: Long) {
        mainRecipeViewModel.deleteComment(recipeId, commentId)
    }

    private fun observeMainViewModelCommentResults() {
        viewModelScope.launch {
            mainRecipeViewModel.commentActionResult.collect { result ->
                if (result != null && result.targetRecipeId == recipeId) {
                    val currentComments = _detailState.value.comments // Asumiendo que RecipeDetailState inicializa comments

                    when (result) {
                        is CommentActionResult.SuccessPost -> {
                            _detailState.update {
                                it.copy(comments = currentComments + result.newComment, postCommentError = false, errorMessage = null)
                            }
                        }
                        is CommentActionResult.SuccessDelete -> {
                            _detailState.update {
                                it.copy(comments = currentComments.filterNot { c: ComentarioDto -> c.id == result.deletedCommentId }, postCommentError = false, errorMessage = null)
                            }
                        }
                        is CommentActionResult.Error -> {
                            _detailState.update {
                                it.copy(postCommentError = true, errorMessage = result.message)
                            }
                        }
                        is CommentActionResult.Loading, is CommentActionResult.LoadingDelete -> { /* No-op o UI de carga */ }
                    }
                    mainRecipeViewModel.clearCommentActionResult()
                }
            }
        }
    }
}