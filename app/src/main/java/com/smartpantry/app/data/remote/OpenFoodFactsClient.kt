package com.smartpantry.app.data.remote

import com.smartpantry.app.data.model.ScannedProduct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenFoodFactsClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()
) {
    suspend fun lookup(barcode: String): ScannedProduct = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://world.openfoodfacts.org/api/v2/product/$barcode.json")
            .header("User-Agent", "SmartPantry/1.0 (Android; educational)")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return@withContext fallback(barcode)
            }
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return@withContext fallback(barcode)

            val root = JSONObject(body)
            if (root.optInt("status", 0) != 1) {
                return@withContext fallback(barcode)
            }

            val product = root.optJSONObject("product") ?: return@withContext fallback(barcode)
            val name = sequenceOf(
                product.optString("product_name_ru"),
                product.optString("product_name"),
                product.optString("generic_name_ru"),
                product.optString("generic_name")
            ).map { it.trim() }.firstOrNull { it.isNotEmpty() } ?: "Продукт $barcode"

            val brand = product.optString("brands").trim()
            ScannedProduct(
                barcode = barcode,
                name = name,
                brand = brand,
                imageHint = emojiFor(name)
            )
        }
    }

    private fun fallback(barcode: String) = ScannedProduct(
        barcode = barcode,
        name = "Продукт $barcode",
        brand = "",
        imageHint = "🛒"
    )

    private fun emojiFor(name: String): String {
        val n = name.lowercase()
        return when {
            "молок" in n || "milk" in n -> "🥛"
            "яйц" in n || "egg" in n -> "🥚"
            "сыр" in n || "cheese" in n -> "🧀"
            "хлеб" in n || "bread" in n -> "🍞"
            "помид" in n || "tomato" in n -> "🍅"
            "курица" in n || "chicken" in n -> "🍗"
            "рис" in n || "rice" in n -> "🍚"
            "макарон" in n || "pasta" in n || "спагет" in n -> "🍝"
            "яблок" in n || "apple" in n -> "🍎"
            "рыб" in n || "fish" in n -> "🐟"
            "масл" in n || "butter" in n -> "🧈"
            else -> "🛒"
        }
    }
}
