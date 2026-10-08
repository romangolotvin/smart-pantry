package com.smartpantry.app.data.remote

import com.smartpantry.app.data.model.ScannedProduct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONObject

/** Бесплатный trial API UPCItemDB. */
class UpcItemDbClient(
    private val client: okhttp3.OkHttpClient = SharedHttp.client
) : BarcodeLookupSource {

    override suspend fun lookup(barcode: String): ScannedProduct? = withContext(Dispatchers.IO) {
        val url = "https://api.upcitemdb.com/prod/trial/lookup".toHttpUrl().newBuilder()
            .addQueryParameter("upc", barcode)
            .build()

        val request = Request.Builder()
            .url(url)
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
                val items = root.optJSONArray("items") ?: return@withContext null
                if (items.length() == 0) return@withContext null
                val item = items.getJSONObject(0)
                val name = sequenceOf(
                    item.optString("title"),
                    item.optString("description")
                ).map { it.trim() }.firstOrNull { ProductNameQuality.isUseful(it, barcode) }
                    ?: return@withContext null
                val brand = item.optString("brand").trim()
                ScannedProduct(
                    barcode = barcode,
                    name = name,
                    brand = brand,
                    imageHint = "🛒",
                    source = "UPC Item DB"
                )
            }
        }.getOrNull()
    }
}
