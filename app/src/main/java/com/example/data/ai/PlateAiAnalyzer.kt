package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.AiPlateAnalysisResult
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.math.min

class PlateAiAnalyzer {

    private val plateSchema = Schema.obj(
        properties = mapOf(
            "dishName" to Schema.string(description = "Nombre descriptivo del plato en español"),
            "estimatedTotalGrams" to Schema.integer(description = "Peso total estimado de la ración en gramos"),
            "totalCalories" to Schema.integer(description = "Calorías totales estimadas (kcal)"),
            "proteinGrams" to Schema.integer(description = "Proteínas totales estimadas en gramos"),
            "carbsGrams" to Schema.integer(description = "Carbohidratos totales estimados en gramos"),
            "fatGrams" to Schema.integer(description = "Grasas totales estimadas en gramos"),
            "detectedIngredients" to Schema.array(
                items = Schema.string(),
                description = "Lista de ingredientes identificados con sus gramos estimados, ej: Pechuga de pollo a la plancha (160g)"
            ),
            "confidenceNote" to Schema.string(description = "Breve consejo nutricional o nota de estimación en español")
        )
    )

    private val config = generationConfig {
        responseMimeType = "application/json"
        responseSchema = plateSchema
    }

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = config
        )
    }

    suspend fun analyzePlatePhoto(bitmap: Bitmap, userHint: String = ""): Result<AiPlateAnalysisResult> {
        return try {
            val scaledBitmap = scaleBitmapSafely(bitmap, maxDimension = 1024)
            val promptText = buildString {
                append("Analiza visualmente esta fotografía de comida como un nutricionista deportivo experto. ")
                append("Identifica todos los alimentos visibles en el plato, estima el peso en gramos de cada ingrediente, ")
                append("y calcula las calorías totales y macronutrientes (proteínas, carbohidratos y grasas). ")
                if (userHint.isNotBlank()) {
                    append("Contexto adicional del usuario: $userHint. ")
                }
                append("Responde siempre en español.")
            }

            val response = withTimeout(60_000L) {
                model.generateContent(
                    content {
                        image(scaledBitmap)
                        text(promptText)
                    }
                )
            }

            val jsonText = response.text
            if (jsonText.isNullOrBlank()) {
                return Result.failure(IllegalStateException("El modelo Gemini devolvió una respuesta vacía."))
            }

            val json = JSONObject(jsonText)
            val ingredientsJson = json.optJSONArray("detectedIngredients")
            val ingredientsList = buildList {
                if (ingredientsJson != null) {
                    for (i in 0 until ingredientsJson.length()) {
                        val item = ingredientsJson.optString(i).trim()
                        if (item.isNotEmpty()) add(item)
                    }
                }
            }

            val result = AiPlateAnalysisResult(
                dishName = json.optString("dishName", "Plato analizado con IA"),
                estimatedTotalGrams = json.optInt("estimatedTotalGrams", 300).coerceIn(10, 3000),
                totalCalories = json.optInt("totalCalories", 450).coerceIn(0, 5000),
                proteinGrams = json.optInt("proteinGrams", 25).coerceIn(0, 500),
                carbsGrams = json.optInt("carbsGrams", 45).coerceIn(0, 600),
                fatGrams = json.optInt("fatGrams", 15).coerceIn(0, 400),
                detectedIngredients = ingredientsList,
                confidenceNote = json.optString("confidenceNote", "Estimación visual calculada por Gemini Flash.")
            )
            Result.success(result)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI plate analysis timed out", e)
            Result.failure(IllegalStateException("El análisis por IA superó el tiempo límite de 60 segundos. Inténtalo de nuevo."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI plate analysis failed", e)
            Result.failure(e)
        }
    }

    private fun scaleBitmapSafely(source: Bitmap, maxDimension: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxDimension && height <= maxDimension) return source
        val scale = min(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
        val targetW = (width * scale).toInt().coerceAtLeast(1)
        val targetH = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }

    private companion object {
        const val TAG = "PlateAiAnalyzer"
    }
}
