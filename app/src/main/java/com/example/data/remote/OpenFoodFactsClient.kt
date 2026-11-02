package com.example.data.remote

import com.example.data.model.ScannedProductInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class OpenFoodFactsClient {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    suspend fun fetchProductByBarcode(rawBarcode: String): Result<ScannedProductInfo> =
        withContext(Dispatchers.IO) {
            val cleanCode = rawBarcode.trim()
            if (cleanCode.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("El código de barras está vacío."))
            }

            try {
                val url = "https://world.openfoodfacts.org/api/v2/product/$cleanCode.json" +
                    "?fields=code,product_name,product_name_es,brands,nutriments,nutriscore_grade,serving_quantity,image_front_small_url"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "NutriPulseAndroid/1.0 (https://openfoodfacts.org)")
                    .get()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("Producto no encontrado en Open Food Facts (HTTP ${response.code}).")
                        )
                    }
                    val bodyStr = response.body?.string().orEmpty()
                    val root = JSONObject(bodyStr)
                    val status = root.optInt("status", 0)
                    val productObj = root.optJSONObject("product")
                    if (status != 1 || productObj == null) {
                        return@withContext Result.failure(
                            IllegalStateException("Código $cleanCode no registrado en Open Food Facts. Puedes buscarlo por nombre o usar Foto con IA.")
                        )
                    }

                    val parsed = parseProductJson(cleanCode, productObj)
                    Result.success(parsed)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun searchProductsByName(query: String): Result<List<ScannedProductInfo>> =
        withContext(Dispatchers.IO) {
            val trimmed = query.trim()
            if (trimmed.length < 2) {
                return@withContext Result.success(emptyList())
            }
            try {
                val encoded = URLEncoder.encode(trimmed, "UTF-8")
                val url = "https://es.openfoodfacts.org/cgi/search.pl?search_terms=$encoded&search_simple=1&action=process&json=1&page_size=12" +
                    "&fields=code,product_name,product_name_es,brands,nutriments,nutriscore_grade,serving_quantity,image_front_small_url"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "NutriPulseAndroid/1.0 (https://openfoodfacts.org)")
                    .get()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("Error al consultar Open Food Facts (HTTP ${response.code}).")
                        )
                    }
                    val bodyStr = response.body?.string().orEmpty()
                    val root = JSONObject(bodyStr)
                    val productsArray = root.optJSONArray("products") ?: return@withContext Result.success(emptyList())
                    val results = mutableListOf<ScannedProductInfo>()
                    for (i in 0 until productsArray.length()) {
                        val item = productsArray.optJSONObject(i) ?: continue
                        val code = item.optString("code", "").ifBlank { "OFF-$i" }
                        val parsed = parseProductJson(code, item)
                        if (parsed.name.isNotBlank()) {
                            results.add(parsed)
                        }
                    }
                    Result.success(results)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun parseProductJson(code: String, productObj: JSONObject): ScannedProductInfo {
        val nameEs = productObj.optString("product_name_es", "").trim()
        val nameDefault = productObj.optString("product_name", "").trim()
        val displayName = nameEs.ifEmpty { nameDefault }.ifEmpty { "Producto $code" }
        val brands = productObj.optString("brands", "").trim().ifEmpty { "Open Food Facts" }
        val nutriScore = productObj.optString("nutriscore_grade", "")
            .trim()
            .uppercase()
            .takeIf { it in listOf("A", "B", "C", "D", "E") }
        val imageUrl = productObj.optString("image_front_small_url", "").trim().takeIf { it.isNotEmpty() }
        val servingDefault = productObj.optDouble("serving_quantity", 100.0)
            .let { if (it.isNaN() || it <= 0.0) 100 else it.roundToInt().coerceIn(10, 1000) }

        val nutriments = productObj.optJSONObject("nutriments")
        val kcal100g = nutriments?.let {
            val kcalDirect = it.optDouble("energy-kcal_100g", Double.NaN)
            val kcalFallback = it.optDouble("energy-kcal", Double.NaN)
            val kjFallback = it.optDouble("energy_100g", Double.NaN)
            when {
                !kcalDirect.isNaN() -> kcalDirect.roundToInt()
                !kcalFallback.isNaN() -> kcalFallback.roundToInt()
                !kjFallback.isNaN() -> (kjFallback / 4.184).roundToInt()
                else -> 0
            }
        } ?: 0

        val protein100g = nutriments?.optDouble("proteins_100g", 0.0)?.let {
            if (it.isNaN()) 0 else it.roundToInt()
        } ?: 0
        val carbs100g = nutriments?.optDouble("carbohydrates_100g", 0.0)?.let {
            if (it.isNaN()) 0 else it.roundToInt()
        } ?: 0
        val fat100g = nutriments?.optDouble("fat_100g", 0.0)?.let {
            if (it.isNaN()) 0 else it.roundToInt()
        } ?: 0

        return ScannedProductInfo(
            barcode = code,
            name = displayName,
            brand = brands,
            caloriesPer100g = kcal100g.coerceAtLeast(0),
            proteinPer100g = protein100g.coerceAtLeast(0),
            carbsPer100g = carbs100g.coerceAtLeast(0),
            fatPer100g = fat100g.coerceAtLeast(0),
            servingGramsDefault = servingDefault,
            nutriScore = nutriScore,
            imageUrl = imageUrl
        )
    }
}
