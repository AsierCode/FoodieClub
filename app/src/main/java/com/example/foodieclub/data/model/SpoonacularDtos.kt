package com.example.foodieclub.data.model // O tu paquete de DTOs

import com.google.gson.annotations.SerializedName

// --- Petición para Spoonacular /recipes/analyze ---
data class SpoonacularAnalyzeRequest(
    val title: String, // Requerido por la API según el error anterior
    val ingredients: List<String>,
    val servings: Int? = 1
)

// --- Respuesta Principal de Spoonacular /recipes/analyze ---
data class SpoonacularNutritionResponse(
    @SerializedName("id")
    val id: Int? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("image")
    val image: String? = null,
    @SerializedName("imageType")
    val imageType: String? = null,
    @SerializedName("servings")
    val servings: Int? = null,
    @SerializedName("readyInMinutes")
    val readyInMinutes: Int? = null, // Puede ser null
    @SerializedName("sourceUrl")
    val sourceUrl: String? = null, // Puede ser null
    @SerializedName("vegetarian")
    val vegetarian: Boolean? = null,
    @SerializedName("vegan")
    val vegan: Boolean? = null,
    @SerializedName("glutenFree")
    val glutenFree: Boolean? = null,
    @SerializedName("dairyFree")
    val dairyFree: Boolean? = null,
    @SerializedName("veryHealthy")
    val veryHealthy: Boolean? = null,
    @SerializedName("cheap")
    val cheap: Boolean? = null,
    @SerializedName("veryPopular")
    val veryPopular: Boolean? = null,
    @SerializedName("sustainable")
    val sustainable: Boolean? = null,
    @SerializedName("lowFodmap")
    val lowFodmap: Boolean? = null,
    @SerializedName("weightWatcherSmartPoints")
    val weightWatcherSmartPoints: Int? = null,
    @SerializedName("gaps")
    val gaps: String? = null,
    @SerializedName("preparationMinutes")
    val preparationMinutes: Int? = null, // Puede ser null
    @SerializedName("cookingMinutes")
    val cookingMinutes: Int? = null, // Puede ser null
    @SerializedName("aggregateLikes")
    val aggregateLikes: Int? = null,
    @SerializedName("healthScore")
    val healthScore: Double? = null,
    @SerializedName("creditsText")
    val creditsText: String? = null, // Puede ser null
    @SerializedName("license")
    val license: String? = null, // Puede ser null
    @SerializedName("sourceName")
    val sourceName: String? = null, // Puede ser null
    @SerializedName("pricePerServing")
    val pricePerServing: Double? = null,

    @SerializedName("extendedIngredients") // Nombre en JSON (Spoonacular suele usar este)
    val parsedIngredients: List<SpoonacularParsedIngredient>? = null, // Lista de ingredientes parseados por Spoonacular

    @SerializedName("nutrition")
    val nutrition: SpoonacularNutritionObject? = null, // Objeto anidado para nutrientes

    @SerializedName("caloricBreakdown")
    val caloricBreakdown: SpoonacularCaloricBreakdown? = null,

    @SerializedName("weightPerServing")
    val weightPerServing: SpoonacularWeightPerServing? = null,

    // Campos que podrían estar en la raíz y no usaste antes pero estaban en el log
    @SerializedName("summary")
    val summary: String? = null,
    @SerializedName("cuisines")
    val cuisines: List<String>? = null,
    @SerializedName("dishTypes")
    val dishTypes: List<String>? = null,
    @SerializedName("diets")
    val diets: List<String>? = null,
    @SerializedName("occasions")
    val occasions: List<String>? = null, // Suele ser lista de strings
    @SerializedName("instructions")
    val instructions: String? = null, // Puede ser null
    @SerializedName("analyzedInstructions")
    val analyzedInstructions: List<Any>? = null, // Tipo genérico si no se usa
    @SerializedName("originalId")
    val originalId: String? = null, // Puede ser null
    @SerializedName("spoonacularScore")
    val spoonacularScore: Double? = null
)

// --- Objeto Anidado para "nutrition" ---
data class SpoonacularNutritionObject(
    @SerializedName("nutrients")
    val nutrients: List<SpoonacularNutrient>? = null,
    @SerializedName("properties")
    val properties: List<SpoonacularProperty>? = null, // Ejemplo, si existe en el JSON
    @SerializedName("flavonoids")
    val flavonoids: List<SpoonacularFlavonoid>? = null // Ejemplo, si existe en el JSON
    // Podrías necesitar más campos aquí si el objeto "nutrition" los tiene
)

// --- DTOs Anidados (Nutriente, Ingrediente Parseado, etc.) ---
data class SpoonacularNutrient(
    @SerializedName("name")
    val name: String?,
    @SerializedName("amount")
    val amount: Double?,
    @SerializedName("unit")
    val unit: String?,
    @SerializedName("percentOfDailyNeeds")
    val percentOfDailyNeeds: Double? = null
)

data class SpoonacularParsedIngredient(
    @SerializedName("id")
    val id: Int?,
    @SerializedName("aisle")
    val aisle: String? = null,
    @SerializedName("image")
    val image: String? = null,
    @SerializedName("consistency")
    val consistency: String? = null,
    @SerializedName("name")
    val name: String?, // Nombre limpio/parseado por Spoonacular
    @SerializedName("nameClean")
    val nameClean: String? = null,
    @SerializedName("original")
    val original: String?, // El string original que enviaste para este ingrediente
    @SerializedName("originalName")
    val originalName: String? = null, // A veces Spoonacular devuelve esto
    @SerializedName("amount")
    val amount: Double?,
    @SerializedName("unit")
    val unit: String?,
    @SerializedName("meta")
    val meta: List<String>? = null, // Información meta
    @SerializedName("measures")
    val measures: SpoonacularMeasures? = null, // Medidas en US y métrico
    // Si cada ingrediente parseado también tiene una lista de sus propios nutrientes:
    @SerializedName("nutrients") // Este campo estaba en tu log anidado en ingredientes
    val nutrients: List<SpoonacularNutrient>? = null
)

data class SpoonacularMeasures(
    @SerializedName("us")
    val us: SpoonacularMeasureAmount?,
    @SerializedName("metric")
    val metric: SpoonacularMeasureAmount?
)

data class SpoonacularMeasureAmount(
    @SerializedName("amount")
    val amount: Double?,
    @SerializedName("unitShort")
    val unitShort: String?,
    @SerializedName("unitLong")
    val unitLong: String?
)

data class SpoonacularCaloricBreakdown(
    @SerializedName("percentProtein")
    val percentProtein: Double?,
    @SerializedName("percentFat")
    val percentFat: Double?,
    @SerializedName("percentCarbs")
    val percentCarbs: Double?
)

data class SpoonacularWeightPerServing(
    @SerializedName("amount")
    val amount: Int?, // Suele ser Int
    @SerializedName("unit")
    val unit: String?
)

// Nuevos DTOs basados en la respuesta de Spoonacular si fueran necesarios (ej. para flavonoids, properties)
data class SpoonacularProperty(
    @SerializedName("name")
    val name: String?,
    @SerializedName("amount")
    val amount: Double?,
    @SerializedName("unit")
    val unit: String?
)

data class SpoonacularFlavonoid(
    @SerializedName("name")
    val name: String?,
    @SerializedName("amount")
    val amount: Double?,
    @SerializedName("unit")
    val unit: String?
)


// --- Data class para mostrar la información nutricional en la UI de forma amigable ---
data class UINutritionInfo(
    val calories: String = "N/A",
    val protein: String = "N/A",
    val fat: String = "N/A",
    val carbs: String = "N/A",
    val parsedIngredientsBySpoonacular: List<String> = emptyList(),
    val notes: List<String> = emptyList()
)