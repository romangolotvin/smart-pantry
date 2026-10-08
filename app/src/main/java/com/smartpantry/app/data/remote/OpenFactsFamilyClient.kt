package com.smartpantry.app.data.remote

import com.smartpantry.app.data.model.ScannedProduct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

/**
 * Open Food / Beauty / Products Facts — одинаковый API v2.
 */
class OpenFactsFamilyClient(
    private val host: String,
    private val sourceLabel: String,
    private val client: okhttp3.OkHttpClient = SharedHttp.client
) : BarcodeLookupSource {

    override suspend fun lookup(barcode: String): ScannedProduct? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://$host/api/v2/product/$barcode.json")
            .header("User-Agent", SharedHttp.USER_AGENT)
            .header("Accept", "application/json")
            .get()
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return@withContext null
                val root = JSONObject(body)
                if (root.optInt("status", 0) != 1) return@withContext null
                val product = root.optJSONObject("product") ?: return@withContext null

                val name = sequenceOf(
                    product.optString("product_name_ru"),
                    product.optString("product_name"),
                    product.optString("abbreviated_product_name"),
                    product.optString("generic_name_ru"),
                    product.optString("generic_name")
                ).map { it.trim() }.firstOrNull { ProductNameQuality.isUseful(it, barcode) }
                    ?: return@withContext null

                val brand = product.optString("brands").trim()
                ScannedProduct(
                    barcode = barcode,
                    name = name,
                    brand = brand,
                    imageHint = emojiFor(name),
                    source = sourceLabel
                )
            }
        }.getOrNull()
    }

    private fun emojiFor(name: String): String {
        val n = name.lowercase()
        return when {
            "молок" in n || "milk" in n -> "🥛"
            "яйц" in n || "egg" in n -> "🥚"
            "сыр" in n || "cheese" in n -> "🧀"
            "хлеб" in n || "bread" in n -> "🍞"
            "шампун" in n || "крем" in n || "мыло" in n -> "🧴"
            "сок" in n || "вода" in n -> "🧃"
            else -> "🛒"
        }
    }
}
