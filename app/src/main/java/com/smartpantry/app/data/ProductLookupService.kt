package com.smartpantry.app.data

import com.smartpantry.app.data.model.ScannedProduct
import com.smartpantry.app.data.remote.ChestnyZnakClient
import com.smartpantry.app.data.remote.OpenFoodFactsClient

class ProductLookupService(
    private val foodFactsClient: OpenFoodFactsClient,
    private val chestnyZnakClient: ChestnyZnakClient
) {
    suspend fun lookup(rawCode: String): ScannedProduct {
        val parsed = MarkingCodeParser.parse(rawCode)

        if (parsed.isMarking) {
            val fromCz = runCatching {
                chestnyZnakClient.lookup(
                    code = parsed.raw,
                    codeType = "datamatrix",
                    fallbackGtin = parsed.gtin,
                    expiryHint = parsed.expiryFromCode
                )
            }.getOrNull()

            if (fromCz != null) {
                // Если название слишком общее — дополним через Open Food Facts по GTIN.
                if (parsed.gtin != null && fromCz.name.length < 4) {
                    val off = runCatching { foodFactsClient.lookup(parsed.gtin) }.getOrNull()
                    if (off != null) {
                        return fromCz.copy(
                            name = off.name,
                            brand = fromCz.brand.ifBlank { off.brand },
                            imageHint = off.imageHint
                        )
                    }
                }
                return fromCz.copy(
                    suggestedExpiry = fromCz.suggestedExpiry ?: parsed.expiryFromCode
                )
            }

            // Честный знак недоступен — пробуем GTIN в Open Food Facts.
            if (parsed.gtin != null) {
                val off = foodFactsClient.lookup(parsed.gtin)
                return off.copy(
                    source = "Open Food Facts (по GTIN из маркировки)",
                    suggestedExpiry = parsed.expiryFromCode,
                    markingCode = parsed.raw
                )
            }

            return ScannedProduct(
                barcode = parsed.raw.take(28),
                name = "Товар с маркировкой",
                brand = "",
                imageHint = "✅",
                source = "Честный знак (не найден)",
                suggestedExpiry = parsed.expiryFromCode,
                markingCode = parsed.raw
            )
        }

        val off = foodFactsClient.lookup(parsed.raw)
        return off.copy(source = "Open Food Facts")
    }
}
