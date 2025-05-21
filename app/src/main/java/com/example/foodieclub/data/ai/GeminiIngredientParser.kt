package com.example.foodieclub.data.ai // O el paquete que elijas

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel // Importa del SDK de Gemini
import com.google.ai.client.generativeai.type.generationConfig // Para configurar el modelo

// Podrías pasar el apiKey en el constructor o en cada llamada,
// pasarla en cada llamada es más flexible si la obtienes dinámicamente.
class GeminiIngredientParser {

    // Define el nombre del modelo que quieres usar. "gemini-pro" es bueno para texto.
    // "gemini-pro-vision" si quisieras enviar imágenes también, pero no es necesario aquí.
    private val modelName = "gemini-1.5-pro-latest"

    suspend fun parseIngredients(ingredientsText: String, apiKey: String): String? {
        if (apiKey.isBlank()) {
            return null
        }

        try {
            val generativeModel = GenerativeModel(
                modelName = modelName,
                apiKey = apiKey,
                // Opcional: Configuración de generación (temperatura, etc.)
                generationConfig = generationConfig {
                    temperature = 0.2f // Más bajo para respuestas más deterministas y menos "creativas"
                    topK = 1
                    // Puedes añadir más configuraciones aquí si es necesario
                }
            )

            // --- TU PROMPT PERFECCIONADO DESDE AI STUDIO ---
            // Asegúrate de que este prompt sea el que te da los mejores resultados.
            // Incluye instrucciones claras y ejemplos (few-shot).
            val prompt = """
                Eres un asistente experto en parsear listas de ingredientes de recetas de cocina. Dada la siguiente lista de ingredientes, extráelos y devuélvelos en formato JSON. El JSON debe ser una lista de objetos, donde cada objeto representa un ingrediente y tiene los siguientes campos:
                - "name": (string) El nombre del ingrediente normalizado (ej. "pechuga de pollo", "cebolla").
                - "quantity": (number o null) La cantidad numérica. Si no hay cantidad explícita (ej. "al gusto", "una pizca"), usa null.
                - "unit": (string o null) La unidad de medida normalizada (ej. "g", "ml", "taza", "cda", "unidad"). Si la cantidad es null o no hay unidad clara, usa null.
                - "notes": (string o null) Cualquier información adicional sobre el ingrediente (ej. "picada", "troceada", "mediana", "grandes").

                Prioriza la precisión. Si un ingrediente es demasiado ambiguo para extraer cantidad o unidad, es mejor ponerlas como null.
                Normaliza unidades comunes: "gramos" a "g", "mililitros" a "ml", "cucharada" a "cda", "cucharadita" a "cdta".
                Si un ingrediente es algo como "1 cebolla", considera la unidad como "unidad".

                Ejemplo 1:
                Texto de ingredientes:
                200g de pechuga de pollo, finamente troceada
                1 cebolla grande, en juliana
                Pimienta negra molida al gusto
                2 cucharadas soperas de aceite de oliva virgen extra

                JSON esperado:
                [
                  {"name": "pechuga de pollo", "quantity": 200, "unit": "g", "notes": "finamente troceada"},
                  {"name": "cebolla", "quantity": 1, "unit": "unidad", "notes": "grande, en juliana"},
                  {"name": "Pimienta negra molida", "quantity": null, "unit": null, "notes": "al gusto"},
                  {"name": "aceite de oliva virgen extra", "quantity": 2, "unit": "cda", "notes": "soperas"}
                ]

                Ahora, parsea el siguiente texto SOLAMENTE devolviendo el JSON:
                Texto de ingredientes:
                $ingredientsText
            """.trimIndent()
            // --- FIN DEL PROMPT ---

            val response = generativeModel.generateContent(prompt)

            // El SDK devuelve la respuesta. El texto puede contener el JSON.
            // A veces puede tener markdown (```json ... ```), necesitarás limpiarlo.
            val responseText = response.text

            if (responseText != null) {
                // Intenta limpiar el JSON si viene envuelto en markdown
                return extractJsonFromString(responseText)
            } else {
                return null
            }

        } catch (e: Exception) {
            // Capturar cualquier excepción durante la llamada a la API de Gemini
            // Podrías querer diferenciar tipos de errores (autenticación, red, etc.)
            // e.g., if (e is AuthenticationException) { ... }
            return null
        }
    }

    // Función de utilidad para extraer JSON de un string que podría tener markdown
    private fun extractJsonFromString(text: String): String? {
        val jsonRegex = """```json\s*([\s\S]*?)\s*```""".toRegex()
        val match = jsonRegex.find(text)
        return match?.groups?.get(1)?.value ?: text.trim() // Si no hay markdown, devuelve el texto trimmeado
    }
}