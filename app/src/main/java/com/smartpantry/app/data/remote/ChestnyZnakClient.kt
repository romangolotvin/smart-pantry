package com.smartpantry.app.data.remote

import com.smartpantry.app.data.model.ScannedProduct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class ChestnyZnakClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()
) {
    suspend fun lookup(
        code: String,
        codeType: String = "datamatrix",
        fallbackGtin: String? = null,
        expiryHint: LocalDate? = null
    ): ScannedProduct? = withContext(Dispatchers.IO) {
        val url = "https://mobile.api.crpt.ru/mobile/check".toHttpUrl().newBuilder()
            .addQueryParameter("code", code)
            .addQueryParameter("codeType", codeType)
            .build()

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "SmartPantry/1.1 (Android; educational)")
            .header("Accept", "application/json")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return@withContext null

            val root = JSONObject(body)
            val found = root.optBoolean("codeFounded", false) ||
                root.optBoolean("codeFound", false)
            if (!found && !root.has("productName") && root.optString("category").isBlank()) {
                return@withContext null
            }

            val category = root.optString("category").trim()
            val nested = if (category.isNotBlank()) {
                root.optJSONObject("${category}Data")
            } else null

            val name = firstNonBlank(
                nested?.optString("productName"),
                root.optString("productName"),
                nested?.optString("goodName"),
                root.optString("goodName"),
                nested?.optString("name")
            ) ?: return@withContext null

            val brand = firstNonBlank(
                nested?.optString("brand"),
                nested?.optString("producerName"),
                root.optString("producerName"),
                nested?.optString("ownerName"),
                root.optString("ownerName")
            ).orEmpty()

            val status = firstNonBlank(
                nested?.optString("status"),
                root.optString("status")
            ).orEmpty()

            val expiry = expiryHint
                ?: parseExpiry(nested)
                ?: parseExpiry(root)

            val displayCode = fallbackGtin?.takeIf { it.isNotBlank() } ?: code.take(28)

            ScannedProduct(
                barcode = displayCode,
                name = name,
                brand = brand,
                imageHint = emojiFor(name, category),
                source = "Честный знак",
                suggestedExpiry = expiry,
                statusLabel = statusLabel(status),
                markingCode = code
            )
        }
    }

    private fun parseExpiry(obj: JSONObject?): LocalDate? {
        if (obj == null) return null
        val keys = listOf(
            "expireDate", "expirationDate", "expiryDate",
            "expireDateTs", "expirationDateTs", "expDate"
        )
        for (key in keys) {
            if (!obj.has(key) || obj.isNull(key)) continue
            val asLong = obj.optLong(key, Long.MIN_VALUE)
            if (asLong > 1_000_000_000_000L) {
                return Instant.ofEpochMilli(asLong).atZone(ZoneId.systemDefault()).toLocalDate()
            }
            if (asLong > 1_000_000_000L) {
                return Instant.ofEpochSecond(asLong).atZone(ZoneId.systemDefault()).toLocalDate()
            }
            val asString = obj.optString(key).trim()
            if (asString.isBlank()) continue
            parseDateString(asString)?.let { return it }
        }
        return null
    }

    private fun parseDateString(value: String): LocalDate? {
        if (value.length >= 10 && value[4] == '-') {
            return runCatching { LocalDate.parse(value.take(10)) }.getOrNull()
        }
        return runCatching {
            LocalDate.parse(value, DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        }.getOrNull()
    }

    private fun firstNonBlank(vararg values: String?): String? =
        values.map { it?.trim().orEmpty() }.firstOrNull { it.isNotEmpty() }

    private fun statusLabel(status: String): String = when (status.uppercase()) {
        "INTRODUCED" -> "В обороте"
        "EMITTED" -> "Эмитирован"
        "APPLIED" -> "Нанесён"
        "RETIRED", "WRITTEN_OFF" -> "Выведен из оборота"
        "DISAGGREGATION" -> "Расформирован"
        "" -> ""
        else -> status
    }

    private fun emojiFor(name: String, category: String): String {
        val n = "$name $category".lowercase()
        return when {
            "milk" in n || "молок" in n || "dairy" in n -> "🥛"
            "water" in n || "вод" in n -> "💧"
            "beer" in n || "пив" in n -> "🍺"
            "shoes" in n || "обув" in n -> "👟"
            "clothes" in n || "одежд" in n -> "👕"
            "tobacco" in n || "табак" in n -> "🚬"
            "pharma" in n || "лекар" in n -> "💊"
            "meat" in n || "мяс" in n -> "🥩"
            else -> "✅"
        }
    }
}
