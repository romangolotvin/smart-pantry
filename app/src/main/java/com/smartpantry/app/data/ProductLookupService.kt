package com.smartpantry.app.data

import com.smartpantry.app.data.model.ScannedProduct
import com.smartpantry.app.data.remote.BarcodeLookupSource
import com.smartpantry.app.data.remote.BarcodeVariants
import com.smartpantry.app.data.remote.ChestnyZnakClient
import com.smartpantry.app.data.remote.OpenFactsFamilyClient
import com.smartpantry.app.data.remote.ProductNameQuality
import com.smartpantry.app.data.remote.UpcItemDbClient
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope

class ProductLookupService(
    private val chestnyZnakClient: ChestnyZnakClient,
    private val sources: List<BarcodeLookupSource> = defaultSources(chestnyZnakClient)
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

            if (fromCz != null && ProductNameQuality.isUseful(fromCz.name)) {
                // Если имя слабое — добьём из каталогов по GTIN.
                if (parsed.gtin != null && fromCz.name.length < 8) {
                    val catalog = lookupInCatalogs(parsed.gtin)
                    if (catalog != null) {
                        return fromCz.copy(
                            name = catalog.name,
                            brand = fromCz.brand.ifBlank { catalog.brand },
                            imageHint = catalog.imageHint,
                            source = "${fromCz.source} + ${catalog.source}",
                            suggestedExpiry = fromCz.suggestedExpiry ?: parsed.expiryFromCode
                        )
                    }
                }
                return fromCz.copy(
                    suggestedExpiry = fromCz.suggestedExpiry ?: parsed.expiryFromCode
                )
            }

            if (parsed.gtin != null) {
                val catalog = lookupInCatalogs(parsed.gtin)
                if (catalog != null) {
                    return catalog.copy(
                        suggestedExpiry = parsed.expiryFromCode,
                        markingCode = parsed.raw,
                        source = catalog.source + " (GTIN маркировки)"
                    )
                }
            }

            return ScannedProduct(
                barcode = parsed.gtin ?: parsed.raw.take(28),
                name = "Товар с маркировкой",
                brand = "",
                imageHint = "✅",
                source = "Честный знак (название не найдено)",
                suggestedExpiry = parsed.expiryFromCode,
                markingCode = parsed.raw
            )
        }

        return lookupInCatalogs(parsed.raw) ?: ScannedProduct(
            barcode = parsed.raw,
            name = "Неизвестный товар",
            brand = "",
            imageHint = "🛒",
            source = "Не найдено ни в одной базе"
        )
    }

    /**
     * Перебирает варианты штрихкода и базы.
     * Для каждого варианта опрашивает источники параллельно и берёт первый полезный ответ.
     */
    private suspend fun lookupInCatalogs(code: String): ScannedProduct? {
        val variants = BarcodeVariants.of(code)
        for (variant in variants) {
            val hit = querySources(variant)
            if (hit != null) return hit.copy(barcode = code.filter { it.isDigit() }.ifEmpty { code })
        }
        return null
    }

    private suspend fun querySources(barcode: String): ScannedProduct? = supervisorScope {
        val jobs = sources.map { source ->
            async {
                runCatching { source.lookup(barcode) }.getOrNull()
                    ?.takeIf { ProductNameQuality.isUseful(it.name, barcode) }
            }
        }
        // Ждём по мере готовности — первый удачный результат.
        // Простой и надёжный вариант: await по порядку (базы уже отсортированы по приоритету).
        for (job in jobs) {
            val value = job.await()
            if (value != null) {
                jobs.forEach { if (it.isActive) it.cancel() }
                return@supervisorScope value
            }
        }
        null
    }

    companion object {
        fun defaultSources(chestnyZnakClient: ChestnyZnakClient): List<BarcodeLookupSource> = listOf(
            // Российские продукты чаще в RU-зеркале OFF
            OpenFactsFamilyClient("ru.openfoodfacts.org", "Open Food Facts RU"),
            OpenFactsFamilyClient("world.openfoodfacts.org", "Open Food Facts"),
            // Не еда: косметика, бытовая химия и прочее
            OpenFactsFamilyClient("world.openbeautyfacts.org", "Open Beauty Facts"),
            OpenFactsFamilyClient("world.openproductsfacts.org", "Open Products Facts"),
            // Честный знак по обычном EAN/GTIN
            BarcodeLookupSource { code ->
                chestnyZnakClient.lookup(
                    code = code,
                    codeType = "ean13",
                    fallbackGtin = code
                )
            },
            UpcItemDbClient()
        )
    }
}
